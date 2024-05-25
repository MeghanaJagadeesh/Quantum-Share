package com.qp.quantum_share.services;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException.BadRequest;
import org.springframework.web.client.HttpServerErrorException.InternalServerError;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qp.quantum_share.configuration.ConfigurationClass;
import com.qp.quantum_share.dao.InstagramUserDao;
import com.qp.quantum_share.dao.QuantumShareUserDao;
import com.qp.quantum_share.dao.SocialAccountDao;
import com.qp.quantum_share.dto.InstagramUser;
import com.qp.quantum_share.dto.MediaPost;
import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.dto.SocialAccounts;
import com.qp.quantum_share.exception.BadRequestException;
import com.qp.quantum_share.exception.CommonException;
import com.qp.quantum_share.exception.FBException;
import com.qp.quantum_share.helper.GenerateId;
import com.qp.quantum_share.helper.UploadFileToServer;
import com.qp.quantum_share.response.ErrorResponse;
import com.qp.quantum_share.response.ResponseStructure;
import com.qp.quantum_share.response.ResponseWrapper;
import com.qp.quantum_share.response.SuccessResponse;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.exception.FacebookException;
import com.restfb.json.JsonObject;
import com.restfb.types.GraphResponse;

@Service
public class InstagramService {

	@Autowired
	UploadFileToServer uploadFileToServer;

	@Autowired
	ResponseStructure<String> structure;

	@Autowired
	ConfigurationClass configuration;

	@Autowired
	SuccessResponse successResponse;

	@Autowired
	ErrorResponse errorResponse;

	@Autowired
	HttpHeaders headers;

	@Autowired
	RestTemplate restTemplate;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	InstagramUser instagramUser;

	@Autowired
	InstagramUserDao instagramUserDao;

	@Autowired
	GenerateId generateId;

	@Autowired
	SocialAccounts socialAccounts;

	@Autowired
	QuantumShareUserDao userDao;

	@Autowired
	SocialAccountDao accountDao;

	public ResponseEntity<ResponseWrapper> postMediaToPage(MediaPost mediaPost, MultipartFile mediaFile,
			InstagramUser instagramUser) {
		System.out.println("post method");
		String accessToken = instagramUser.getInstUserAccessToken();
		String fileUrl = uploadFileToServer.uploadFile(mediaFile);
		String instaId = instagramUser.getInstaUserId();
		if (mediaPost.getCaption() == null)
			mediaPost.setCaption(" ");
		if (mediaFile.getContentType().startsWith("image")) {
			if (mediaFile.getContentType().equals("image/jpeg") || mediaFile.getContentType().equals("image/png")
					|| mediaFile.getContentType().equals("image/jpg")) {
				return postImageToMedia(instaId, fileUrl, mediaPost.getCaption(), accessToken);
			} else {
				structure.setCode(HttpStatus.BAD_REQUEST.value());
				structure.setMessage("Invalid File Type. Accepted image types are JPG, PNG, and JPEG.");
				structure.setStatus("error");
				structure.setData(null);
				structure.setPlatform("instagram");
				return new ResponseEntity<ResponseWrapper>(configuration.getResponseWrapper(structure),
						HttpStatus.BAD_REQUEST);
			}
		} else if (mediaFile.getContentType().startsWith("video")) {
			if (mediaFile.getContentType().equals("video/mp4")) {
				return postVideoToMedia(instaId, fileUrl, mediaPost.getCaption(), accessToken);
			} else {
				structure.setCode(HttpStatus.BAD_REQUEST.value());
				structure.setMessage("Invalid File Type. Accepted video types are JPG, PNG, and JPEG.");
				structure.setStatus("error");
				structure.setData(null);
				structure.setPlatform("instagram");
				return new ResponseEntity<ResponseWrapper>(configuration.getResponseWrapper(structure),
						HttpStatus.BAD_REQUEST);
			}
		} else {
			structure.setCode(HttpStatus.BAD_REQUEST.value());
			structure.setMessage("Invalid File Type");
			structure.setStatus("error");
			structure.setData(null);
			structure.setPlatform("instagram");
			return new ResponseEntity<ResponseWrapper>(configuration.getResponseWrapper(structure),
					HttpStatus.BAD_REQUEST);
		}
	}

