package com.qp.quantum_share.services;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qp.quantum_share.configuration.ConfigurationClass;
import com.qp.quantum_share.controller.SocialMediaLogoutController;
import com.qp.quantum_share.dao.QuantumShareUserDao;
import com.qp.quantum_share.dto.CreditSystem;
import com.qp.quantum_share.dto.MediaPost;
import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.dto.SocialAccounts;
import com.qp.quantum_share.dto.YoutubeUser;
import com.qp.quantum_share.exception.CommonException;
import com.qp.quantum_share.response.ErrorResponse;
import com.qp.quantum_share.response.ResponseStructure;
import com.qp.quantum_share.response.ResponseWrapper;
import com.qp.quantum_share.response.SuccessResponse;

@Service
public class YoutubeService {

	@Value("${youtube.client-id}")
	private String clientId;

	@Value("${youtube.client-secret}")
	private String clientSecret;

	@Value("${youtube.redirect-uri}")
	private String redirectUri;

	@Value("${youtube.token-uri}")
	private String tokenUri;

	@Value("${youtube.scope}")
	private String scope;

	@Autowired
	QuantumShareUserDao userDao;

	@Autowired
	HttpHeaders headers;

	@Autowired
	ResponseStructure<String> structure;

	@Autowired
	SuccessResponse successResponse;

	@Autowired
	ErrorResponse errorResponse;

	@Autowired
	ConfigurationClass config;

	@Autowired
	MultiValueMap<String, Object> multiValueMap;

	@Autowired
	RestTemplate restTemplate;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	YoutubeUser youtubeUser;
	
	@Autowired
	SocialMediaLogoutController logoutController;

	@Autowired
	SocialAccounts socialAccounts;

	@Autowired
	MultiValueMap<String, Object> linkedMultiValueMap;

	@Autowired
	AnalyticsPostService analyticsPostService;

	@Autowired
	ConfigurationClass.ByteArrayResourceFactory byteArrayResourceFactory;

	public ResponseEntity<ResponseStructure<String>> getAuthorizationUrl(QuantumShareUser user) {
		String authUri = "https://accounts.google.com/o/oauth2/v2/auth";
		String ouath = authUri + "?response_type=code&client_id=" + clientId + "&redirect_uri=" + redirectUri
				+ "&scope=" + scope + "&access_type=" + "offline" + "&prompt=" + "consent";
		ResponseStructure<String> structure = new ResponseStructure<String>();
		structure.setCode(HttpStatus.OK.value());
		structure.setStatus("success");
		structure.setMessage("oauth_url generated successfully");
		structure.setPlatform(null);
		structure.setData(ouath);
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
	}

