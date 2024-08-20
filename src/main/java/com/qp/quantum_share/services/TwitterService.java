package com.qp.quantum_share.services;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qp.quantum_share.configuration.ConfigurationClass;
import com.qp.quantum_share.dao.QuantumShareUserDao;
import com.qp.quantum_share.dto.MediaPost;
import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.dto.SocialAccounts;
import com.qp.quantum_share.dto.TwitterUser;
import com.qp.quantum_share.exception.CommonException;
import com.qp.quantum_share.response.ErrorResponse;
import com.qp.quantum_share.response.ResponseStructure;
import com.qp.quantum_share.response.ResponseWrapper;
import com.qp.quantum_share.response.SuccessResponse;

import io.jsonwebtoken.io.IOException;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.v1.UploadedMedia;

@Service
public class TwitterService {

	@Value("${twitter.client_id}")
	private String client_id;

	@Value("${twitter.redirect_uri}")
	private String redirect_uri;

	@Value("${twitter.code_challenge_method}")
	private String code_challenge_method;

	@Value("${twitter.scope}")
	private String scope;

	@Value("${twitter.state}")
	private String state;

	@Value("${twitter.code_challenge}")
	private String code_challenge;

	@Value("${twitter.consumerKey}")
	private String consumerKey;

	@Value("${twitter.consumerSecret}")
	private String consumerSecret;

	@Value("${twitter.accessToken}")
	private String accessToken;

	@Value("${twitter.accessTokenSecret}")
	private String accessTokenSecret;
	@Autowired
	ResponseStructure<String> structure;

	@Autowired
	HttpHeaders headers;

	@Autowired
	ConfigurationClass configurationClass;

	@Autowired
	MultiValueMap<String, Object> multiValueMap;

	@Autowired
	RestTemplate restTemplate;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	TwitterUser twitterUser;

	@Autowired
	SocialAccounts socialAccounts;

	@Autowired
	QuantumShareUserDao userDao;

	@Autowired
	SuccessResponse successResponse;

	@Autowired
	ErrorResponse errorResponse;

	public ResponseEntity<ResponseStructure<String>> getAuthorizationUrl(QuantumShareUser user) {
		String apiUrl = "https://twitter.com/i/oauth2/authorize";
		String state = generateStateValue(user.getUserId());
		String ouath = apiUrl + "?response_type=code&client_id=" + client_id + "&redirect_uri=" + redirect_uri
				+ "&scope=" + scope + "&state=" + state + "&code_challenge=" + code_challenge
				+ "&code_challenge_method=" + code_challenge_method;
		structure.setCode(HttpStatus.OK.value());
		structure.setStatus("success");
		structure.setMessage("oauth_url generated successfully");
		structure.setPlatform(null);
		structure.setData(ouath);
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
	}

	public static String generateStateValue(int i) {
		return i + UUID.randomUUID().toString();
	}

