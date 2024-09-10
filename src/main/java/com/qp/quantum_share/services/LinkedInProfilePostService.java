package com.qp.quantum_share.services;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.qp.quantum_share.configuration.ConfigurationClass;
import com.qp.quantum_share.dto.LinkedInPageDto;
import com.qp.quantum_share.dto.LinkedInProfileDto;
import com.qp.quantum_share.exception.CommonException;
import com.qp.quantum_share.response.ResponseStructure;

@Service
public class LinkedInProfilePostService {
	@Autowired
	ResponseStructure<String> response;

	@Autowired
	RestTemplate restTemplate;

	@Autowired
	HttpHeaders headers;

	@Autowired
	ConfigurationClass config;

	// LinkedIn Caption Posting
	public ResponseStructure<String> createPostProfile(String caption, LinkedInProfileDto linkedInProfileUser) {
		String profileURN = linkedInProfileUser.getLinkedinProfileURN();
		String accessToken = linkedInProfileUser.getLinkedinProfileAccessToken();

		try {
			System.out.println("Caption: " + caption);
			String url = "https://api.linkedin.com/v2/ugcPosts";
			String requestBody = "{\"author\":\"urn:li:person:" + profileURN
					+ "\",\"lifecycleState\":\"PUBLISHED\",\"specificContent\":{\"com.linkedin.ugc.ShareContent\":{\"shareCommentary\":{\"text\":\""
					+ caption
					+ "\"},\"shareMediaCategory\":\"NONE\"}},\"visibility\":{\"com.linkedin.ugc.MemberNetworkVisibility\":\"PUBLIC\"}}";

			headers.set("Authorization", "Bearer " + accessToken);
			headers.set("Content-Type", "application/json");
			HttpEntity<String> entity = config.getHttpEntity(requestBody, headers);

			ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

			if (responseEntity.getStatusCode() == HttpStatus.CREATED) {
				response.setStatus("Success");
				response.setMessage("Posted To LinkedIn Profile");
				response.setCode(HttpStatus.CREATED.value());
				response.setData(responseEntity.getBody());
			} else {
				handleFailureResponse(response, responseEntity.getStatusCode(), responseEntity.getBody());
			}
		} catch (HttpClientErrorException e) {
			handleClientErrorResponse(response, e);
		} catch (HttpServerErrorException e) {
			handleServerErrorResponse(response, e);
		} catch (Exception e) {
			throw new CommonException(e.getMessage());
		}
		return response;
	}

	private void handleFailureResponse(ResponseStructure<String> response, HttpStatusCode httpStatusCode,
			String responseBody) {
		if (httpStatusCode == org.springframework.http.HttpStatus.BAD_REQUEST) {
			response.setStatus("Failure");
			response.setMessage("Failed to create LinkedIn post: Caption is invalid");
			response.setCode(HttpStatus.BAD_REQUEST.value());
			response.setData(null);
		} else if (httpStatusCode == HttpStatus.UNAUTHORIZED) {
			response.setStatus("Failure");
			response.setMessage("Failed to create LinkedIn post: Unauthorized access");
			response.setCode(HttpStatus.UNAUTHORIZED.value());
			response.setData(null);
		} else if (httpStatusCode == HttpStatus.UNPROCESSABLE_ENTITY) {
			response.setStatus("Failure");
			response.setMessage("Failed to create LinkedIn post: Media asset error");
			response.setCode(HttpStatus.UNPROCESSABLE_ENTITY.value());
			response.setData(null);
		} else if (httpStatusCode == HttpStatus.TOO_MANY_REQUESTS) {
			response.setStatus("Failure");
			response.setMessage("Failed to create LinkedIn post: Too Many Requests");
			response.setCode(HttpStatus.TOO_MANY_REQUESTS.value());
			response.setData(null);
		} else if (httpStatusCode == HttpStatus.INTERNAL_SERVER_ERROR) {
			response.setStatus("Failure");
			response.setMessage("Failed to create LinkedIn post: Internal server error");
			response.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
			response.setData(null);
		} else if (httpStatusCode == HttpStatus.SERVICE_UNAVAILABLE) {
			response.setStatus("Failure");
			response.setMessage("Failed to create LinkedIn post: Network issues");
			response.setCode(HttpStatus.SERVICE_UNAVAILABLE.value());
			response.setData(null);
		} else {
			response.setStatus("Failure");
			response.setMessage("Failed to create LinkedIn post: Unexpected error occurred");
			response.setCode(httpStatusCode.value());
			response.setData(null);
		}
	}

