package com.qp.quantum_share.services;

import java.io.IOException;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.qp.quantum_share.configuration.ConfigurationClass;
import com.qp.quantum_share.dao.QuantumShareUserDao;
import com.qp.quantum_share.dto.MediaPost;
import com.qp.quantum_share.dto.PinterestBoard;
import com.qp.quantum_share.dto.PinterestUser;
import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.dto.SocialAccounts;
import com.qp.quantum_share.exception.CommonException;
import com.qp.quantum_share.response.ErrorResponse;
import com.qp.quantum_share.response.ResponseStructure;
import com.qp.quantum_share.response.ResponseWrapper;
import com.qp.quantum_share.response.SuccessResponse;

@Service
public class PinterestService {

	@Value("${pinterest.client.id}")
	private String clientId;

	@Value("${pinterest.client.secret.key}")
	private String clientSecret;

	@Value("${pinterest.redirect.uri}")
	private String redirectUri;

	@Value("${pinterest.token.uri}")
	private String tokenUri;

	@Value("${pinterest.scope}")
	private String scope;

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
	MultiValueMap<String, Object> linkedMultiValueMap;

	@Autowired
	ConfigurationClass.ByteArrayResourceFactory byteArrayResourceFactory;

	@Autowired
	RestTemplate restTemplate;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	PinterestUser pinterestUser;

	@Autowired
	SocialAccounts socialAccounts;

	@Autowired
	QuantumShareUserDao userDao;

	public ResponseEntity<ResponseStructure<String>> getPinterestAuthorizationUrl(QuantumShareUser user) {
		String authUri = "https://www.pinterest.com/oauth/";
		String pinterestOauth = authUri + "?response_type=code" + "&client_id=" + clientId + "&redirect_uri="
				+ redirectUri + "&scope=" + scope;

		ResponseStructure<String> structure = new ResponseStructure<String>();
		structure.setCode(HttpStatus.OK.value());
		structure.setStatus("success");
		structure.setMessage("Pinterest oauth_url generated successfully");
		structure.setPlatform("pinterest");
		structure.setData(pinterestOauth);

		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
	}

