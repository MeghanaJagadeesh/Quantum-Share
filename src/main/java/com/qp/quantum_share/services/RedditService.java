package com.qp.quantum_share.services;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qp.quantum_share.dao.QuantumShareUserDao;
import com.qp.quantum_share.dao.RedditDao;
import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.dto.RedditDto;
import com.qp.quantum_share.dto.SocialAccounts;
import com.qp.quantum_share.helper.PostOnServer;
import com.qp.quantum_share.response.ResponseStructure;

@Service
public class RedditService {

	@Value("${reddit.client_id}")
	private String clientId;

	@Value("${reddit.redirect_uri}")
	private String redirectUri;

	@Value("${reddit.scope}")
	private String scope;

	@Value("${reddit.authorization_header}")
	private String authorizationHeader;

	@Value("${reddit.user_agent}")
	private String userAgent;

	@Autowired
	RedditDto redditDto;

	@Autowired
	RedditDao redditDao;

	@Autowired
	PostOnServer postOnServer;

	@Autowired
	HttpEntity<MultiValueMap<String, String>> entity;

	RestTemplate restTemplate = new RestTemplate();

	@Autowired
	QuantumShareUserDao userDao;
	
	@Autowired
	AnalyticsPostService analyticsPostService;

	private HttpHeaders headers = new HttpHeaders();
	private MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
	private ObjectMapper mapper = new ObjectMapper();
	private ResponseStructure<Map<String, String>> responseStructure = new ResponseStructure<>();
	private Map<String, String> responseData = new HashMap<>();

	public String getAuthorizationUrl() {
		return "https://www.reddit.com/api/v1/authorize?client_id=" + clientId
				+ "&response_type=code&state="+ UUID.randomUUID() +"&redirect_uri=" + redirectUri + "&duration=permanent&scope=" + scope;
	}

	// REDDIT FETCHING ACCESSTOKEN
	public ResponseStructure<Map<String, String>> getAccessToken(String code, QuantumShareUser user) {
		String url = "https://www.reddit.com/api/v1/access_token";

		// System.out.println(user);

		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		headers.set("Authorization", authorizationHeader);

		body.clear();
		body.add("grant_type", "authorization_code");
		body.add("code", code);
		body.add("redirect_uri", redirectUri);

		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

		responseStructure = new ResponseStructure<>();
		responseData.clear();

		if (response.getStatusCode().is2xxSuccessful()) {
			String responseBody = response.getBody();
			String accessToken = extractAccessToken(responseBody);
			String refreshToken = extractRefreshToken(responseBody);

			if (accessToken != null && refreshToken != null) {
				RedditDto redditDto = getUserInfo(accessToken);

				// Fetch moderated subreddits and extract subscribers count
				ResponseStructure<List<Map<String, Object>>> subscribersCountResponse = getUserSubscribersCount(
						accessToken);

				if (subscribersCountResponse.getStatus().equals("success")) {
					@SuppressWarnings("unchecked")
					List<Map<String, Object>> moderatedSubreddits = (List<Map<String, Object>>) subscribersCountResponse
							.getData();

					if (!moderatedSubreddits.isEmpty()) {
						Map<String, Object> firstSubreddit = moderatedSubreddits.get(0);
						int redditSubscribers = (Integer) firstSubreddit.get("subscribers");
						redditDto.setRedditSubscribers(redditSubscribers);
					}
				}
				redditDto.setRedditUid(redditDto.getRedditUid());
				redditDto.setRedditUserImage(redditDto.getRedditUserImage());
				redditDto.setRedditUsername(redditDto.getRedditUsername());
				redditDto.setRedditAccessToken(accessToken);
				redditDto.setRedditRefreshToken(refreshToken);
				redditDto.setTokenIssuedTime(Instant.now());

				SocialAccounts socialAccounts = user.getSocialAccounts();
				if (socialAccounts == null) {
					socialAccounts = new SocialAccounts();
					socialAccounts.setRedditDto(redditDto);
					user.setSocialAccounts(socialAccounts);
				} else {
					if (socialAccounts.getRedditDto() == null) {
						socialAccounts.setRedditDto(redditDto);
					}
				}

				userDao.save(user);

				Map<String, Object> responseData = new HashMap<>();
				responseData.put("redditUserName", redditDto.getRedditUsername());
				responseData.put("redditProfileImage", redditDto.getRedditUserImage());
				responseData.put("subscribersCount", redditDto.getRedditSubscribers());

				responseStructure.setMessage("Reddit connected successfully");
				responseStructure.setStatus("OK");
				responseStructure.setCode(HttpStatus.OK.value());
				responseStructure.setPlatform("Reddit");
				responseStructure.setData(responseData);
			} else {
				responseStructure.setMessage("Failed to extract access or refresh token");
				responseStructure.setStatus("Error");
				responseStructure.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
				responseStructure.setPlatform("Reddit");
				responseStructure.setData(null);
			}
		} else {
			responseStructure.setMessage("Failure");
			responseStructure.setStatus("Error");
			responseStructure.setCode(response.getStatusCode().value());
			responseStructure.setPlatform("Reddit");
			responseStructure.setData(null);
		}

		return responseStructure;
	}