	private void handleClientErrorResponse(ResponseStructure<String> response, HttpClientErrorException e) {
		if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
			response.setStatus("Failure");
			response.setMessage("Failed to create LinkedIn post: Too Many Requests - " + e.getMessage());
			response.setCode(HttpStatus.TOO_MANY_REQUESTS.value());
			response.setData(null);
		} else {
			response.setStatus("Failure");
			response.setMessage(
					"Failed to create LinkedIn post: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
			response.setCode(e.getStatusCode().value());
			response.setData(null);
		}
	}

	private void handleServerErrorResponse(ResponseStructure<String> response, HttpServerErrorException e) {
		response.setStatus("Failure");
		response.setMessage("HTTP Server Error: " + e.getStatusCode());
		response.setCode(e.getStatusCode().value());
		response.setData(null);
	}

	public ResponseStructure<String> uploadImageToLinkedIn(MultipartFile mediaFile, String caption,
			LinkedInProfileDto linkedInProfileUser) {
		String profileURN = linkedInProfileUser.getLinkedinProfileURN();
		String accessToken = linkedInProfileUser.getLinkedinProfileAccessToken();
		try {
			System.out.println("controller is here 1 " + caption + " " + mediaFile);

			String recipeType = determineRecipeType(mediaFile);
			String mediaType = determineMediaType(mediaFile);
			JsonNode uploadResponse = registerUpload(recipeType, accessToken, profileURN);
			String uploadUrl = uploadResponse.get("value").get("uploadMechanism")
					.get("com.linkedin.digitalmedia.uploading.MediaUploadHttpRequest").get("uploadUrl").asText();
			String mediaAsset = uploadResponse.get("value").get("asset").asText();
			uploadImage(uploadUrl, mediaFile, accessToken);
			ResponseStructure<String> postResponse = createLinkedInPost(mediaAsset, caption, mediaType, accessToken,
					profileURN);
			System.out.println(postResponse);

			handlePostResponse(response, postResponse);

		} catch (HttpClientErrorException.TooManyRequests e) {
			response.setStatus("Failure");
			response.setMessage("Failed to create LinkedIn post: Too Many Requests - " + e.getMessage());
			response.setCode(HttpStatus.TOO_MANY_REQUESTS.value());
			response.setData(null);
		} catch (HttpClientErrorException e) {
			response.setStatus("Failure");
			response.setMessage(
					"Failed to create LinkedIn post: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
			response.setCode(e.getStatusCode().value());
			response.setData(null);
		} catch (IOException e) {
			response.setStatus("Failure");
			response.setMessage("Failed to upload media to LinkedIn: " + e.getMessage());
			response.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
			response.setData(null);
		}
		return response;
	}

	private void handlePostResponse(ResponseStructure<String> response, ResponseStructure<String> postResponse) {
		if (postResponse.getCode() == 201) {
			response.setStatus(postResponse.getStatus());
			response.setMessage(postResponse.getMessage());
			response.setCode(postResponse.getCode());
			response.setData(postResponse.getData());
		} else if (postResponse.getCode() == 400) {
			response.setStatus("Failure");
			response.setMessage("Failed to create LinkedIn post: Caption is invalid");
			response.setCode(400);
			response.setData(null);
		} else if (postResponse.getCode() == 401) {
			response.setStatus("Failure");
			response.setMessage("Failed to create LinkedIn post: Unauthorized access");
			response.setCode(401);
			response.setData(null);
		} else if (postResponse.getCode() == 422) {
			response.setStatus("Failure");
			response.setMessage("Failed to create LinkedIn post: Media asset error");
			response.setCode(422);
			response.setData(null);
		} else if (postResponse.getCode() == 429) {
			response.setStatus("Failure");
			response.setMessage("Failed to create LinkedIn post: Too Many Requests");
			response.setCode(429);
			response.setData(null);
		} else if (postResponse.getCode() == 500) {
			response.setStatus("Failure");
			response.setMessage("Failed to create LinkedIn post: Internal server error");
			response.setCode(500);
			response.setData(null);
		} else if (postResponse.getCode() == 503) {
			response.setStatus("Failure");
			response.setMessage("Failed to create LinkedIn post: Network issues");
			response.setCode(503);
			response.setData(null);
		} else {
			// Handle other failure scenarios
			response.setStatus("Failure");
			response.setMessage("Failed to create LinkedIn post: Unexpected error occurred");
			response.setCode(postResponse.getCode());
			response.setData(null);
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

	private JsonNode registerUpload(String recipeType, String accessToken, String profileURN) throws IOException {
		System.out.println("controller is here 2 " + recipeType);

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
		} else {
			throw new RuntimeException("Failed to register upload: " + responseEntity.getStatusCode());
		}
	}

	private ResponseStructure<String> uploadImage(String uploadUrl, MultipartFile file, String accessToken) {
		try {
			headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
			headers.set("Authorization", "Bearer " + accessToken);
			byte[] fileContent;
			try {
				fileContent = file.getBytes();
			} catch (IOException e) {
				response.setStatus("Failure");
				response.setMessage("Failed to read image file");
				response.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
				return response;
			}

			HttpEntity<byte[]> requestEntity = config.getByteHttpEntity(fileContent, headers);
			ResponseEntity<String> responseEntity = restTemplate.exchange(uploadUrl, HttpMethod.POST, requestEntity,
					String.class);

			if (responseEntity.getStatusCode() == HttpStatus.CREATED) {
				response.setStatus("Success");
				response.setMessage("Media uploaded successfully");
				response.setCode(HttpStatus.CREATED.value());
			} else {
				response.setStatus("Failure");
				response.setMessage("Failed to upload media: " + responseEntity.getStatusCode());
				response.setCode(responseEntity.getStatusCode().value());
			}
		} catch (Exception e) {
			response.setStatus("Failure");
			response.setMessage("Internal Server Error");
			response.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}
		return response;
	}

	private ResponseStructure<String> createLinkedInPost(String mediaAsset, String caption, String mediaType,
			String accessToken, String profileURN) {

		try {
			System.out.println("controller is here 4 " + caption + " " + mediaAsset);
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
			ResponseEntity<String> responseEntity = restTemplate.exchange("https://api.linkedin.com/v2/ugcPosts",
					HttpMethod.POST, requestEntity, String.class);

			if (responseEntity.getStatusCode() == HttpStatus.CREATED) {
				System.out.println("Image with caption created successfully !!");
				response.setStatus("Success");
				response.setMessage("Posted To LinkedIn Profile");
				response.setCode(HttpStatus.CREATED.value());
				response.setData(responseEntity.getBody());
			} else {
				response.setStatus("Failure");
				response.setMessage("Failed to create LinkedIn post: " + responseEntity.getStatusCode());
				response.setCode(responseEntity.getStatusCode().value());
			}
		} catch (Exception e) {
			throw new CommonException(e.getMessage());
		}
		return response;
	}

	public ResponseStructure<String> createPostPage(String caption, LinkedInProfileDto linkedInProfileUser) {
		LinkedInPageDto pageDetails = linkedInProfileUser.getPages().get(0);
		String pageURN = pageDetails.getLinkedinPageURN();
		String accessToken = pageDetails.getLinkedinPageAccessToken();

		try {
			System.out.println("Caption: " + caption);
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
				response.setStatus("Success");
				response.setMessage("Posted To LinkedIn Page");
				response.setCode(HttpStatus.CREATED.value());
				response.setData(responseEntity.getBody());
				System.out.println("Response Body: " + responseEntity.getBody());
			} else {
				response.setStatus("Failure");
				response.setMessage("Failed to create post");
				response.setCode(responseEntity.getStatusCode().value());
				response.setData(responseEntity.getBody());
				System.out.println("Error Response: " + responseEntity.getBody());
			}
		} catch (HttpClientErrorException e) {
			response.setStatus("Failure");
			response.setMessage("HTTP Client Error: " + e.getStatusCode());
			response.setCode(e.getStatusCode().value());
			System.out.println("HttpClientErrorException: " + e.getMessage());
		} catch (HttpServerErrorException e) {
			response.setStatus("Failure");
			response.setMessage("HTTP Server Error: " + e.getStatusCode());
			response.setCode(e.getStatusCode().value());
			System.out.println("HttpServerErrorException: " + e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			response.setStatus("Failure");
			response.setMessage("Internal Server Error: " + e.getMessage());
			response.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
			System.out.println("Exception: " + e.getMessage());
			e.printStackTrace();
		}
		return response;
	}

	// SHARE IMAGE/VIDEO AND TEXT TO LINKEDIN PAGE/ORGANIZATION
	public ResponseStructure<String> uploadImageToLinkedInPage(MultipartFile file, String caption,
			LinkedInProfileDto linkedInProfileUser) {
		LinkedInPageDto pageDetails = linkedInProfileUser.getPages().get(0);
		String pageURN = "urn:li:organization:" + pageDetails.getLinkedinPageURN();
		String accessToken = pageDetails.getLinkedinPageAccessToken();

		try {
			String recipeType = determineRecipeTypePage(file);
			String mediaType = determineMediaTypePage(file);
			JsonNode uploadResponse = registerUploadPage(recipeType, pageURN, accessToken);
			String uploadUrl = uploadResponse.get("value").get("uploadMechanism")
					.get("com.linkedin.digitalmedia.uploading.MediaUploadHttpRequest").get("uploadUrl").asText();
			String mediaAsset = uploadResponse.get("value").get("asset").asText();
			uploadImagePage(uploadUrl, file, accessToken);
			ResponseStructure<String> postResponse = createLinkedInPostPage(mediaAsset, caption, mediaType, pageURN,
					accessToken);
			handlePostResponse(response, postResponse);
		} catch (IOException e) {
			response.setStatus("Failure");
			response.setMessage("Failed to upload media to LinkedIn: " + e.getMessage());
			response.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}
		return response;
	}

	private String determineRecipeTypePage(MultipartFile file) {
		String contentType = file.getContentType();
		return contentType != null && contentType.startsWith("image") ? "urn:li:digitalmediaRecipe:feedshare-image"
				: "urn:li:digitalmediaRecipe:feedshare-video";
	}

	private String determineMediaTypePage(MultipartFile file) {
		return file.getContentType() != null && file.getContentType().startsWith("image") ? "image" : "video";
	}

	private JsonNode registerUploadPage(String recipeType, String pageURN, String accessToken) throws IOException {
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Authorization", "Bearer " + accessToken);

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

	private ResponseStructure<String> uploadImagePage(String uploadUrl, MultipartFile file, String accessToken) {
		try {
			headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
			headers.set("Authorization", "Bearer " + accessToken);

			byte[] fileContent;
			try {
				fileContent = file.getBytes();
			} catch (IOException e) {
				response.setStatus("Failure");
				response.setMessage("Failed to read image file");
				response.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
				return response;
			}

			HttpEntity<byte[]> requestEntity = config.getByteHttpEntity(fileContent, headers);

			ResponseEntity<String> responseEntity = restTemplate.exchange(uploadUrl, HttpMethod.POST, requestEntity,
					String.class);

			if (responseEntity.getStatusCode() == org.springframework.http.HttpStatus.CREATED) {
				response.setStatus("Success");
				response.setMessage("Media uploaded successfully");
				response.setCode(HttpStatus.CREATED.value());
			} else {
				response.setStatus("Failure");
				response.setMessage("Failed to upload media: " + responseEntity.getStatusCode());
				response.setCode(responseEntity.getStatusCode().value());
			}
		} catch (Exception e) {
			throw new CommonException(e.getMessage());
		}
		return response;
	}

	public ResponseStructure<String> createLinkedInPostPage(String mediaAsset, String caption, String mediaType,
			String pageURN, String accessToken) {
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
				System.out.println("Image with caption created successfully !!");
				response.setStatus("Success");
				response.setMessage("Posted To LinkedIn Page");
				response.setCode(HttpStatus.CREATED.value());
				response.setData(responseEntity.getBody());
			} else {
				response.setStatus("Failure");
				response.setMessage("Failed to create LinkedIn post: " + responseEntity.getStatusCode());
				response.setCode(responseEntity.getStatusCode().value());
			}
		} catch (Exception e) {
			throw new CommonException(e.getMessage());
		}
		return response;
	}

}