	public ResponseEntity<ResponseStructure<String>> verifyToken(String code, QuantumShareUser user) {
//		try {
		String url = "https://api.twitter.com/2/oauth2/token";

		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		multiValueMap.clear();
		multiValueMap.add("code", code);
		multiValueMap.add("grant_type", "authorization_code");
		multiValueMap.add("redirect_uri", redirect_uri);
		multiValueMap.add("code_verifier", code_challenge);
		multiValueMap.add("client_id", client_id);
		HttpEntity<MultiValueMap<String, Object>> httpRequest = configurationClass.getHttpEntityWithMap(multiValueMap,
				headers);
		System.out.println("before");
		ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.POST, httpRequest, JsonNode.class);
		System.out.println("response : " + response);
		if (response.getStatusCode().is2xxSuccessful()) {

			JsonNode responseBody = response.getBody();
			String access_token = responseBody.get("access_token").asText();
			String refresh_token = responseBody.get("refresh_token").asText();
			long expires_in = responseBody.get("expires_in").asLong();
			String tweetUser = fetchUser(access_token);
			return saveTwitterUser(tweetUser, user, access_token, refresh_token, expires_in);
		} else {
			structure.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
			structure.setData(null);
			structure.setMessage("Something went wrong!!");
			structure.setPlatform(null);
			structure.setStatus("error");
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	private ResponseEntity<ResponseStructure<String>> saveTwitterUser(String tweetUser, QuantumShareUser user,
			String access_token, String refresh_token, long expires_in) {
		try {
			JsonNode rootNode = objectMapper.readTree(tweetUser);
			SocialAccounts accounts = user.getSocialAccounts();
			if (accounts == null) {
				twitterUser.setTwitterUserId(rootNode.get("data").get("id").asLong());
				twitterUser.setAccess_token(access_token);
				twitterUser.setRefresh_token(refresh_token);
				twitterUser.setTokenGenerationTime(System.currentTimeMillis() / 1000);
				twitterUser.setAccessTokenExpirationTime(expires_in);
				twitterUser.setName(rootNode.get("data").get("name").asText());
				twitterUser.setUserName(rootNode.get("data").get("username").asText());
				twitterUser.setPicture_url(rootNode.get("data").get("profile_image_url").asText());
				twitterUser
						.setFollower_count(rootNode.get("data").get("public_metrics").get("followers_count").asInt());

				socialAccounts.setTwitterUser(twitterUser);
				user.setSocialAccounts(socialAccounts);
			} else if (accounts.getTwitterUser() == null) {
				twitterUser.setAccess_token(access_token);
				twitterUser.setRefresh_token(refresh_token);
				twitterUser.setTokenGenerationTime(System.currentTimeMillis() / 1000);
				twitterUser.setAccessTokenExpirationTime(expires_in);
				twitterUser.setTwitterUserId(rootNode.get("data").get("id").asLong());
				twitterUser.setName(rootNode.get("data").get("name").asText());
				twitterUser.setUserName(rootNode.get("data").get("username").asText());
				twitterUser.setPicture_url(rootNode.get("data").get("profile_image_url").asText());
				twitterUser
						.setFollower_count(rootNode.get("data").get("public_metrics").get("followers_count").asInt());
				accounts.setTwitterUser(twitterUser);
				user.setSocialAccounts(accounts);
			} else {
				TwitterUser exUser = accounts.getTwitterUser();
				exUser.setTwitterUserId(rootNode.get("data").get("id").asLong());
				exUser.setAccess_token(access_token);
				exUser.setRefresh_token(refresh_token);
				exUser.setTokenGenerationTime(System.currentTimeMillis() / 1000);
				exUser.setAccessTokenExpirationTime(expires_in);
				exUser.setName(rootNode.get("data").get("name").asText());
				exUser.setUserName(rootNode.get("data").get("username").asText());
				exUser.setPicture_url(rootNode.get("data").get("profile_image_url").asText());
				exUser.setFollower_count(rootNode.get("data").get("public_metrics").get("followers_count").asInt());
				accounts.setTwitterUser(exUser);
				user.setSocialAccounts(accounts);
			}
			userDao.save(user);

			structure.setCode(HttpStatus.OK.value());
			structure.setStatus("success");
			structure.setMessage("twitter connected successfully");
			structure.setPlatform("twitter");

			TwitterUser tuser = user.getSocialAccounts().getTwitterUser();
			Map<String, Object> map = configurationClass.getMap();
			map.clear();
			map.put("username", tuser.getUserName());
			map.put("follower_count", tuser.getFollower_count());
			map.put("picture_url", tuser.getPicture_url());

			structure.setData(map);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);

		} catch (NullPointerException e) {
			throw new CommonException(e.getMessage());
		} catch (JsonMappingException e) {
			throw new CommonException(e.getMessage());
		} catch (JsonProcessingException e) {
			throw new CommonException(e.getMessage());
		}
	}