	private String extractAccessToken(String responseBody) {
		try {
			JsonNode rootNode = mapper.readTree(responseBody);
			return rootNode.path("access_token").asText();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private String extractRefreshToken(String responseBody) {
		try {
			JsonNode rootNode = mapper.readTree(responseBody);
			return rootNode.path("refresh_token").asText();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	// CHECK ACCESSTOKEN EXPIRATION
	public ResponseEntity<ResponseStructure<Map<String, String>>> checkAndRefreshAccessToken(QuantumShareUser user) {
		SocialAccounts account = user.getSocialAccounts();
		if(account==null)
			return null;
		RedditDto redditUser = account.getRedditDto();
		
		if (redditUser == null) {
			return createErrorResponse("No Reddit account linked", HttpStatus.BAD_REQUEST);
		}
		Instant tokenIssuedTime = redditUser.getTokenIssuedTime();
		Instant expirationTime = tokenIssuedTime.plusSeconds(24 * 60 * 60);

		if (Instant.now().isAfter(expirationTime.minusSeconds(5 * 60 * 60))) {
			ResponseStructure<Map<String, String>> refreshResponse = refreshAccessToken(
					redditUser.getRedditRefreshToken());
			if (refreshResponse.getCode() == HttpStatus.OK.value()) {
				@SuppressWarnings("unchecked")
				Map<String, String> responseData = (Map<String, String>) refreshResponse.getData();
				redditUser.setRedditAccessToken(responseData.get("access_token"));
				redditUser.setRedditRefreshToken(responseData.get("refresh_token")); // Update if necessary
				redditUser.setTokenIssuedTime(Instant.now());

				redditDao.saveReddit(redditUser);

				return createSuccessResponse(refreshResponse.getData(), "Access token refreshed successfully");
			} else {
				return createErrorResponse("Failed to refresh access token", HttpStatus.INTERNAL_SERVER_ERROR);
			}
		} else {
			return createSuccessResponse(Map.of("message", "Access token is still valid"), "No need to refresh");
		}
	}

	private ResponseEntity<ResponseStructure<Map<String, String>>> createSuccessResponse(Object object,
			String message) {
		ResponseStructure<Map<String, String>> responseStructure = new ResponseStructure<>();
		responseStructure.setMessage(message);
		responseStructure.setStatus("success");
		responseStructure.setCode(HttpStatus.OK.value());
		responseStructure.setData(object);

		return ResponseEntity.ok(responseStructure);
	}

	private ResponseEntity<ResponseStructure<Map<String, String>>> createErrorResponse(String message,
			HttpStatus status) {
		ResponseStructure<Map<String, String>> responseStructure = new ResponseStructure<>();
		responseStructure.setMessage(message);
		responseStructure.setStatus("error");
		responseStructure.setCode(status.value());

		return ResponseEntity.status(status).body(responseStructure);
	}

	// REFRESH TOKEN CODE
	public ResponseStructure<Map<String, String>> refreshAccessToken(String refreshToken) {
		String url = "https://www.reddit.com/api/v1/access_token";

		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		headers.set("Authorization", authorizationHeader);

		body.clear();
		body.add("grant_type", "refresh_token");
		body.add("refresh_token", refreshToken);

		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

		responseStructure = new ResponseStructure<>();
		Map<String, String> responseData = new HashMap<>();

		if (response.getStatusCode().is2xxSuccessful()) {
			String responseBody = response.getBody();
			String accessToken = extractAccessToken(responseBody);
			// refreshToken = extractRefreshToken(responseBody);
			if (accessToken != null) {
				redditDto = getUserInfo(accessToken);
				getUserSubscribersCount(accessToken);

				responseData.put("access_token", accessToken);
				responseData.put("refresh_token", refreshToken); // Use the same refresh token
				responseData.put("uid", String.valueOf(redditDto.getRedditUid())); // uid should be a String
				responseData.put("name", redditDto.getRedditUsername());
				responseData.put("image", redditDto.getRedditUserImage());

				responseStructure.setMessage("Reddit access token refreshed successfully");
				responseStructure.setStatus("OK");
				responseStructure.setCode(HttpStatus.OK.value());
				responseStructure.setPlatform("Reddit");
				responseStructure.setData(responseData);
			} else {
				responseStructure.setMessage("Failed to refresh access token");
				responseStructure.setStatus("Error");
				responseStructure.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
				responseStructure.setPlatform("Reddit");
				responseStructure.setData(null);
			}
		} else {
			responseStructure.setMessage("Failed to refresh access token");
			responseStructure.setStatus("Error");
			responseStructure.setCode(response.getStatusCode().value());
			responseStructure.setPlatform("Reddit");
			responseStructure.setData(null);
		}

		return responseStructure;
	}

	// USER INFO FETCHING
	private RedditDto getUserInfo(String accessToken) {
		String url = "https://oauth.reddit.com/api/v1/me";

		headers.clear();
		headers.setContentType(MediaType.TEXT_PLAIN);
		headers.set("Authorization", "Bearer " + accessToken);
		headers.set("User-Agent", userAgent);

		HttpEntity<String> entity = new HttpEntity<>(headers);

		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

		responseStructure = new ResponseStructure<>();
		if (response.getStatusCode().is2xxSuccessful()) {
			try {
				JsonNode rootNode = mapper.readTree(response.getBody());
				String name = rootNode.path("name").asText();
				String iconImg = rootNode.path("icon_img").asText();

				if (iconImg == null || iconImg.isEmpty()) {
					iconImg = "https://quantumshare.quantumparadigm.in/vedio/ProfilePicture.jpg";
				}

				redditDto = new RedditDto();
				redditDto.setRedditUsername(name);
				redditDto.setRedditUserImage(iconImg);
				redditDto.setRedditAccessToken(accessToken);

				return redditDto;
			} catch (Exception e) {
				e.printStackTrace();
				responseStructure.setMessage("Failed to parse user info");
				responseStructure.setStatus("Error");
				responseStructure.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
				responseStructure.setPlatform("Reddit");
				responseStructure.setData(null);
			}
		} else {
			responseStructure.setMessage("Failed to retrieve user info");
			responseStructure.setStatus("Error");
			responseStructure.setCode(response.getStatusCode().value());
			responseStructure.setPlatform("Reddit");
			responseStructure.setData(null);
		}
		return null;
	}

	public ResponseStructure<List<Map<String, Object>>> getUserSubscribersCount(String accessToken) {
		ResponseStructure<List<Map<String, Object>>> responseStructure = new ResponseStructure<>();

		// Define the URL for fetching moderated subreddits
		String moderatedSubredditsUrl = "https://oauth.reddit.com/subreddits/mine/moderator";

		// Prepare headers
		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", "Bearer " + accessToken);
		headers.set("User-Agent", userAgent);
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<String> entity = new HttpEntity<>(headers);

		try {
			ResponseEntity<JsonNode> response = restTemplate.exchange(moderatedSubredditsUrl, HttpMethod.GET, entity,
					JsonNode.class);

			if (response.getStatusCode().is2xxSuccessful()) {
				JsonNode responseBody = response.getBody();
				if (responseBody != null && responseBody.has("data")) {
					JsonNode dataNode = responseBody.get("data");

					if (dataNode.has("children")) {
						List<Map<String, Object>> userSubredditInfoList = new ArrayList<>();

						for (JsonNode child : dataNode.get("children")) {
							JsonNode childData = child.get("data");

							// Check if it's the user's profile subreddit
							if (childData.has("subreddit_type")
									&& "user".equals(childData.get("subreddit_type").asText())) {
								String displayName = childData.get("display_name").asText();
								int subscribers = childData.get("subscribers").asInt();

								// Store user's subreddit info in a map
								Map<String, Object> userInfo = new HashMap<>();
								userInfo.put("display_name", displayName);
								userInfo.put("subscribers", subscribers);

								// Add the map to a list
								userSubredditInfoList.add(userInfo);

								responseStructure.setMessage("User subscribers count retrieved successfully");
								responseStructure.setStatus("success");
								responseStructure.setCode(HttpStatus.OK.value());
								responseStructure.setData(userSubredditInfoList);
								return responseStructure; // Return once the user's data is found
							}
						}

						if (userSubredditInfoList.isEmpty()) {
							responseStructure.setMessage("User subreddit not found.");
							responseStructure.setStatus("error");
							responseStructure.setCode(HttpStatus.NOT_FOUND.value());
							responseStructure.setData(null);
						}
					} else {
						responseStructure.setMessage("No subreddits found.");
						responseStructure.setStatus("error");
						responseStructure.setCode(HttpStatus.NOT_FOUND.value());
						responseStructure.setData(null);
					}
				} else {
					responseStructure.setMessage("Invalid response structure.");
					responseStructure.setStatus("error");
					responseStructure.setCode(HttpStatus.BAD_REQUEST.value());
					responseStructure.setData(null);
				}
			} else {
				responseStructure.setMessage("Failed to retrieve subreddits");
				responseStructure.setStatus("error");
				responseStructure.setCode(response.getStatusCode().value());
				responseStructure.setData(null);
			}
		} catch (HttpClientErrorException e) {
			responseStructure.setMessage("Error retrieving subreddits: " + e.getStatusText());
			responseStructure.setStatus("error");
			responseStructure.setCode(e.getStatusCode().value());
			responseStructure.setData(null);
		}

		return responseStructure;
	}

	// REDDIT TEXT POST
	public ResponseStructure<JsonNode> submitPost(String subreddit, String title, String text, RedditDto redditUser) {

		ResponseStructure<JsonNode> responseStructure = new ResponseStructure<>();
		String accessToken = redditUser.getRedditAccessToken();

		// Prepare headers and request entity
		// HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", "Bearer " + accessToken);
		headers.set("User-Agent", userAgent);
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("sr", subreddit);
		params.add("title", title);
		params.add("text", text);
		params.add("kind", "self"); // kind = "self" for text posts

		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

		try {
			// Make the request to Reddit API
			ResponseEntity<JsonNode> response = restTemplate.exchange("https://oauth.reddit.com/api/submit",
					HttpMethod.POST, entity, JsonNode.class);

			// Handle successful response
			if (response.getStatusCode().is2xxSuccessful()) {
				responseStructure.setMessage("Post submitted successfully");
				responseStructure.setStatus("success");
				responseStructure.setCode(HttpStatus.OK.value());
				responseStructure.setData(response.getBody());
			} else {
				// Handle non-2xx responses
				responseStructure.setMessage("Failed to submit post");
				responseStructure.setStatus("error");
				responseStructure.setCode(response.getStatusCode().value());
				responseStructure.setData(response.getBody());
			}

		} catch (HttpClientErrorException e) {
			// Handle HTTP errors (e.g., 4xx, 5xx errors)
			responseStructure.setMessage("Error submitting post: " + e.getStatusText());
			responseStructure.setStatus("error");
			responseStructure.setCode(e.getStatusCode().value());
			responseStructure.setData(null);
		}

		return responseStructure;
	}

	// REDDIT LINK POSTING
		public ResponseStructure<JsonNode> PostOnReddit(String subreddit, String title, QuantumShareUser user, MultipartFile[] mediafile,
		        RedditDto redditUser) {
		    String fileUrl = postOnServer.uploadFile(mediafile, "posts/").get(0);

		    String endpoint = "https://oauth.reddit.com/api/submit";
		    String accessToken = redditUser.getRedditAccessToken();

		    // HttpHeaders headers = new HttpHeaders();
		    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		    headers.set("Authorization", "Bearer " + accessToken);
		    headers.set("User-Agent", userAgent);

		    MultiValueMap<String, String> bodyMap = new LinkedMultiValueMap<>();
		    bodyMap.add("sr", subreddit);
		    bodyMap.add("kind", "link");
		    bodyMap.add("title", title);
		    bodyMap.add("url", fileUrl);

		    HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(bodyMap, headers);

		    ResponseStructure<JsonNode> responseStructure = new ResponseStructure<>(); // Use JsonNode as type

		    try {
		        // Make the request to Reddit API
		        ResponseEntity<JsonNode> response = restTemplate.exchange(
		            endpoint,
		            HttpMethod.POST,
		            entity,
		            JsonNode.class
		        );

		        if (response.getStatusCode().is2xxSuccessful()) {
		            JsonNode responseBody = response.getBody();

		            // Extract post URL from response
		            String redditUrl = null;
		            JsonNode jqueryArray = responseBody.get("jquery");
		            if (jqueryArray != null && jqueryArray.isArray()) {
		                for (JsonNode node : jqueryArray) {
		                    if (node.isArray() && node.size() >= 4) {
		                        JsonNode innerArray = node.get(3);
		                        if (innerArray.isArray() && innerArray.size() > 0) {
		                            String potentialUrl = innerArray.get(0).asText();
		                            if (potentialUrl.startsWith("https://www.reddit.com")) {
		                                redditUrl = potentialUrl;
		                                break;
		                            }
		                        }
		                    }
		                }
		            }

		            String postId = null;
		            if (redditUrl != null) {
		                // Extract post ID using regex
		                Pattern pattern = Pattern.compile("/comments/([^/]+)/");
		                Matcher matcher = pattern.matcher(redditUrl);
		                if (matcher.find()) {
		                    postId = matcher.group(1);
		                }
		            }

		            if (postId != null) {
		            	analyticsPostService.savePost(postId, " ", user, "image", "reddit",redditUser.getRedditUsername(), redditUrl);
		                responseStructure.setMessage("Post submitted successfully with ID: " + postId);
		            } else {
		                responseStructure.setMessage("Post submitted successfully, but ID could not be extracted.");
		            }

		            responseStructure.setStatus("success");
		            responseStructure.setPlatform("reddit");
		            responseStructure.setCode(HttpStatus.OK.value());
		            responseStructure.setData(responseBody.path("success"));
		        } else {
		            responseStructure.setMessage("Failed to submit post");
		            responseStructure.setStatus("error");
		            responseStructure.setCode(response.getStatusCode().value());
		            responseStructure.setData(response.getBody().path("success"));
		        }

		    } catch (HttpClientErrorException e) {
		        responseStructure.setMessage("Error submitting post: " + e.getStatusText());
		        responseStructure.setStatus("error");
		        responseStructure.setCode(e.getStatusCode().value());
		        responseStructure.setData(null);
		    }

		    return responseStructure;
		}
}