package com.qp.quantum_share.services;

import java.util.ArrayList;
import java.util.List;
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
import com.qp.quantum_share.dto.CreditSystem;
import com.qp.quantum_share.dto.InstagramUser;
import com.qp.quantum_share.dto.MediaPost;
import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.dto.SocialAccounts;
import com.qp.quantum_share.exception.BadRequestException;
import com.qp.quantum_share.exception.CommonException;
import com.qp.quantum_share.exception.FBException;
import com.qp.quantum_share.helper.GenerateId;
import com.qp.quantum_share.helper.PostOnServer;
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
	ConfigurationClass configuration;

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

	@Autowired
	PostOnServer postOnServer;

	public ResponseEntity<ResponseWrapper> postMediaToPage(MediaPost mediaPost, MultipartFile[] file,
			InstagramUser instagramUser, int userId) {
		String accessToken = instagramUser.getInstUserAccessToken();
		ResponseStructure<String> structure = new ResponseStructure<String>();

		List<String> url = postOnServer.uploadFile(file, "posts/");
		String fileUrl=url.get(0);
		MultipartFile mediaFile=file[0];
		if (fileUrl == null) {
			structure.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
			structure.setMessage("An error occured while uploading on server");
			structure.setStatus("error");
			structure.setData(null);
			structure.setPlatform(null);
			return new ResponseEntity<ResponseWrapper>(configuration.getResponseWrapper(structure),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
		String instaId = instagramUser.getInstaUserId();
		String profileName = instagramUser.getInstaUsername();
		if (mediaPost.getCaption() == null)
			mediaPost.setCaption(" ");
		if (mediaFile.getContentType().startsWith("image")) {
			return postImageToMedia(instaId, fileUrl, mediaPost.getCaption(), accessToken, userId, profileName);
		} else if (mediaFile.getContentType().startsWith("video")) {
			return postVideoToMedia(instaId, fileUrl, mediaPost.getCaption(), accessToken, userId, profileName,
					mediaFile);
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
			String accessToken, int userId, String profileName, MultipartFile mediaFile) {
		try {
			FacebookClient client = configuration.getFacebookClient(accessToken);
			GraphResponse container = client.publish(instagramUserId + "/media", GraphResponse.class,
					Parameter.with("video_url", fileUrl), Parameter.with("caption", caption),
					Parameter.with("media_type", "REELS"));
			String containerId = container.getId();
			if (containerId != null) {
				String status = checkMediaStatus(containerId, accessToken);
				while (!status.equals("FINISHED")) {
					System.out.println(status);
					if (status.equals("ERROR")) {
						ErrorResponse errorResponse = new ErrorResponse();
						errorResponse.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
						errorResponse.setMessage("Request Failed");
						errorResponse.setStatus("error");
						errorResponse.setData(null);
						errorResponse.setPlatform("instagram");
						return new ResponseEntity<ResponseWrapper>(configuration.getResponseWrapper(errorResponse),
								HttpStatus.BAD_REQUEST);
//						String fileurl=uploadFileToServer.uploadFile(mediaFile);
//						return postVideoToMedia(instagramUserId,fileurl,caption,accessToken,userId,profileName,mediaFile);
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
					QuantumShareUser user = userDao.fetchUser(userId);
					CreditSystem credits = user.getCreditSystem();
					credits.setRemainingCredit(credits.getRemainingCredit() - 1);
					user.setCreditSystem(credits);
					userDao.save(user);
					analyticsPostService.savePost(response.getId(), instagramUserId, user, "video", "instagram",
							profileName);
					SuccessResponse successResponse = new SuccessResponse();
					successResponse.setCode(HttpStatus.OK.value());
					successResponse.setMessage("Posted On Instagram");
					successResponse.setStatus("success");
					successResponse.setData(response);
					successResponse.setPlatform("instagram");
					successResponse.setRemainingCredits(credits.getRemainingCredit());
					return new ResponseEntity<ResponseWrapper>(configuration.getResponseWrapper(successResponse),
							HttpStatus.OK);
				} else {
					ErrorResponse errorResponse = new ErrorResponse();
					errorResponse.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
					errorResponse.setMessage("Request Failed");
					errorResponse.setStatus("error");
					errorResponse.setData(response);
					errorResponse.setPlatform("instgram");
					return new ResponseEntity<ResponseWrapper>(configuration.getResponseWrapper(errorResponse),
							HttpStatus.INTERNAL_SERVER_ERROR);
				}

			} else {
				ResponseStructure<String> structure = new ResponseStructure<String>();
				structure.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
				structure.setMessage("Request Failed");
				structure.setStatus("error");
				structure.setData(null);
				structure.setPlatform("instagram");
				return new ResponseEntity<ResponseWrapper>(configuration.getResponseWrapper(structure),
						HttpStatus.INTERNAL_SERVER_ERROR);

			}
		} catch (FacebookException e) {
			if (e.getMessage().contains("Error validating access token: Session has expired")) {
//				mediaLogoutService.disconnectInstagram(user);
				ResponseStructure<String> structure = new ResponseStructure<String>();
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

	private ResponseEntity<ResponseWrapper> postImageToMedia(String instagramUserId, String fileUrl, String caption,
			String accessToken, int userId, String profileName) {
		try {
			FacebookClient client = configuration.getFacebookClient(accessToken);
			GraphResponse container = client.publish(instagramUserId + "/media", GraphResponse.class,
					Parameter.with("image_url", fileUrl), Parameter.with("caption", caption),
					Parameter.with("media_type", "IMAGE"));
			String containerId = container.getId();
			GraphResponse response = client.publish(instagramUserId + "/media_publish", GraphResponse.class,
					Parameter.with("creation_id", containerId));
			if (response.isSuccess()) {
				QuantumShareUser user = userDao.fetchUser(userId);
				CreditSystem credits = user.getCreditSystem();
				credits.setRemainingCredit(credits.getRemainingCredit() - 1);
				user.setCreditSystem(credits);
				userDao.save(user);
				analyticsPostService.savePost(response.getId(), instagramUserId, user, "image", "instagram",
						profileName);
				SuccessResponse successResponse = new SuccessResponse();
				successResponse.setCode(HttpStatus.OK.value());
				successResponse.setMessage("Posted On Instagram");
				successResponse.setStatus("success");
				successResponse.setData(response);
				successResponse.setPlatform("instagram");
				successResponse.setRemainingCredits(credits.getRemainingCredit());
				return new ResponseEntity<ResponseWrapper>(configuration.getResponseWrapper(successResponse),
						HttpStatus.OK);
			} else {
				ErrorResponse errorResponse = new ErrorResponse();
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
				ResponseStructure<String> structure = new ResponseStructure<String>();
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

	public ResponseEntity<ResponseStructure<String>> verifyToken(String access_token, QuantumShareUser user,
			int userId) {
		List<String> instaIds = fetchID(access_token);
		return fetchInstaProfiles(instaIds, access_token);
//		return saveInstaUser(instaId, username, profile, access_token, user, instaUser, userId);
	}

	private ResponseEntity<ResponseStructure<String>> fetchInstaProfiles(List<String> instaIds, String access_token) {
		ResponseStructure<String> structure = new ResponseStructure<String>();
		List<InstagramUser> list = new ArrayList<InstagramUser>();
		try {
			if (instaIds.isEmpty()) {
				structure.setCode(HttpStatus.BAD_REQUEST.value());
				structure.setData(null);
				structure.setMessage("No instagrams are associated with facebook pages");
				structure.setPlatform(null);
				structure.setStatus("error");
				return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.BAD_REQUEST);
			}
			for (String instaId : instaIds) {
				JsonNode instaUser = fetchUsername(instaId, access_token);
				String username = instaUser.get("username").asText();
				ResponseEntity<String> profile = fetchProfile(instaId, username, access_token);
				InstagramUser instagramUser = new InstagramUser();
				instagramUser.setInstaUserId(instaId);
				instagramUser.setInstaUsername(username.replace("\"", ""));
				instagramUser.setFollwersCount(
						instaUser.get("followers_count") != null && !instaUser.get("followers_count").isNull()
								? instaUser.get("followers_count").asInt()
								: 0 // Default value
				);
				JsonNode jsonResponse = objectMapper.readTree(profile.getBody());
				instagramUser.setPictureUrl(
						jsonResponse.has("profile_picture_url") ? jsonResponse.get("profile_picture_url").asText()
								: null);
				instagramUser.setInstUserAccessToken(access_token);
				list.add(instagramUser);
			}
			structure.setCode(HttpStatus.OK.value());
			structure.setData(list);
			structure.setMessage(null);
			structure.setPlatform(null);
			structure.setStatus("success");
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			throw new CommonException(e.getMessage());
		}
	}

	public ResponseEntity<ResponseStructure<String>> saveInstaUser(InstagramUser instaUser, QuantumShareUser user) {
		try {
			if (user.getSocialAccounts() == null) {
				SocialAccounts socialAccounts = new SocialAccounts();
				socialAccounts.setInstagramUser(instaUser);
				user.setSocialAccounts(socialAccounts);
				userDao.save(user);
			} else if (user.getSocialAccounts().getInstagramUser() == null) {
				SocialAccounts accounts = user.getSocialAccounts();
				accounts.setInstagramUser(instaUser);
				user.setSocialAccounts(accounts);
				userDao.save(user);
			} else {
				SocialAccounts accounts = user.getSocialAccounts();
				InstagramUser exInstaUser = user.getSocialAccounts().getInstagramUser();
				exInstaUser.setInstaUserId(instaUser.getInstaUserId());
				exInstaUser.setFollwersCount(instaUser.getFollwersCount());
				exInstaUser.setInstaUsername(instaUser.getInstaUsername());
				exInstaUser.setInstUserAccessToken(instaUser.getInstUserAccessToken());
				exInstaUser.setPictureUrl(instaUser.getPictureUrl());
				accounts.setInstagramUser(exInstaUser);
				user.setSocialAccounts(accounts);
				userDao.save(user);
			}

			ResponseStructure<String> structure = new ResponseStructure<String>();
			structure.setCode(HttpStatus.CREATED.value());
			structure.setMessage("Instagram Connected Successfully");
			structure.setStatus("success");
			structure.setPlatform("instagram");
			Map<String, Object> data = configuration.getMap();
			data.clear();
			InstagramUser datauser = user.getSocialAccounts().getInstagramUser();
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

	private List<String> fetchID(String access_token) {
		try {
			List<String> list = new ArrayList<String>();
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
						list.add(instagramIdNode.asText());
					}
				}
			}
			return list;
		} catch (JsonMappingException e) {
			throw new CommonException(e.getMessage());
		} catch (JsonProcessingException e) {
			throw new CommonException(e.getMessage());
		} catch (BadRequest e) {
			throw new BadRequestException(e.getMessage());
		}

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