	public ResponseEntity<ResponseStructure<String>> pinterestVerifyToken(String code, QuantumShareUser user,
			int userId) {
		try {
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			headers.setBasicAuth(clientId, clientSecret);
			multiValueMap.clear();
			multiValueMap.add("code", code);
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
					String pinterestUserDetails = fetchProfileDetails(accessToken);
					return savePinterestUser(pinterestUserDetails, user, accessToken, refreshToken, userId);
				} else {
					throw new CommonException("Access token not found in response");
				}
			} else {
				structure.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
				structure.setData(null);
				structure.setMessage("Something went wrong!!");
				structure.setPlatform("pinterest");
				structure.setStatus("error");
				return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.INTERNAL_SERVER_ERROR);
			}
		} catch (JsonProcessingException e) {
			throw new CommonException(e.getMessage());
		} catch (Exception e) {
			throw new CommonException(e.getMessage());
		}
	}

	private String fetchProfileDetails(String accessToken) {
		try {
			String userAccountUrl = "https://api-sandbox.pinterest.com/v5/user_account";
			headers.setBearerAuth(accessToken);
			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<String> httpRequest = config.getHttpEntity(headers);
			ResponseEntity<String> userResponse = restTemplate.exchange(userAccountUrl, HttpMethod.GET, httpRequest,
					String.class);

			if (userResponse.getStatusCode() == HttpStatus.OK) {
				JsonNode userProfileDetails = objectMapper.readTree(userResponse.getBody());
				String boardsUrl = "https://api-sandbox.pinterest.com/v5/boards";
				ResponseEntity<String> boardsResponse = restTemplate.exchange(boardsUrl, HttpMethod.GET, httpRequest,
						String.class);
				if (boardsResponse.getStatusCode() == HttpStatus.OK) {
					JsonNode boardsDetails = objectMapper.readTree(boardsResponse.getBody());
					ObjectNode combinedDetails = objectMapper.createObjectNode();
					combinedDetails.set("userProfile", userProfileDetails);
					combinedDetails.set("boards", boardsDetails);
					return combinedDetails.toString();
				} else {
					throw new CommonException("Failed to fetch Pinterest boards");
				}
			} else {
				throw new CommonException("Failed to fetch Pinterest user profile");
			}
		} catch (Exception e) {
			throw new CommonException("Error fetching Pinterest details: " + e.getMessage());
		}
	}

	private ResponseEntity<ResponseStructure<String>> savePinterestUser(String pinterestUserDetails,
			QuantumShareUser user, String accessToken, String refreshToken, int userId) {
		try {
			JsonNode userDetailsNode = objectMapper.readTree(pinterestUserDetails);
			SocialAccounts accounts = user.getSocialAccounts();
			JsonNode boardsNode = userDetailsNode.path("boards").path("items");
			ArrayNode boardArray = objectMapper.createArrayNode();
			if (boardsNode.isArray() && boardsNode.size() > 0) {
				for (JsonNode board : boardsNode) {
					ObjectNode boardDetails = objectMapper.createObjectNode();
					boardDetails.put("boardName", board.path("name").asText(""));
					boardDetails.put("boardId", board.path("id").asText(""));
					boardArray.add(boardDetails);
				}
			}
			String boardDetailsJson = boardArray.toString();
			if (accounts == null) {
				PinterestUser pinterestUser = new PinterestUser();
				pinterestUser.setPinterestProfileId(userDetailsNode.path("userProfile").path("id").asText());
				pinterestUser.setPinterestProfileName(userDetailsNode.path("userProfile").path("business_name").asText());
				pinterestUser.setPinterestProfileImage(userDetailsNode.path("userProfile").path("profile_image").asText());
				pinterestUser.setPinterestFollowersCount(userDetailsNode.path("userProfile").path("follower_count").asInt());
				pinterestUser.setPinterestBoardDetails(boardDetailsJson);
				pinterestUser.setPinterestUserAccessToken(accessToken);
				pinterestUser.setPinterestUserRefreshToken(refreshToken);
				pinterestUser.setPinterestUserTokenIssuedTime(Instant.now());
				socialAccounts.setPinterestUser(pinterestUser);
				user.setSocialAccounts(socialAccounts);
			} else if (accounts.getPinterestUser() == null) {
				PinterestUser pinterestUser = new PinterestUser();
				pinterestUser.setPinterestProfileId(userDetailsNode.path("userProfile").path("id").asText());
				pinterestUser.setPinterestProfileName(userDetailsNode.path("userProfile").path("business_name").asText());
				pinterestUser.setPinterestProfileImage(userDetailsNode.path("userProfile").path("profile_image").asText());
				pinterestUser.setPinterestFollowersCount(userDetailsNode.path("userProfile").path("follower_count").asInt());
				pinterestUser.setPinterestBoardDetails(boardDetailsJson);
				pinterestUser.setPinterestUserAccessToken(accessToken);
				pinterestUser.setPinterestUserRefreshToken(refreshToken);
				pinterestUser.setPinterestUserTokenIssuedTime(Instant.now());
				SocialAccounts socialAccounts = new SocialAccounts();
				socialAccounts.setPinterestUser(pinterestUser);
				user.setSocialAccounts(socialAccounts);
			} else {
				PinterestUser ptUser = accounts.getPinterestUser();
				ptUser.setPinterestProfileId(userDetailsNode.path("userProfile").path("id").asText());
				ptUser.setPinterestProfileName(userDetailsNode.path("userProfile").path("business_name").asText());
				ptUser.setPinterestProfileImage(userDetailsNode.path("userProfile").path("profile_image").asText());
				ptUser.setPinterestFollowersCount(userDetailsNode.path("userProfile").path("follower_count").asInt());
				ptUser.setPinterestBoardDetails(boardDetailsJson);
				ptUser.setPinterestUserAccessToken(accessToken);
				ptUser.setPinterestUserRefreshToken(refreshToken);
				ptUser.setPinterestUserTokenIssuedTime(Instant.now());
				SocialAccounts socialAccounts = new SocialAccounts();
				socialAccounts.setPinterestUser(ptUser);
				user.setSocialAccounts(socialAccounts);
			}
			userDao.save(user);
			ResponseStructure<String> structure = new ResponseStructure<String>();
			structure.setCode(HttpStatus.OK.value());
			structure.setStatus("success");
			structure.setMessage("Pinterest Connected Successfully");
			structure.setPlatform("pinterest");

			PinterestUser pUser = user.getSocialAccounts().getPinterestUser();
			Map<String, Object> map = config.getMap();
			map.put("pinterestProfileName", pUser.getPinterestProfileName());
			map.put("pinterestFollowersCount", pUser.getPinterestFollowersCount());
			map.put("pinterestProfileImage", pUser.getPinterestProfileImage());
			map.put("pinterestBoardDetails", pUser.getPinterestBoardDetails());
			map.put("user_id", userId);
			structure.setData(map);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
		} catch (NullPointerException e) {
			throw new CommonException(e.getMessage());
		} catch (JsonMappingException e) {
			throw new CommonException(e.getMessage());
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			throw new CommonException(e.getMessage());
		}
	}

	// Media Posting
	public ResponseEntity<ResponseWrapper> postMediaToProfile(MediaPost mediaPost, MultipartFile mediaFile,
			PinterestUser pinterestUser, int userId) {
		try {
			if (pinterestUser == null) {
				structure.setMessage("Pinterest user not found");
				structure.setCode(HttpStatus.NOT_FOUND.value());
				structure.setPlatform("pinterest");
				structure.setStatus("error");
				structure.setData(null);
				return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(structure), HttpStatus.NOT_FOUND);
			}

			String pinterestBoardDetails = pinterestUser.getPinterestBoardDetails();
			
			Map<String, Object> responseData = new HashMap<>();
			responseData.put("boardDetails", pinterestBoardDetails);

			String boardId = getBoardIdForBoardName(pinterestUser.getPinterestBoardDetails(), mediaPost.getBoardName());
			if (boardId == null) {
				structure.setMessage("Please select the Pinterest Board");
				structure.setCode(HttpStatus.NOT_FOUND.value());
				structure.setPlatform("pinterest");
				structure.setStatus("error");
				structure.setData(responseData);
				return new ResponseEntity<>(config.getResponseWrapper(structure), HttpStatus.NOT_FOUND);
			}

			String contentType = mediaFile.getContentType();
			if (contentType != null && contentType.startsWith("image/")) {
				return sendImageToProfile(pinterestUser, boardId, mediaFile, mediaPost.getTitle(),
						mediaPost.getCaption(), mediaPost.getBoardName(), userId);
			} else {
				structure.setMessage("Pinterest: Unsupported media type");
				structure.setCode(HttpStatus.BAD_REQUEST.value());
				structure.setPlatform("pinterest");
				structure.setStatus("error");
				structure.setData(null);
				return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(structure),
						HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			structure.setMessage("Failed to send media: " + e.getMessage());
			structure.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
			structure.setPlatform("pinterest");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(structure),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private String getBoardIdForBoardName(String pinterestBoardDetailsJson, String boardName) {
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			List<PinterestBoard> boards = objectMapper.readValue(pinterestBoardDetailsJson, new TypeReference<>() {
			});
			
			for (PinterestBoard board : boards) {
				if (board.getBoardName().equalsIgnoreCase(boardName)) {
					return board.getBoardId();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private ResponseEntity<ResponseWrapper> sendImageToProfile(PinterestUser ptUser, String boardId,
			MultipartFile mediaFile, String title, String caption, String boardName, int userId) throws IOException {
		String accessToken = ptUser.getPinterestUserAccessToken();
		String pinterestApiUrl = "https://api-sandbox.pinterest.com/v5/pins";

		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Authorization", "Bearer " + accessToken);
		
		String encodedImageData = Base64.getEncoder().encodeToString(mediaFile.getBytes());
		Map<String, Object> mediaSource = new HashMap<>();
		mediaSource.put("source_type", "image_base64");
		mediaSource.put("data", encodedImageData);
		mediaSource.put("content_type", mediaFile.getContentType());
		mediaSource.put("is_standard", true);

		Map<String, Object> requestBody = new HashMap<>();
		requestBody.put("title", title);
		requestBody.put("description", caption);
		requestBody.put("board_id", boardId);
		requestBody.put("media_source", mediaSource);
		
		HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
		ResponseEntity<String> response = restTemplate.postForEntity(pinterestApiUrl, requestEntity, String.class);
		
		if (response.getStatusCode() == HttpStatus.CREATED) {
			successResponse.setMessage("Posted On Pinterest");
			successResponse.setCode(HttpStatus.OK.value());
			successResponse.setPlatform("pinterest");
			successResponse.setStatus("success");
			successResponse.setData(null);
			return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(successResponse), HttpStatus.OK);
		} else {
			errorResponse.setMessage("Request Failed to process");
			errorResponse.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
			errorResponse.setPlatform("pinterest");
			errorResponse.setStatus("error");
			errorResponse.setData(null);
			return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(errorResponse),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

}