	private ResponseEntity<ResponseWrapper> postVideoToMedia(String instagramUserId, String fileUrl, String caption,
			String accessToken) {
		try {
			FacebookClient client = configuration.getFacebookClient(accessToken);
			GraphResponse container = client.publish(instagramUserId + "/media", GraphResponse.class,
					Parameter.with("video_url", fileUrl), Parameter.with("caption", caption),
					Parameter.with("media_type", "REELS"));
			String containerId = container.getId();
			if (containerId != null) {
				String status = checkMediaStatus(containerId, accessToken);
				while (!status.equals("FINISHED")) {
					if (status.equals("ERROR")) {
						errorResponse.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
						errorResponse.setMessage("Request Failed");
						errorResponse.setStatus("error");
						errorResponse.setData(null);
						errorResponse.setPlatform("instagram");
						return new ResponseEntity<ResponseWrapper>(configuration.getResponseWrapper(errorResponse),
								HttpStatus.BAD_REQUEST);
					}
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					status = checkMediaStatus(containerId, accessToken);
				}
				GraphResponse response = client.publish(instagramUserId + "/media_publish", GraphResponse.class,
						Parameter.with("creation_id", containerId));
				if (response.isSuccess()) {
					successResponse.setCode(HttpStatus.OK.value());
					successResponse.setMessage("Posted On Instagram");
					successResponse.setStatus("success");
					successResponse.setData(response);
					successResponse.setPlatform("instagram");
					return new ResponseEntity<ResponseWrapper>(configuration.getResponseWrapper(successResponse),
							HttpStatus.OK);
				} else {
					errorResponse.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
					errorResponse.setMessage("Request Failed");
					errorResponse.setStatus("error");
					errorResponse.setData(response);
					errorResponse.setPlatform("instgram");
					return new ResponseEntity<ResponseWrapper>(configuration.getResponseWrapper(errorResponse),
							HttpStatus.INTERNAL_SERVER_ERROR);
				}
			} else {
				structure.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
				structure.setMessage("Request Failed");
				structure.setStatus("error");
				structure.setData(null);
				structure.setPlatform("instagram");
				return new ResponseEntity<ResponseWrapper>(configuration.getResponseWrapper(structure),
						HttpStatus.INTERNAL_SERVER_ERROR);

			}
		} catch (FacebookException e) {
			throw new FBException(e.getMessage(), "instagram");
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(e.getMessage());
		} catch (NullPointerException e) {
			throw new NullPointerException(e.getMessage());
		} catch (InternalServerError error) {
			throw new CommonException(error.getMessage());
		} catch (Exception e) {
			throw new CommonException(e.getMessage());
		}
	}

	private ResponseEntity<ResponseWrapper> postImageToMedia(String instagramUserId, String fileUrl, String caption,
			String accessToken) {
		try {
			System.out.println("instagram response");
			FacebookClient client = configuration.getFacebookClient(accessToken);
			GraphResponse container = client.publish(instagramUserId + "/media", GraphResponse.class,
					Parameter.with("image_url", fileUrl), Parameter.with("caption", caption),
					Parameter.with("media_type", "IMAGE"));
			String containerId = container.getId();
			GraphResponse response = client.publish(instagramUserId + "/media_publish", GraphResponse.class,
					Parameter.with("creation_id", containerId));
			System.out.println("instagram response : " + response);
			if (response.isSuccess()) {
				successResponse.setCode(HttpStatus.OK.value());
				successResponse.setMessage("Posted On Instagram");
				successResponse.setStatus("success");
				successResponse.setData(response);
				successResponse.setPlatform("instagram");
				return new ResponseEntity<ResponseWrapper>(configuration.getResponseWrapper(successResponse),
						HttpStatus.OK);
			} else {
				errorResponse.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
				errorResponse.setMessage("Request Failed");
				errorResponse.setStatus("error");
				errorResponse.setData(response);
				errorResponse.setPlatform("instagram");
				return new ResponseEntity<ResponseWrapper>(configuration.getResponseWrapper(errorResponse),
						HttpStatus.INTERNAL_SERVER_ERROR);
			}
		} catch (FacebookException e) {
			throw new FBException(e.getMessage(), "instagram");
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(e.getMessage());
		} catch (NullPointerException e) {
			throw new NullPointerException(e.getMessage());
		} catch (InternalServerError error) {
			throw new CommonException(error.getMessage());
		} catch (Exception e) {
			throw new CommonException(e.getMessage());
		}
	}

