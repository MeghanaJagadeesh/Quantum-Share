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

	@Autowired
	AnalyticsPostService analyticsPostService;

	@Autowired
	SocialMediaLogoutService mediaLogoutService;

	public ResponseEntity<ResponseWrapper> postMediaToPage(MediaPost mediaPost, MultipartFile mediaFile,

			InstagramUser instagramUser, QuantumShareUser user) {
		String accessToken = instagramUser.getInstUserAccessToken();
		String fileUrl = uploadFileToServer.uploadFile(mediaFile);
		String instaId = instagramUser.getInstaUserId();
		String profileName = instagramUser.getInstaUsername();
		if (mediaPost.getCaption() == null)
			mediaPost.setCaption(" ");
		if (mediaFile.getContentType().startsWith("image")) {
			if (mediaFile.getContentType().equals("image/jpeg") || mediaFile.getContentType().equals("image/png")
					|| mediaFile.getContentType().equals("image/jpg")) {
				return postImageToMedia(instaId, fileUrl, mediaPost.getCaption(), accessToken, user, profileName);
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
				return postVideoToMedia(instaId, fileUrl, mediaPost.getCaption(), accessToken, user, profileName);
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
			String accessToken, QuantumShareUser user, String profileName) {
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
				System.out.println("video " + response);
				if (response.isSuccess()) {
					user.setCredit(user.getCredit() - 1);
					userDao.save(user);
					analyticsPostService.savePost(response.getId(), instagramUserId, user, "video", "instagram",
							profileName);
					successResponse.setCode(HttpStatus.OK.value());
					successResponse.setMessage("Posted On Instagram");
					successResponse.setStatus("success");
					successResponse.setData(response);
					successResponse.setPlatform("instagram");
					successResponse.setRemainingCredits(user.getCredit());
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
		} 
		catch (FacebookException e) {
			if (e.getMessage().contains("Error validating access token: Session has expired")) {
//				mediaLogoutService.disconnectInstagram(user);
				structure.setCode(118);
				structure.setMessage("Access Expiry!! Please Connect your Instagram profile");
				structure.setPlatform("instagram");
				structure.setStatus("error");
				structure.setData(e.getMessage());
				return new ResponseEntity<ResponseWrapper>(configuration.getResponseWrapper(structure), HttpStatus.OK);
			}
			throw new FBException(e.getMessage(), "instagram");
		}
		catch (IllegalArgumentException e) {
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
			String accessToken, QuantumShareUser user, String profileName) {
		System.out.println(fileUrl);
		try {
			FacebookClient client = configuration.getFacebookClient(accessToken);
			GraphResponse container = client.publish(instagramUserId + "/media", GraphResponse.class,
					Parameter.with("image_url", fileUrl), Parameter.with("caption", caption),
					Parameter.with("media_type", "IMAGE"));
			String containerId = container.getId();
			GraphResponse response = client.publish(instagramUserId + "/media_publish", GraphResponse.class,
					Parameter.with("creation_id", containerId));
			System.out.println(response);
			if (response.isSuccess()) {

				user.setCredit(user.getCredit() - 1);
				userDao.save(user);
				analyticsPostService.savePost(response.getId(), instagramUserId, user, "image", "instagram",
						profileName);
				successResponse.setCode(HttpStatus.OK.value());
				successResponse.setMessage("Posted On Instagram");
				successResponse.setStatus("success");
				successResponse.setData(response);
				successResponse.setPlatform("instagram");
				System.out.println("instagram " + user.getCredit());
				successResponse.setRemainingCredits(user.getCredit());
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
			if (e.getMessage().contains("Error validating access token: Session has expired")) {
//				mediaLogoutService.disconnectInstagram(user);
				structure.setCode(118);
				structure.setMessage("Access Expiry!! Please Connect your Instagram profile");
				structure.setPlatform("instagram");
				structure.setStatus("error");
				structure.setData(e.getMessage());
				return new ResponseEntity<ResponseWrapper>(configuration.getResponseWrapper(structure), HttpStatus.OK);
			}
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
		return saveInstaUser(instaId, username, profile, access_token, user, instaUser);
	}

	private ResponseEntity<ResponseStructure<String>> saveInstaUser(String instaId, String username,
			ResponseEntity<String> profile, String access_token, QuantumShareUser user, JsonNode instaUser) {
		try {
			if (user.getSocialAccounts() == null) {
				instagramUser.setInstaUserId(instaId);
				instagramUser.setInstaUsername(username.replace("\"", ""));
				instagramUser.setFollwersCount(instaUser.get("followers_count").asInt());
				JsonNode jsonResponse = objectMapper.readTree(profile.getBody());
				instagramUser.setPictureUrl(
						jsonResponse.has("profile_picture_url") ? jsonResponse.get("profile_picture_url").asText()
								: null);
				instagramUser.setInstUserAccessToken(access_token);
				socialAccounts.setInstagramUser(instagramUser);
				user.setSocialAccounts(socialAccounts);
			} else if (user.getSocialAccounts().getInstagramUser() == null) {
				instagramUser.setInstaUserId(instaId);
				instagramUser.setInstaUsername(username.replace("\"", ""));
				instagramUser.setFollwersCount(instaUser.get("followers_count").asInt());
				JsonNode jsonResponse = objectMapper.readTree(profile.getBody());
				instagramUser.setPictureUrl(
						jsonResponse.has("profile_picture_url") ? jsonResponse.get("profile_picture_url").asText()
								: null);
				instagramUser.setInstUserAccessToken(access_token);
				SocialAccounts accounts = user.getSocialAccounts();
				accounts.setInstagramUser(instagramUser);
				user.setSocialAccounts(accounts);
			} else {
				SocialAccounts socialAccount = user.getSocialAccounts();
				InstagramUser exInstaUser = socialAccount.getInstagramUser();

				exInstaUser.setInstaUserId(instaId);
				exInstaUser.setInstaUsername(username.replace("\"", ""));
				exInstaUser.setFollwersCount(instaUser.get("followers_count").asInt());
				JsonNode jsonResponse = objectMapper.readTree(profile.getBody());
				exInstaUser.setPictureUrl(
						jsonResponse.has("profile_picture_url") ? jsonResponse.get("profile_picture_url").asText()
								: null);
				exInstaUser.setInstUserAccessToken(access_token);

				socialAccount.setInstagramUser(exInstaUser);
				user.setSocialAccounts(socialAccount);
			}

			userDao.save(user);

			structure.setCode(HttpStatus.CREATED.value());
			structure.setMessage("Instagram Connected Successfully");
			structure.setStatus("success");
			structure.setPlatform("instagram");
			Map<String, Object> data = configuration.getMap();
			data.clear();
			InstagramUser datauser = instagramUserDao
					.findById(user.getSocialAccounts().getInstagramUser().getInstaId());
			String instagramUrl;
			if (datauser.getPictureUrl() == null) {
				instagramUrl = "https://quantumshare.quantumparadigm.in/vedio/ProfilePicture.jpg";
			} else {
				instagramUrl = datauser.getPictureUrl();
			}
			data.put("instagramUrl", instagramUrl);
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
			String fetchAPI = "https://graph.facebook.com/v19.0/" + instaId
					+ "?fields=profile_picture_url,ig_id,media_count,username";
			headers.setBearerAuth(access_token);
			HttpEntity<String> requestEntity = configuration.getHttpEntity(headers);
			ResponseEntity<String> response = restTemplate.exchange(fetchAPI, HttpMethod.GET, requestEntity,
					String.class);
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
			JsonNode responseJson = objectMapper.readTree(response.getBody());
			JsonNode data = responseJson.get("data");
			if (data != null && data.isArray() && data.size() > 0) {
//				JsonNode instagramIdNode = data.get("instagram_business_account").get("id");
				JsonNode instagramIdNode = null;
				for (JsonNode itemNode : data) {
					instagramIdNode = extractBusinessAccountIdFromItem(itemNode);
					if (instagramIdNode != null && instagramIdNode.isTextual()) {
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
