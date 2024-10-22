package com.qp.quantum_share.services;

import org.springframework.stereotype.Service;

@Service
public class TestService {

//	@Autowired
//	HttpHeaders headers;
//
//	@Autowired
//	ConfigurationClass config;
//
//	@Autowired
//	RestTemplate restTemplate;
//
//	@Autowired
//	AnalyticsPostService analyticsPostService;
//
//	public String postVideoToPage(FacebookPageDetails page, MultipartFile videoFile, String message) {
//		try {
//			String url = "https://graph.facebook.com/v20.0/" + page.getFbPageId() + "/videos";
//
//			// Prepare headers
//			HttpHeaders headers = new HttpHeaders();
//			headers.setContentType(MediaType.MULTIPART_FORM_DATA);
//			headers.setBearerAuth(page.getFbPageAceessToken());
//
//			// Prepare body
//			MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
//			body.add("file", videoFile.getResource());
//			body.add("description", message);
//
//			HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
//
//			// Send request
//			RestTemplate restTemplate = new RestTemplate();
//			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
//			// Check response status and return appropriate result
//			if (response.getStatusCode().is2xxSuccessful()) {
//				return "Video posted successfully: " + response.getBody();
//			} else {
//				return "Failed to post video: " + response.getBody();
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//			return "Exception occurred while posting video: " + e.getMessage();
//		}
//	}
//
//	public ResponseEntity<ResponseStructure<String>> fetchPosts(QuantumShareUser user, String platform) {
//		if (platform.equals("facebook")) {
//			fetchFacebookPost(user);
//		}
//		if (platform.equals("instagram")) {
//			fetchInstagramPost(user);
//		}
//		return null;
//	}
//
//	private void fetchInstagramPost(QuantumShareUser user) {
//		InstagramUser insta = user.getSocialAccounts().getInstagramUser();
//
//		String url = "https://graph.facebook.com/v20.0/" + insta.getInstaUserId() + "/media?fields=media_type";
//		headers.setBearerAuth(insta.getInstUserAccessToken());
//		HttpEntity<String> requestEntity = config.getHttpEntity(headers);
//		ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, JsonNode.class);
//		if (response.getStatusCode().is2xxSuccessful() && response.getBody().get("data") != null) {
//			JsonNode dataArray = response.getBody().get("data");
////			System.out.println("data : "+dataArray);
//			for (JsonNode data : dataArray) {
////				System.out.println(data);
//				String id = data.path("id").asText();
//				String type = data.path("media_type").asText();
//				if (type.equals("IMAGE"))
//					type = "image";
//				else if (type.equals("VIDEO"))
//					type = "video";
//				analyticsPostService.savePost(id, insta.getInstaUserId(), user, type, "instagram",
//						insta.getInstaUsername());
//				analyticsPostService.getRecentPost(id, user);
//			}
//		}
//
//	}
//
//	private void fetchFacebookPost(QuantumShareUser user) {
//		List<FacebookPageDetails> pages = user.getSocialAccounts().getFacebookUser().getPageDetails();
//		for (FacebookPageDetails page : pages) {
//			String url = "https://graph.facebook.com/v20.0/me/posts";
//			headers.setBearerAuth(page.getFbPageAceessToken());
//			HttpEntity<String> requestEntity = config.getHttpEntity(headers);
//			ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity,
//					JsonNode.class);
//			if (response.getStatusCode().is2xxSuccessful() && response.getBody().get("data") != null) {
//				JsonNode dataArray = response.getBody().get("data");
//				for (JsonNode data : dataArray) {
//					String id = data.path("id").asText();
//					String[] arr = id.split("_");
//					analyticsPostService.savePost(arr[1], page.getFbPageId(), user, "image", "facebook",
//							page.getPageName());
//					analyticsPostService.getRecentPost(arr[1], user);
//
//				}
//			}
//		}
//
//	}

}