	private String checkMediaStatus(String containerId, String accessToken) {
		FacebookClient client = configuration.getFacebookClient(accessToken);
		JsonObject jsonObject = client.fetchObject(containerId, JsonObject.class,
				Parameter.with("fields", "status_code"));
		String status = jsonObject.get("status_code").asString();
		return status;
	}

	public ResponseEntity<ResponseStructure<String>> verifyToken(String access_token, QuantumShareUser user) {
		String instaId = fetchID(access_token);
		System.out.println("instaid  " + instaId);
		JsonNode instaUser = null;
		ResponseEntity<String> profile = null;
		if (instaId != null) {
			instaUser = fetchUsername(instaId, access_token);
		} else {
			structure.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
			structure.setMessage("Something went wrong, please try again later");
			structure.setPlatform("instagram");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		String username = instaUser.get("username").asText();
		if (instaId != null && username != null) {
			profile = fetchProfile(instaId, username, access_token);
		} else {
			structure.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
			structure.setMessage("Something went wrong, please try again later");
			structure.setPlatform("instagram");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		System.out.println("coming");
		return saveInstaUser(instaId, username, profile, access_token, user, instaUser);
	}

	private ResponseEntity<ResponseStructure<String>> saveInstaUser(String instaId, String username,
			ResponseEntity<String> profile, String access_token, QuantumShareUser user, JsonNode instaUser) {
		try {
			String lastUserId = instagramUserDao.findLastUserId();
			String id = null;
			System.out.println(user);
			if (user.getSocialAccounts() == null || user.getSocialAccounts().getInstagramUser() == null) {
				System.out.println("if block");
				id = generateId.generateinstaId(lastUserId);
			} else {
				id = user.getSocialAccounts().getInstagramUser().getInstaId();
			}
			instagramUser.setInstaId(id);
			instagramUser.setInstaUserId(instaId);
			instagramUser.setInstaUsername(username.replace("\"", ""));
			instagramUser.setFollwersCount(instaUser.get("followers_count").asInt());
			JsonNode jsonResponse = objectMapper.readTree(profile.getBody());
			instagramUser.setPictureUrl(
					jsonResponse.has("profile_picture_url") ? jsonResponse.get("profile_picture_url").asText() : null);
			instagramUser.setInstUserAccessToken(access_token);
//			instagramUserDao.save(instagramUser);
			SocialAccounts accounts = user.getSocialAccounts();
			System.out.println("accounts :" + accounts);
			if (accounts == null) {
				socialAccounts.setInstagramUser(instagramUser);
				user.setSocialAccounts(socialAccounts);
			} else {
				if (accounts.getInstagramUser() == null) {
					accounts.setInstagramUser(instagramUser);
					user.setSocialAccounts(accounts);
				}
			}

//			accountDao.save(socialAccounts);
//			user.setSocialAccounts(accounts);
			userDao.save(user);

			structure.setCode(HttpStatus.CREATED.value());
			structure.setMessage("Instagram Connected Successfully");
			structure.setStatus("success");
			structure.setPlatform("instagram");
			Map<String, Object> data = configuration.getMap();
			InstagramUser datauser = instagramUserDao
					.findById(user.getSocialAccounts().getInstagramUser().getInstaId());
			data.put("instagramUrl", datauser.getPictureUrl());
			data.put("InstagramUsername", datauser.getInstaUsername());
			data.put("Instagram_follwers_count", datauser.getFollwersCount());
			structure.setData(data);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.CREATED);

		} catch (JsonMappingException e) {
			throw new CommonException(e.getMessage());
		} catch (JsonProcessingException e) {
			throw new CommonException(e.getMessage());
		} catch (BadRequest e) {
			throw new BadRequestException(e.getMessage());
		} catch (InternalServerError e) {
			throw new CommonException(e.getMessage());
		} catch (Exception e) {
			throw new CommonException(e.getMessage());
		}

	}

	private ResponseEntity<String> fetchProfile(String instaId, String username, String access_token) {
		try {
			String fetchAPI = "https://graph.facebook.com/v19.0/" + instaId + "?fields=profile_picture_url,ig_id,media_count,username";
			System.out.println(fetchAPI);
			headers.setBearerAuth(access_token);
			HttpEntity<String> requestEntity = configuration.getHttpEntity(headers);
			ResponseEntity<String> response = restTemplate.exchange(fetchAPI, HttpMethod.GET, requestEntity,
					String.class);
			System.out.println(response);
			return response;
		} catch (BadRequest e) {
			throw new BadRequestException(e.getMessage());
		}

	}

	private JsonNode fetchUsername(String instaId, String access_token) {
		try {
			String fetchAPI = "https://graph.facebook.com/v19.0/" + instaId
					+ "?fields=id,name,followers_count,username,website,follows_count";
			headers.setBearerAuth(access_token);
			HttpEntity<String> requestEntity = configuration.getHttpEntity(headers);
			ResponseEntity<String> response = restTemplate.exchange(fetchAPI, HttpMethod.GET, requestEntity,
					String.class);
			System.out.println(response.getBody());
			JsonNode responseJson = objectMapper.readTree(response.getBody());
			return responseJson;

		} catch (JsonMappingException e) {
			throw new CommonException(e.getMessage());
		} catch (JsonProcessingException e) {
			throw new CommonException(e.getMessage());
		} catch (BadRequest e) {
			throw new BadRequestException(e.getMessage());
		}

	}

	private String fetchID(String access_token) {
		try {
			String fetchAPI = "https://graph.facebook.com/v19.0/me/accounts?fields=id,instagram_business_account,username&access_token="
					+ access_token;
			headers.setBearerAuth(access_token);
			HttpEntity<String> requestEntity = configuration.getHttpEntity(headers);
			ResponseEntity<String> response = restTemplate.exchange(fetchAPI, HttpMethod.GET, requestEntity,
					String.class);
			System.out.println(response.getBody());
			JsonNode responseJson = objectMapper.readTree(response.getBody());
			JsonNode data = responseJson.get("data");
			if (data != null && data.isArray() && data.size() > 0) {
//				JsonNode instagramIdNode = data.get("instagram_business_account").get("id");
//				System.out.println("instagramIdNode "+instagramIdNode.asText());
				JsonNode instagramIdNode = null;
				for (JsonNode itemNode : data) {
					instagramIdNode = extractBusinessAccountIdFromItem(itemNode);
					if (instagramIdNode != null && instagramIdNode.isTextual()) {
						System.out.println("xcc");
						return instagramIdNode.asText();
					}
				}
			}
		} catch (JsonMappingException e) {
			throw new CommonException(e.getMessage());
		} catch (JsonProcessingException e) {
			throw new CommonException(e.getMessage());
		} catch (BadRequest e) {
			throw new BadRequestException(e.getMessage());
		}
		return null;

	}

	private JsonNode extractBusinessAccountIdFromItem(JsonNode itemNode) {
		JsonNode businessAccountNode = itemNode.get("instagram_business_account");
		if (businessAccountNode != null && businessAccountNode.isObject()) {
			JsonNode businessAccountIdNode = businessAccountNode.get("id");
			if (businessAccountIdNode != null && businessAccountIdNode.isTextual()) {
				return businessAccountIdNode;
			} else {
				return null;
			}
		} else {
			return null;
		}
	}
}