	public ResponseEntity<ResponseStructure<String>> verifyToken(String code, QuantumShareUser user, int userId) {
		try {
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			multiValueMap.add("code", code);
			multiValueMap.add("client_id", clientId);
			multiValueMap.add("client_secret", clientSecret);
			multiValueMap.add("redirect_uri", redirectUri);
			multiValueMap.add("grant_type", "authorization_code");

			HttpEntity<MultiValueMap<String, Object>> httpRequest = config.getHttpEntityWithMap(multiValueMap, headers);
			ResponseEntity<String> response = restTemplate.exchange(tokenUri, HttpMethod.POST, httpRequest,
					String.class);
			if (response.getStatusCode() == HttpStatus.OK) {
				JsonNode responseBody = objectMapper.readTree(response.getBody());
				if (responseBody != null && responseBody.has("access_token")) {
					String accessToken = responseBody.get("access_token").asText();
					String refreshToken = responseBody.get("refresh_token").asText();
					String youtubeUserDetails = getChannelDetails(accessToken);

					if (youtubeUserDetails == null || youtubeUserDetails.isEmpty()) {
						structure.setCode(HttpStatus.NOT_FOUND.value());
						structure.setStatus("error");
						structure.setMessage("Please create a YouTube channel and try again.");
						structure.setPlatform("youtube");
						structure.setData(null);
						return new ResponseEntity<>(structure, HttpStatus.NOT_FOUND);
					}
					return saveYoutubeUser(youtubeUserDetails, user, accessToken, refreshToken, userId);
				} else {
					throw new CommonException("Access token not found in response");
				}
			} else {
				structure.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
				structure.setData(null);
				structure.setMessage("Something went wrong!!");
				structure.setPlatform("youtube");
				structure.setStatus("error");
				return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.INTERNAL_SERVER_ERROR);
			}
		} catch (JsonProcessingException e) {
			throw new CommonException(e.getMessage());
		} catch (Exception e) {
			throw new CommonException(e.getMessage());
		}
	}

	private String getChannelDetails(String accessToken) {
		try {
			String url = "https://www.googleapis.com/youtube/v3/channels?mine=true&part=snippet,statistics&access_token="
					+ accessToken;
			headers.setBearerAuth(accessToken);
			HttpEntity<String> httpRequest = config.getHttpEntity(headers);
			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, httpRequest, String.class);

			if (response.getStatusCode().is2xxSuccessful()) {
				JsonNode responseBody = objectMapper.readTree(response.getBody());
				if (responseBody != null && responseBody.has("items") && responseBody.get("items").size() > 0) {
					return response.getBody();
				} else {
					return null;
				}
			} else {
				return null;
			}
		} catch (NullPointerException | JsonProcessingException exception) {
			throw new CommonException(exception.getMessage());
		}
	}

	private ResponseEntity<ResponseStructure<String>> saveYoutubeUser(String youtubeUserDetails, QuantumShareUser user,
			String accessToken, String refreshToken, int userId) {
		try {
			JsonNode rootNode = objectMapper.readTree(youtubeUserDetails);
			SocialAccounts accounts = user.getSocialAccounts();
			if (accounts == null) {
				JsonNode itemsNode = rootNode.path("items").get(0);
				JsonNode snippetNode = itemsNode.path("snippet");
				YoutubeUser youtubeUser = new YoutubeUser();
				youtubeUser.setYoutubeUserAccessToken(accessToken);
				youtubeUser.setYoutubeUserRefreshToken(refreshToken);
				youtubeUser.setYtUserTokenIssuedTime(Instant.now());
				youtubeUser.setYoutubeChannelId(itemsNode.path("id").asText());
				youtubeUser.setChannelName(snippetNode.path("title").asText());
				youtubeUser.setSubscriberCount(itemsNode.path("statistics").path("subscriberCount").asInt());
				youtubeUser.setChannelImageUrl(snippetNode.path("thumbnails").path("default").path("url").asText());
				SocialAccounts socialAccounts = new SocialAccounts();
				socialAccounts.setYoutubeUser(youtubeUser);
				user.setSocialAccounts(socialAccounts);
			} else if (accounts.getYoutubeUser() == null) {
				JsonNode itemsNode = rootNode.path("items").get(0);
				JsonNode snippetNode = itemsNode.path("snippet");
				YoutubeUser youtubeUser = new YoutubeUser();
				youtubeUser.setYoutubeUserAccessToken(accessToken);
				youtubeUser.setYoutubeUserRefreshToken(refreshToken);
				youtubeUser.setYtUserTokenIssuedTime(Instant.now());
				youtubeUser.setYoutubeChannelId(itemsNode.path("id").asText());
				youtubeUser.setChannelName(snippetNode.path("title").asText());
				youtubeUser.setSubscriberCount(itemsNode.path("statistics").path("subscriberCount").asInt());
				youtubeUser.setChannelImageUrl(snippetNode.path("thumbnails").path("default").path("url").asText());
				accounts.setYoutubeUser(youtubeUser);
				user.setSocialAccounts(accounts);
			} else {
				YoutubeUser ytUser = accounts.getYoutubeUser();
				JsonNode itemsNode = rootNode.path("items").get(0);
				JsonNode snippetNode = itemsNode.path("snippet");
				ytUser.setYoutubeUserAccessToken(accessToken);
				ytUser.setYoutubeUserRefreshToken(refreshToken);
				ytUser.setYtUserTokenIssuedTime(Instant.now());
				ytUser.setYoutubeChannelId(itemsNode.path("id").asText());
				ytUser.setChannelName(snippetNode.path("title").asText());
				ytUser.setSubscriberCount(itemsNode.path("statistics").path("subscriberCount").asInt());
				ytUser.setChannelImageUrl(snippetNode.path("thumbnails").path("default").path("url").asText());
				accounts.setYoutubeUser(ytUser);
				user.setSocialAccounts(accounts);
			}
			userDao.save(user);
			ResponseStructure<String> structure = new ResponseStructure<String>();
			structure.setCode(HttpStatus.OK.value());
			structure.setStatus("success");
			structure.setMessage("Youtube Connected Successfully");
			structure.setPlatform("youtube");

			YoutubeUser yUser = user.getSocialAccounts().getYoutubeUser();
			Map<String, Object> map = config.getMap();
			map.put("youtubeChannelName", yUser.getChannelName());
			map.put("youtubeSubscriberCount", yUser.getSubscriberCount());
			map.put("youtubeUrl", yUser.getChannelImageUrl());
			map.put("user_id", userId);
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

	public ResponseEntity<ResponseStructure<Map<String, String>>> ytCheckAndRefreshAccessToken(QuantumShareUser user) {
		SocialAccounts account = user.getSocialAccounts();
		if(account==null)
			return null;
		
		YoutubeUser youtubeUser =account.getYoutubeUser();
		ResponseStructure<Map<String, String>> responseStructure = new ResponseStructure<>();
		
	if (youtubeUser == null) {
			responseStructure.setMessage("No YouTube user connected");
			responseStructure.setStatus("error");
			responseStructure.setData(Collections.singletonMap("status", "failure"));
			return new ResponseEntity<>(responseStructure, HttpStatus.BAD_REQUEST);
		}
		
	    if (youtubeUser.getYoutubeUserRefreshToken() == null || youtubeUser.getYtUserTokenIssuedTime() == null) {
	    	logoutController.disconnectYoutube();
	    }

		Instant now = Instant.now();
		Instant expiryTime = youtubeUser.getYtUserTokenIssuedTime().plusSeconds(1 * 60 * 60);

		// Check if token is expired or about to expire within the next 5 minutes
		if (now.isAfter(expiryTime.minus(Duration.ofMinutes(5)))) {
			try {
				// Refresh token logic
				MultiValueMap<String, String> requestMap = new LinkedMultiValueMap<>();
				requestMap.add("client_id", clientId);
				requestMap.add("client_secret", clientSecret);
				requestMap.add("refresh_token", youtubeUser.getYoutubeUserRefreshToken());
				requestMap.add("grant_type", "refresh_token");

				HttpEntity<MultiValueMap<String, String>> httpRequest = new HttpEntity<>(requestMap, new HttpHeaders());

				ResponseEntity<String> response = restTemplate.exchange(tokenUri, HttpMethod.POST, httpRequest,
						String.class);

				if (response.getStatusCode().is2xxSuccessful()) {
					JsonNode responseBody = objectMapper.readTree(response.getBody());
					String newAccessToken = responseBody.has("access_token") ? responseBody.get("access_token").asText()
							: null;
					String newRefreshToken = responseBody.has("refresh_token")
							? responseBody.get("refresh_token").asText()
							: youtubeUser.getYoutubeUserRefreshToken();

					// Update user with new token details
					youtubeUser.setYoutubeUserAccessToken(newAccessToken);
					youtubeUser.setYoutubeUserRefreshToken(newRefreshToken);
					youtubeUser.setYtUserTokenIssuedTime(Instant.now());
					userDao.save(user);
					responseStructure.setMessage("Access token refreshed successfully");
					responseStructure.setStatus("success");
//		                Map<String, String> data = new HashMap<>();
//		                data.put("status", "success");
//		                data.put("newAccessToken", newAccessToken);
//		                data.put("newRefreshToken", newRefreshToken);
					responseStructure.setData(null);
					return new ResponseEntity<>(responseStructure, HttpStatus.OK);
				}
			} catch (Exception e) {
				responseStructure.setMessage("Error refreshing YouTube access token: " + e.getMessage());
				responseStructure.setStatus("error");
				responseStructure.setData(null);
				return new ResponseEntity<>(responseStructure, HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}
		// Token is still valid
		responseStructure.setMessage("Access token is still valid");
		responseStructure.setStatus("success");
		responseStructure.setData(null);
		return new ResponseEntity<>(responseStructure, HttpStatus.OK);
	}

	// Media Posting
	public ResponseEntity<ResponseWrapper> postMediaToChannel(MediaPost mediaPost, MultipartFile mediaFile,
			YoutubeUser ytubeuser, int userId) {
		try {
			if (ytubeuser == null) {
				structure.setMessage("Youtube user not found");
				structure.setCode(HttpStatus.NOT_FOUND.value());
				structure.setPlatform("youtube");
				structure.setStatus("error");
				structure.setData(null);
				return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(structure), HttpStatus.NOT_FOUND);
			}
			String youtubeChannelId = ytubeuser.getYoutubeChannelId();
			String contentType = mediaFile.getContentType();
			String visibility = mediaPost.getVisibility();

			if (visibility == null || (!visibility.equals("public") && !visibility.equals("private")
					&& !visibility.equals("unlisted"))) {
				structure.setMessage("Invalid visibility status. Must be 'public', 'private', or 'unlisted'.");
				structure.setCode(HttpStatus.BAD_REQUEST.value());
				structure.setPlatform("youtube");
				structure.setStatus("error");
				structure.setData(null);
				return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(structure),
						HttpStatus.BAD_REQUEST);
			}

			if (contentType != null && contentType.startsWith("video/")) {
				return sendVideoToChannel(youtubeChannelId, mediaFile, mediaPost.getTitle(), mediaPost.getCaption(),
						userId, visibility);
			} else {
				structure.setMessage("Youtube: Unsupported media type");
				structure.setCode(HttpStatus.BAD_REQUEST.value());
				structure.setPlatform("youtube");
				structure.setStatus("error");
				structure.setData(null);
				return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(structure),
						HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			structure.setMessage("Failed to send media: " + e.getMessage());
			structure.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
			structure.setPlatform("youtube");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(structure),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	public ResponseEntity<ResponseWrapper> sendVideoToChannel(String youtubeChannelId, MultipartFile mediaFile,
			String title, String caption, int userId, String visibility) throws IOException {
		String uploadUrl = "https://www.googleapis.com/upload/youtube/v3/videos?part=snippet,status";

		headers.setContentType(MediaType.MULTIPART_FORM_DATA);

		String snippetJson = "{" + "\"snippet\": {" + "\"channelId\": \"" + youtubeChannelId + "\"," + "\"title\": \""
				+ title + "\"," + "\"description\": \"" + caption + "\"" + "}," + "\"status\": {"
				+ "\"privacyStatus\": \"" + visibility + "\"" + "}" + "}";

		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("snippet", new ByteArrayResource(snippetJson.getBytes()) {
			@Override
			public String getFilename() {
				return "snippet.json";
			}
		});

		body.add("file", new ByteArrayResource(mediaFile.getBytes()) {
			@Override
			public String getFilename() {
				return mediaFile.getOriginalFilename();
			}
		});
		HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

		ResponseEntity<String> response = restTemplate.exchange(uploadUrl, HttpMethod.POST, requestEntity,
				String.class);

		String channelName = youtubeUser.getChannelName();
		if (response.getStatusCode().is2xxSuccessful()) {
			QuantumShareUser qsuser = userDao.fetchUser(userId);
			CreditSystem credits = qsuser.getCreditSystem();
			credits.setRemainingCredit(credits.getRemainingCredit() - 1);
			qsuser.setCreditSystem(credits);
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode responseBody = objectMapper.readTree(response.getBody());
			String postid = responseBody.get("id").asText();
			analyticsPostService.savePost(postid, youtubeChannelId, qsuser, mediaFile.getContentType(), "youtube",
					channelName);
			userDao.save(qsuser);

			successResponse.setMessage("Posted On Youtube");
			successResponse.setCode(HttpStatus.OK.value());
			successResponse.setPlatform("youtube");
			successResponse.setStatus("success");
			successResponse.setData(responseBody);
			return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(successResponse), HttpStatus.OK);
		} else {
			errorResponse.setMessage("Request Failed to process");
			errorResponse.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
			errorResponse.setPlatform("youtube");
			errorResponse.setStatus("error");
			errorResponse.setData(null);
			return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(errorResponse),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

}