	private String fetchUser(String access_token) {

		try {
			String apiUrl = "https://api.twitter.com/2/users/me?user.fields=id,name,profile_image_url,username,public_metrics";
			headers.setBearerAuth(access_token);
//			String requestBody = "user.fields=id,name,profile_image_url,username,public_metrics";
			HttpEntity<String> httpRequest = configurationClass.getHttpEntity(headers);
			ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.GET, httpRequest, String.class);
			if (response.getStatusCode().is2xxSuccessful()) {
				return response.getBody();
			} else {
				return null;
			}
		} catch (NullPointerException exception) {
			throw new CommonException(exception.getMessage());
		}
	}

	public File convertMultipartFileToFile(MultipartFile multipartFile) throws java.io.IOException {
		File file = new File(multipartFile.getOriginalFilename());
		try (FileOutputStream fos = new FileOutputStream(file)) {
			fos.write(multipartFile.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return file;
	}

	public ResponseEntity<ResponseWrapper> postOnTwitter(MediaPost mediaPost, MultipartFile mediaFile,
			TwitterUser twitteruser, QuantumShareUser user) throws TwitterException {
		if (mediaFile.getContentType().startsWith("image")) {
			if (mediaFile.getContentType().equals("image/jpeg") || mediaFile.getContentType().equals("image/png")
					|| mediaFile.getContentType().equals("image/jpg")) {
				return postImageOnTwitter(mediaPost, mediaFile, twitteruser, user);
			} else {
				structure.setCode(HttpStatus.BAD_REQUEST.value());
				structure.setMessage("Invalid File Type. Accepted image types are JPG, PNG, and JPEG.");
				structure.setStatus("error");
				structure.setData(null);
				structure.setPlatform("twitter");
				return new ResponseEntity<ResponseWrapper>(configurationClass.getResponseWrapper(structure),
						HttpStatus.BAD_REQUEST);
			}
		} else if (mediaFile.getContentType().startsWith("video")) {
			if (mediaFile.getContentType().equals("video/mp4")) {
				return postVideoOnTwitter(mediaPost, mediaFile, twitteruser, user);
			} else {
				structure.setCode(HttpStatus.BAD_REQUEST.value());
				structure.setMessage("Invalid File Type. Accepted video types are JPG, PNG, and JPEG.");
				structure.setStatus("error");
				structure.setData(null);
				structure.setPlatform("instagram");
				return new ResponseEntity<ResponseWrapper>(configurationClass.getResponseWrapper(structure),
						HttpStatus.BAD_REQUEST);
			}
		} else {
			structure.setCode(HttpStatus.BAD_REQUEST.value());
			structure.setMessage("Invalid File Type");
			structure.setStatus("error");
			structure.setData(null);
			structure.setPlatform("twitter");
			return new ResponseEntity<ResponseWrapper>(configurationClass.getResponseWrapper(structure),
					HttpStatus.BAD_REQUEST);
		}
	}

	private ResponseEntity<ResponseWrapper> postVideoOnTwitter(MediaPost mediaPost, MultipartFile mediaFile,
			TwitterUser twitteruser2, QuantumShareUser user) throws TwitterException {
		try {
			Twitter twitter = Twitter.newBuilder().oAuthConsumer(consumerKey, consumerSecret)
					.oAuthAccessToken(accessToken, accessTokenSecret).build();
			UploadedMedia response = twitter.v1().tweets().uploadMediaChunked(mediaFile.getOriginalFilename(),
					mediaFile.getInputStream());
			long mediaId = response.getMediaId();

			if (mediaId != 0) {
				return postTweet(mediaId, mediaPost, twitteruser2, user);

			} else {
				structure.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
				structure.setMessage("Something went wrong unable to post media on Twitter!!");
				structure.setPlatform("twitter");
				structure.setStatus("error");
				structure.setData(null);
				return new ResponseEntity<ResponseWrapper>(configurationClass.getResponseWrapper(structure),
						HttpStatus.INTERNAL_SERVER_ERROR);
			}
		} catch (NullPointerException e) {
			throw new CommonException(e.getMessage());
		} catch (TwitterException e) {
			e.printStackTrace();
			throw new TwitterException("Error while making OAuth request", e);
		} catch (java.io.IOException e) {
			throw new CommonException(e.getMessage());
		}

	}

	private ResponseEntity<ResponseWrapper> postImageOnTwitter(MediaPost mediaPost, MultipartFile mediaFile,
			TwitterUser twitteruser, QuantumShareUser user) throws TwitterException {
		try {

			// step-1 upload image
			Twitter twitter = Twitter.newBuilder().oAuthConsumer(consumerKey, consumerSecret)
					.oAuthAccessToken(accessToken, accessTokenSecret).build();
			UploadedMedia response = twitter.v1().tweets().uploadMedia(convertMultipartFileToFile(mediaFile));

			long mediaId = response.getMediaId();
			System.out.println(mediaId);
			if (mediaId != 0) {
				return postTweet(mediaId, mediaPost, twitteruser, user);

			} else {
				structure.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
				structure.setMessage("Something went wrong unable to post media on Twitter!!");
				structure.setPlatform("twitter");
				structure.setStatus("error");
				structure.setData(null);
				return new ResponseEntity<ResponseWrapper>(configurationClass.getResponseWrapper(structure),
						HttpStatus.INTERNAL_SERVER_ERROR);
			}
		} catch (NullPointerException e) {
			throw new CommonException(e.getMessage());
		} catch (TwitterException e) {
			e.printStackTrace();
			throw new TwitterException("Error while making OAuth request", e);
		} catch (java.io.IOException e) {
			throw new CommonException(e.getMessage());
		}

	}

	private ResponseEntity<ResponseWrapper> postTweet(long mediaId, MediaPost mediaPost, TwitterUser twitteruser2,
			QuantumShareUser user) {
		System.out.println(" postTweet -  ");
		try {
			if (mediaPost.getCaption() == null) {
				mediaPost.setCaption(" ");
			}
			String apiUrl = "https://api.twitter.com/2/tweets";
			String access_token = twitteruser2.getAccess_token();
			if (isAccessTokenExpired(twitteruser2)) {
				access_token = generateAccessToken(twitteruser2, user);
			}
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.setBearerAuth(access_token);
			System.out.println("before request");
			String requestBody = String.format("{\"text\": \"%s\", \"media\": {\"media_ids\": [\"%s\"]}}",
					mediaPost.getCaption(), mediaId);

			HttpEntity<String> httpRequest = configurationClass.getHttpEntity(requestBody, headers);
			ResponseEntity<JsonNode> response = restTemplate.exchange(apiUrl, HttpMethod.POST, httpRequest,
					JsonNode.class);
			System.out.println("response :   " + response);
			if (response.getStatusCode().is2xxSuccessful()) {
				successResponse.setCode(HttpStatus.OK.value());
				successResponse.setMessage("Posted On Twitter");
				successResponse.setStatus("success");
				successResponse.setData(response.getBody().get("data"));
				successResponse.setPlatform("twitter");
				return new ResponseEntity<ResponseWrapper>(configurationClass.getResponseWrapper(successResponse),
						HttpStatus.OK);
			} else {
				errorResponse.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
				errorResponse.setMessage("Request Failed");
				errorResponse.setStatus("error");
				errorResponse.setData(response.getBody());
				errorResponse.setPlatform("twitter");
				return new ResponseEntity<ResponseWrapper>(configurationClass.getResponseWrapper(errorResponse),
						HttpStatus.INTERNAL_SERVER_ERROR);
			}
		} catch (NullPointerException e) {
			throw new CommonException(e.getMessage());
		}

	}

	public boolean isAccessTokenExpired(TwitterUser twitteruser2) {
		long expirationTime = twitteruser2.getAccessTokenExpirationTime();
		long generationTime = twitteruser2.getTokenGenerationTime();
		long bufferTime = 15 * 60;

		// Calculate actual expiration time based on generation time and expiration
		// duration
		long actualExpirationTimeInSec = generationTime + expirationTime;

		// Convert expiration to milliseconds for consistency with current time
		long actualExpirationTime = actualExpirationTimeInSec * 1000;

		// Check if current timestamp with buffer is greater than or equal to actual
		// expiration
		long currentTime = System.currentTimeMillis();
		return (currentTime + (bufferTime * 1000)) >= actualExpirationTime;

	}

	private String generateAccessToken(TwitterUser twitteruser2, QuantumShareUser user) {
		try {
			String apiUrl = "https://api.twitter.com/2/oauth2/token";

			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			SocialAccounts accounts = user.getSocialAccounts();
			MultiValueMap<String, Object> requestBody = configurationClass.getMultiValueMap();
			requestBody.clear();
			requestBody.add("refresh_token", twitteruser2.getRefresh_token());
			requestBody.add("grant_type", "refresh_token");
			requestBody.add("client_id", client_id);

			HttpEntity<MultiValueMap<String, Object>> httpRequest = configurationClass.getHttpEntityWithMap(requestBody,
					headers);
			ResponseEntity<JsonNode> response = restTemplate.exchange(apiUrl, HttpMethod.POST, httpRequest,
					JsonNode.class);
			if (response.getStatusCode().is2xxSuccessful()) {
				JsonNode res = response.getBody();
				twitteruser2.setAccess_token(res.has("access_token") ? res.get("access_token").asText() : null);
				twitteruser2.setRefresh_token(res.has("refresh_token") ? res.get("refresh_token").asText() : null);
				twitteruser2.setAccessTokenExpirationTime(res.has("expires_in") ? res.get("expires_in").asLong() : 0);
				twitteruser2.setTokenGenerationTime(System.currentTimeMillis() / 1000);
				accounts.setTwitterUser(twitteruser2);
				user.setSocialAccounts(accounts);
				userDao.save(user);
				return res.has("access_token") ? res.get("access_token").asText() : null;
			} else {
				return null;
			}
		} catch (NullPointerException e) {
			throw new CommonException(e.getMessage());
		}

	}

}
