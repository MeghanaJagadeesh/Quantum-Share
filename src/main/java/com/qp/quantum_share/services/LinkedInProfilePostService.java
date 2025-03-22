package com.qp.quantum_share.services;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.qp.quantum_share.configuration.ConfigurationClass;
import com.qp.quantum_share.dao.QuantumShareUserDao;
import com.qp.quantum_share.dto.CreditSystem;
import com.qp.quantum_share.dto.LinkedInPageDto;
import com.qp.quantum_share.dto.LinkedInProfileDto;
import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.exception.CommonException;
import com.qp.quantum_share.response.ResponseStructure;

@Service
public class LinkedInProfilePostService {

	@Autowired
	RestTemplate restTemplate;

	@Autowired
	HttpHeaders headers;

	@Autowired
	ConfigurationClass config;

	@Autowired
	QuantumShareUserDao userDao;

	@Autowired
	AnalyticsPostService analyticsPostService;

	public ResponseStructure<String> uploadImageToLinkedIn(MultipartFile mediaFile, String caption,
			LinkedInProfileDto linkedInProfileUser, int userId) {
		ResponseStructure<String> response = new ResponseStructure<String>();
		String profileURN = linkedInProfileUser.getLinkedinProfileURN();
		String accessToken = linkedInProfileUser.getLinkedinProfileAccessToken();
		try {
			String recipeType = determineRecipeType(mediaFile);
			String mediaType = determineMediaType(mediaFile);
			// step 1
			JsonNode uploadResponse = registerUpload(recipeType, accessToken, profileURN);

			String uploadUrl = uploadResponse.get("value").get("uploadMechanism")
					.get("com.linkedin.digitalmedia.uploading.MediaUploadHttpRequest").get("uploadUrl").asText();
			String mediaAsset = uploadResponse.get("value").get("asset").asText();
			// step 2
			if (uploadImage(uploadUrl, mediaFile, accessToken, userId)) {
				// step 3
				return createLinkedInPost(mediaAsset, caption, mediaType, accessToken, profileURN, userId,
						linkedInProfileUser.getLinkedinProfileUserName(), mediaFile.getSize());
			} else {
				response.setStatus("error");
				response.setMessage("Internal server eooro");
				response.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
				response.setPlatform("linkedin");
				response.setData(null);
			}
		} catch (HttpClientErrorException.TooManyRequests e) {
			response.setStatus("Failure");
			response.setMessage("Failed to create LinkedIn post: Too Many Requests - " + e.getMessage());
			response.setCode(HttpStatus.TOO_MANY_REQUESTS.value());
			response.setPlatform("linkedin");
			response.setData(null);
		} catch (HttpClientErrorException e) {
			response.setStatus("Failure");
			response.setMessage(
					"Failed to create LinkedIn post: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
			response.setCode(e.getStatusCode().value());
			response.setPlatform("linkedin");
			response.setData(null);
		} catch (IOException e) {
			response.setStatus("Failure");
			response.setMessage("Failed to upload media to LinkedIn: " + e.getMessage());
			response.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
			response.setPlatform("linkedin");
			response.setData(null);
		}
		return response;
	}

	// step - 1
	private JsonNode registerUpload(String recipeType, String accessToken, String profileURN) throws IOException {
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set("Authorization", "Bearer " + accessToken);
			String requestBody = "{\"registerUploadRequest\": {\"recipes\": [\"" + recipeType
					+ "\"],\"owner\": \"urn:li:person:" + profileURN
					+ "\",\"serviceRelationships\": [{\"relationshipType\": \"OWNER\",\"identifier\": \"urn:li:userGeneratedContent\"}]}}";
			HttpEntity<String> requestEntity = config.getHttpEntity(requestBody, headers);
			ResponseEntity<JsonNode> responseEntity = restTemplate.exchange(
					"https://api.linkedin.com/v2/assets?action=registerUpload", HttpMethod.POST, requestEntity,
					JsonNode.class);
			if (responseEntity.getStatusCode() == HttpStatus.OK) {
				return responseEntity.getBody();
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new CommonException(e.getMessage());
		}
		return null;
	}

	// step 2
	private boolean uploadImage(String uploadUrl, MultipartFile file, String accessToken, int userId) {
		ResponseStructure<String> response = new ResponseStructure<String>();
		try {
			headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
			headers.setBearerAuth(accessToken);
			byte[] fileContent = file.getBytes();

			HttpEntity<byte[]> requestEntity = config.getByteHttpEntity(fileContent, headers);
			ResponseEntity<String> responseEntity = restTemplate.exchange(uploadUrl, HttpMethod.POST, requestEntity,
					String.class);

			if (responseEntity.getStatusCode() == HttpStatus.CREATED) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			response.setData(null);
			response.setPlatform("linkedin");
			response.setStatus("Failure");
			response.setMessage("Internal Server Error");
			response.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}
		return false;

	}

	// step 3
	private ResponseStructure<String> createLinkedInPost(String mediaAsset, String caption, String mediaType,
			String accessToken, String profileURN, int userId, String profileName, long size) {
		ResponseStructure<String> response = new ResponseStructure<String>();

		try {
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set("Authorization", "Bearer " + accessToken);

			String shareMediaCategory = mediaType.equals("image") ? "IMAGE" : "VIDEO";

			String requestBody = "{\n" + "    \"author\": \"urn:li:person:" + profileURN + "\",\n"
					+ "    \"lifecycleState\": \"PUBLISHED\",\n" + "    \"specificContent\": {\n"
					+ "        \"com.linkedin.ugc.ShareContent\": {\n" + "            \"shareCommentary\": {\n"
					+ "                \"text\": \"" + caption + "\"\n" + "            },\n"
					+ "            \"shareMediaCategory\": \"" + shareMediaCategory + "\",\n"
					+ "            \"media\": [\n" + "                {\n"
					+ "                    \"status\": \"READY\",\n" + "                    \"description\": {\n"
					+ "                        \"text\": \"Center stage!\"\n" + "                    },\n"
					+ "                    \"media\": \"" + mediaAsset + "\",\n" + "                    \"title\": {\n"
					+ "                        \"text\": \"LinkedIn Talent Connect 2021\"\n" + "                    }\n"
					+ "                }\n" + "            ]\n" + "        }\n" + "    },\n" + "    \"visibility\": {\n"
					+ "        \"com.linkedin.ugc.MemberNetworkVisibility\": \"PUBLIC\"\n" + "    }\n" + "}";

			HttpEntity<String> requestEntity = config.getHttpEntity(requestBody, headers);
			ResponseEntity<JsonNode> responseEntity = restTemplate.exchange("https://api.linkedin.com/v2/ugcPosts",
					HttpMethod.POST, requestEntity, JsonNode.class);

			if (responseEntity.getStatusCode() == HttpStatus.CREATED) {
				System.out.println("response : " + responseEntity.getBody()+" "+mediaType);
				QuantumShareUser user = userDao.fetchUser(userId);
				CreditSystem credits = user.getCreditSystem();
				credits.setRemainingCredit(credits.getRemainingCredit() - 1);
				user.setCreditSystem(credits);
				userDao.save(user);
				saveLinkedInPost(responseEntity.getBody().get("id").asText(), profileURN, user,
						mediaType.equals("IMAGE") ? "image" : "video", profileName, accessToken, size);

				response.setStatus("Success");
				response.setPlatform("linkedin");
				response.setMessage("Posted To LinkedIn Profile");
				response.setCode(HttpStatus.CREATED.value());
				response.setData(responseEntity.getBody());
			} else {
				response.setData(null);
				response.setPlatform("linkedin");
				response.setStatus("Failure");
				response.setMessage("Failed to create LinkedIn post: " + responseEntity.getStatusCode());
				response.setCode(responseEntity.getStatusCode().value());
			}
		} catch (Exception e) {
			throw new CommonException(e.getMessage());
		}
		return response;
	}

	public ResponseStructure<String> uploadImageToLinkedInPage(MultipartFile file, String caption,
			LinkedInPageDto linkedInPageUser, int userId) {
		String pageURN = "urn:li:organization:" + linkedInPageUser.getLinkedinPageURN();
		String accessToken = linkedInPageUser.getLinkedinPageAccessToken();
		ResponseStructure<String> response = new ResponseStructure<String>();

		try {
			String recipeType = determineRecipeTypePage(file);
			String mediaType = determineMediaTypePage(file);
			JsonNode uploadResponse = registerUploadPage(recipeType, pageURN, accessToken);
			String uploadUrl = uploadResponse.get("value").get("uploadMechanism")
					.get("com.linkedin.digitalmedia.uploading.MediaUploadHttpRequest").get("uploadUrl").asText();
			String mediaAsset = uploadResponse.get("value").get("asset").asText();
			if (uploadImagePage(uploadUrl, file, accessToken, userId)) {
				return createLinkedInPostPage(mediaAsset, caption, mediaType, pageURN, accessToken, userId,
						linkedInPageUser.getLinkedinPageName(), file.getSize());
			} else {
				response.setStatus("error");
				response.setMessage("Failed to upload media to LinkedIn");
				response.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
				response.setData(null);
				response.setPlatform(null);
			}

		} catch (IOException e) {
			response.setStatus("Failure");
			response.setMessage("Failed to upload media to LinkedIn: " + e.getMessage());
			response.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}
		return response;
	}

	private JsonNode registerUploadPage(String recipeType, String pageURN, String accessToken) throws IOException {
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(accessToken);
		String requestBody = "{\"registerUploadRequest\": {\"recipes\": [\"" + recipeType + "\"],\"owner\": \""
				+ pageURN
				+ "\",\"serviceRelationships\": [{\"relationshipType\": \"OWNER\",\"identifier\": \"urn:li:userGeneratedContent\"}]}}";

		HttpEntity<String> requestEntity = config.getHttpEntity(requestBody, headers);

		ResponseEntity<JsonNode> responseEntity = restTemplate.exchange(
				"https://api.linkedin.com/v2/assets?action=registerUpload", HttpMethod.POST, requestEntity,
				JsonNode.class);

		if (responseEntity.getStatusCode() == HttpStatus.OK) {
			return responseEntity.getBody();
		} else {
			throw new RuntimeException("Failed to register upload: " + responseEntity.getStatusCode());
		}
	}

	private boolean uploadImagePage(String uploadUrl, MultipartFile file, String accessToken, int userId) {
		try {
			headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
			headers.set("Authorization", "Bearer " + accessToken);

			byte[] fileContent = file.getBytes();
			HttpEntity<byte[]> requestEntity = config.getByteHttpEntity(fileContent, headers);
			ResponseEntity<String> responseEntity = restTemplate.exchange(uploadUrl, HttpMethod.POST, requestEntity,
					String.class);

			if (responseEntity.getStatusCode() == HttpStatus.CREATED) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			throw new CommonException(e.getMessage());
		}
	}

	private String determineRecipeTypePage(MultipartFile file) {
		String contentType = file.getContentType();
		return contentType != null && contentType.startsWith("image") ? "urn:li:digitalmediaRecipe:feedshare-image"
				: "urn:li:digitalmediaRecipe:feedshare-video";
	}

	private String determineMediaTypePage(MultipartFile file) {
		return file.getContentType() != null && file.getContentType().startsWith("image") ? "image" : "video";
	}

	public ResponseStructure<String> createLinkedInPostPage(String mediaAsset, String caption, String mediaType,
			String pageURN, String accessToken, int userId, String pageName, long size) {
		ResponseStructure<String> response = new ResponseStructure<String>();
		try {
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set("Authorization", "Bearer " + accessToken);
			String shareMediaCategory = mediaType.equals("image") ? "IMAGE" : "VIDEO";
			String requestBody = "{\n" + "    \"author\": \"" + pageURN + "\",\n"
					+ "    \"lifecycleState\": \"PUBLISHED\",\n" + "    \"specificContent\": {\n"
					+ "        \"com.linkedin.ugc.ShareContent\": {\n" + "            \"shareCommentary\": {\n"
					+ "                \"text\": \"" + caption + "\"\n" + "            },\n"
					+ "            \"shareMediaCategory\": \"" + shareMediaCategory + "\",\n"
					+ "            \"media\": [\n" + "                {\n"
					+ "                    \"status\": \"READY\",\n" + "                    \"description\": {\n"
					+ "                        \"text\": \"Center stage!\"\n" + "                    },\n"
					+ "                    \"media\": \"" + mediaAsset + "\",\n" + "                    \"title\": {\n"
					+ "                        \"text\": \"LinkedIn Talent Connect 2021\"\n" + "                    }\n"
					+ "                }\n" + "            ]\n" + "        }\n" + "    },\n" + "    \"visibility\": {\n"
					+ "        \"com.linkedin.ugc.MemberNetworkVisibility\": \"PUBLIC\"\n" + "    }\n" + "}";

			HttpEntity<String> requestEntity = config.getHttpEntity(requestBody, headers);

			ResponseEntity<JsonNode> responseEntity = restTemplate.exchange("https://api.linkedin.com/v2/ugcPosts",
					HttpMethod.POST, requestEntity, JsonNode.class);

			if (responseEntity.getStatusCode() == HttpStatus.CREATED) {
				QuantumShareUser user = userDao.fetchUser(userId);
				CreditSystem credits = user.getCreditSystem();
				credits.setRemainingCredit(credits.getRemainingCredit() - 1);
				user.setCreditSystem(credits);
				userDao.save(user);
				saveLinkedInPost(responseEntity.getBody().get("id").asText(), pageURN, user,
						shareMediaCategory.equals("IMAGE") ? "image" : "video", pageName, accessToken, size);

				response.setStatus("success");
				response.setMessage("Posted To LinkedIn Page");
				response.setCode(HttpStatus.CREATED.value());
				response.setData(responseEntity.getBody());
			} else {
				response.setStatus("error");
				response.setMessage("Failed to create LinkedIn post: " + responseEntity.getStatusCode());
				response.setCode(responseEntity.getStatusCode().value());
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new CommonException(e.getMessage());
		}
		return response;
	}

	private void saveLinkedInPost(String postId, String pageURN, QuantumShareUser user, String type, String pageName,
			String accessToken, long size) {
		if (type.equals("video")) {
			try {
				if (size < 20 * 1024 * 1024) {
					Thread.sleep(9000);
				} else if (size > 20 * 1024 * 1024) {
					Thread.sleep(15000);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setBearerAuth(accessToken);
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			headers.set("LinkedIn-Version", "202411");
			String url = "https://api.linkedin.com/rest/posts/";
			HttpEntity<String> requestEntity = config.getHttpEntity(headers);
			ResponseEntity<JsonNode> response = restTemplate.exchange(url + postId, HttpMethod.GET, requestEntity,
					JsonNode.class);
			JsonNode body = response.getBody();
			String mediaId = null;
			if (response.getStatusCode().is2xxSuccessful()) {
				mediaId = body.path("content").path("media").path("id").asText();
			}
			String postUrl = null;
			String apiUrl = "https://api.linkedin.com/rest/";
			if (mediaId.startsWith("urn:li:image")) {
				apiUrl = apiUrl + "images/" + mediaId;

			} else if (mediaId.startsWith("urn:li:video")) {
				apiUrl = apiUrl + "videos/" + mediaId;
			}
			ResponseEntity<JsonNode> response1 = restTemplate.exchange(apiUrl, HttpMethod.GET, requestEntity,
					JsonNode.class);
			JsonNode body2 = response1.getBody();
			if (response.getStatusCode().is2xxSuccessful()) {
				postUrl = body2.get("downloadUrl").asText();
			}

			analyticsPostService.savePost(postId, pageURN, user, type, "linkedin", pageName, postUrl);
		} catch (Exception e) {
			e.printStackTrace();
			throw new CommonException(e.getMessage());
		}

	}

	private String determineRecipeType(MultipartFile file) {
		String contentType = file.getContentType();
		return contentType != null && contentType.startsWith("image") ? "urn:li:digitalmediaRecipe:feedshare-image"
				: "urn:li:digitalmediaRecipe:feedshare-video";
	}

	private String determineMediaType(MultipartFile file) {
		return file.getContentType() != null && file.getContentType().startsWith("image") ? "image" : "video";
	}

	// post only message to page
	public ResponseStructure<String> createPostPage(String caption, LinkedInPageDto linkedInPageUser, int userId) {
		String pageURN = linkedInPageUser.getLinkedinPageURN();
		String accessToken = linkedInPageUser.getLinkedinPageAccessToken();
		ResponseStructure<String> response = new ResponseStructure<String>();

		try {
			String url = "https://api.linkedin.com/v2/ugcPosts";
			String requestBody = "{\"author\":\"urn:li:organization:" + pageURN
					+ "\",\"lifecycleState\":\"PUBLISHED\",\"specificContent\":{\"com.linkedin.ugc.ShareContent\":{\"shareCommentary\":{\"text\":\""
					+ caption
					+ "\"},\"shareMediaCategory\":\"NONE\"}},\"visibility\":{\"com.linkedin.ugc.MemberNetworkVisibility\":\"PUBLIC\"}}";

			headers.set("Authorization", "Bearer " + accessToken);
			headers.set("Content-Type", "application/json");
			HttpEntity<String> entity = config.getHttpEntity(requestBody, headers);

			ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
			if (responseEntity.getStatusCode() == HttpStatus.CREATED) {
				QuantumShareUser user = userDao.fetchUser(userId);
				CreditSystem credits = user.getCreditSystem();
				credits.setRemainingCredit(credits.getRemainingCredit() - 1);
				user.setCreditSystem(credits);
				userDao.save(user);
				response.setStatus("Success");
				response.setMessage("Posted To LinkedIn Page");
				response.setCode(HttpStatus.CREATED.value());
				response.setData(responseEntity.getBody());
			} else {
				response.setStatus("Failure");
				response.setMessage("Failed to create post");
				response.setCode(responseEntity.getStatusCode().value());
				response.setData(responseEntity.getBody());
			}
		} catch (HttpClientErrorException e) {
			response.setStatus("Failure");
			response.setMessage("HTTP Client Error: " + e.getStatusCode());
			response.setCode(e.getStatusCode().value());
		} catch (HttpServerErrorException e) {
			response.setStatus("Failure");
			response.setMessage("HTTP Server Error: " + e.getStatusCode());
			response.setCode(e.getStatusCode().value());
			e.printStackTrace();
		} catch (Exception e) {
			response.setStatus("Failure");
			response.setMessage("Internal Server Error: " + e.getMessage());
			response.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
			e.printStackTrace();
		}
		return response;
	}

	// LinkedIn Caption Posting to profile
	public ResponseStructure<String> createPostProfile(String caption, LinkedInProfileDto linkedInProfileUser,
			int userId) {
		String profileURN = linkedInProfileUser.getLinkedinProfileURN();
		String accessToken = linkedInProfileUser.getLinkedinProfileAccessToken();
		ResponseStructure<String> response = new ResponseStructure<String>();
		try {
			String url = "https://api.linkedin.com/v2/ugcPosts";
			String requestBody = "{\"author\":\"urn:li:person:" + profileURN
					+ "\",\"lifecycleState\":\"PUBLISHED\",\"specificContent\":{\"com.linkedin.ugc.ShareContent\":{\"shareCommentary\":{\"text\":\""
					+ caption
					+ "\"},\"shareMediaCategory\":\"NONE\"}},\"visibility\":{\"com.linkedin.ugc.MemberNetworkVisibility\":\"PUBLIC\"}}";

			headers.setBearerAuth(accessToken);
			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<String> entity = config.getHttpEntity(requestBody, headers);

			ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
			if (responseEntity.getStatusCode() == HttpStatus.CREATED) {
				QuantumShareUser user = userDao.fetchUser(userId);
				CreditSystem credits = user.getCreditSystem();
				credits.setRemainingCredit(credits.getRemainingCredit() - 1);
				user.setCreditSystem(credits);
				userDao.save(user);
				response.setStatus("Success");
				response.setMessage("Posted To LinkedIn Profile");
				response.setCode(HttpStatus.CREATED.value());
				response.setData(responseEntity.getBody());
				response.setPlatform("linkedin");
			} else {
				response.setStatus("error");
				response.setMessage("Internal server error");
				response.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
				response.setData(responseEntity.getBody());
				response.setPlatform("linkedin");
			}
		} catch (HttpClientErrorException e) {
			throw new CommonException(e.getMessage());
		} catch (HttpServerErrorException e) {
			throw new CommonException(e.getMessage());
		} catch (Exception e) {
			throw new CommonException(e.getMessage());
		}
		return response;
	}

}