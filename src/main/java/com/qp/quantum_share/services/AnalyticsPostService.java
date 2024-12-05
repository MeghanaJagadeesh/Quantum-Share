package com.qp.quantum_share.services;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qp.quantum_share.configuration.ConfigurationClass;
import com.qp.quantum_share.dao.PostsDao;
import com.qp.quantum_share.dao.QuantumShareUserDao;
import com.qp.quantum_share.dto.FacebookPageDetails;
import com.qp.quantum_share.dto.InstagramUser;
import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.dto.SocialMediaPosts;
import com.qp.quantum_share.dto.YoutubeUser;
import com.qp.quantum_share.exception.CommonException;
import com.qp.quantum_share.response.ResponseStructure;

@Service
public class AnalyticsPostService {

	@Autowired
	PostsDao postsDao;

	@Autowired
	QuantumShareUserDao userDao;

	@Autowired
	ConfigurationClass config;

//	@Autowired
//	ResponseStructure<String> structure;

	@Autowired
	HttpHeaders headers;

	@Autowired
	RestTemplate restTemplate;

	@Autowired
	ObjectMapper objectMapper;

	public void deletePosts(QuantumShareUser user, String platform) {
		List<SocialMediaPosts> facebookPosts = user.getPosts().stream()
				.filter(post -> post.getPlatformName().equals(platform)).collect(Collectors.toList());
		user.getPosts().removeAll(facebookPosts);
		postsDao.deletePages(facebookPosts);
		userDao.save(user);
	}

	public void savePost(String id, String profileid, QuantumShareUser qsuser, String contentType, String platform,
			String profileName) {
		SocialMediaPosts post = config.getsocialMediaPosts();
		post.setPostid(id);
		post.setMediaType(contentType);
		post.setPlatformName(platform);
		post.setPostDate(LocalDate.now());
		post.setProfileId(profileid);

		LocalTime localTime = LocalTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
		String formattedTime = localTime.format(formatter);
		post.setPostTime(formattedTime);
		post.setProfileName(profileName);
		List<SocialMediaPosts> posts = qsuser.getPosts();
		if (posts.isEmpty()) {
			List<SocialMediaPosts> list = config.getListOfPost();
			posts = list;
			posts.add(post);
		} else {
			posts.add(post);
		}
		qsuser.setPosts(posts);
		userDao.save(qsuser);
	}

	public ResponseEntity<ResponseStructure<String>> getRecentPost(String postId, QuantumShareUser user) {
		SocialMediaPosts post = postsDao.getPostByPostId(postId);
//		System.out.println(post);
		if (post == null) {
			ResponseStructure<String> structure = new ResponseStructure<String>();
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("Invalid PostId");
			structure.setPlatform(null);
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		if (post.getPlatformName().equals("facebook")) {
			return fetchFacebookRecentPost(post, user, postId);
		} else if (post.getPlatformName().equals("instagram")) {
			return fetchInstagramRecentPost(post, user, postId);
		} else if (post.getPlatformName().equals("youtube")) {
			return fetchYoutubeRecentPost(post, user, postId);
		}
		return null;
	}

	private ResponseEntity<ResponseStructure<String>> fetchYoutubeRecentPost(SocialMediaPosts post,
			QuantumShareUser user, String postId) {
		YoutubeUser youtubeUser = user.getSocialAccounts().getYoutubeUser();
		if (youtubeUser == null) {
			ResponseStructure<String> structure = new ResponseStructure<String>();
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("This post does not have an associated YouTube Channel.");
			structure.setPlatform("youtube");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		String apiUrl = "https://www.googleapis.com/youtube/v3/videos";
		headers.setBearerAuth(youtubeUser.getYoutubeUserAccessToken());
		HttpEntity<String> entity = config.getHttpEntity(headers);
		String url = apiUrl + "?part=snippet&id=" + postId;
		ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
		if (response.getBody() != null && response.getBody().has("items")
				&& response.getBody().get("items").size() > 0) {
			JsonNode snippet = response.getBody().get("items").get(0).get("snippet");
			String videoUrl = snippet.get("thumbnails").get("high").get("url").asText();
			post.setImageUrl(videoUrl);
			post.setProfileName(youtubeUser.getChannelName());
			userDao.save(user);
			ResponseStructure<String> structure = new ResponseStructure<String>();
			structure.setCode(HttpStatus.OK.value());
			structure.setData(post);
			structure.setMessage(null);
			structure.setPlatform("youtube");
			structure.setStatus("success");
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);

		} else {
			ResponseStructure<String> structure = new ResponseStructure<String>();
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("Video not found or invalid video ID.");
			structure.setPlatform("youtube");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
	}

	private ResponseEntity<ResponseStructure<String>> fetchInstagramRecentPost(SocialMediaPosts post,
			QuantumShareUser user, String postId) {
		InstagramUser instagramUser = user.getSocialAccounts().getInstagramUser();
		if (instagramUser == null) {
			ResponseStructure<String> structure = new ResponseStructure<String>();
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("This post does not have an associated Instagram Profile.");
			structure.setPlatform("instagram");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		headers.setBearerAuth(instagramUser.getInstUserAccessToken());
		HttpEntity<String> entity = config.getHttpEntity(headers);
		ResponseEntity<JsonNode> response = restTemplate.exchange(
				"https://graph.facebook.com/v20.0/" + postId + "?fields=media_url", HttpMethod.GET, entity,
				JsonNode.class);
		post.setImageUrl(response.getBody().has("media_url") ? response.getBody().get("media_url").asText() : null);
		userDao.save(user);
		ResponseStructure<String> structure = new ResponseStructure<String>();
		structure.setCode(HttpStatus.OK.value());
		structure.setData(post);
		structure.setMessage(null);
		structure.setPlatform("instagram");
		structure.setStatus("success");
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);

	}

	private ResponseEntity<ResponseStructure<String>> fetchFacebookRecentPost(SocialMediaPosts post,
			QuantumShareUser user, String postId) {
		try {
			List<FacebookPageDetails> pages = user.getSocialAccounts().getFacebookUser().getPageDetails();
			Optional<FacebookPageDetails> filteredPage = pages.stream()
					.filter(page -> page.getFbPageId().equals(post.getProfileId())).findFirst();
			FacebookPageDetails page = null;
			if (filteredPage.isPresent()) {
				page = filteredPage.get();
			} else {
				ResponseStructure<String> structure = new ResponseStructure<String>();
				structure.setCode(HttpStatus.NOT_FOUND.value());
				structure.setMessage("This post does not have an associated Facebook Page.");
				structure.setPlatform("facebook");
				structure.setStatus("error");
				structure.setData(null);
				return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);

			}
			String apiUrl = "https://graph.facebook.com/v20.0/";
			headers.setBearerAuth(page.getFbPageAceessToken());
			HttpEntity<String> requestEntity = config.getHttpEntity(headers);
			ResponseEntity<JsonNode> response = restTemplate.exchange(
					apiUrl + page.getFbPageId() + "_" + postId + "?fields=full_picture", HttpMethod.GET, requestEntity,
					JsonNode.class);
			post.setImageUrl(response.getBody().get("full_picture").asText());
			userDao.save(user);
			ResponseStructure<String> structure = new ResponseStructure<String>();
			structure.setCode(HttpStatus.OK.value());
			structure.setData(post);
			structure.setMessage(null);
			structure.setPlatform("facebook");
			structure.setStatus("success");
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
		} catch (HttpClientErrorException e) {
			System.out.println(e.getMessage());
			System.out.println(e.toString());
			try {
				Thread.sleep(50000);
				return fetchFacebookRecentPost(post, user, postId);
			} catch (InterruptedException e1) {
				Thread.currentThread().interrupt();
				return null;
			}
		}
	}

	
	public ResponseEntity<ResponseStructure<String>> getHistory(QuantumShareUser user) {
		List<SocialMediaPosts> list = postsDao.getRecentPosts(user.getUserId());
		ResponseStructure<String> structure = new ResponseStructure<String>();
		structure.setCode(HttpStatus.OK.value());
		structure.setMessage(null);
		structure.setPlatform(null);
		structure.setStatus(null);
		structure.setData(list);
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
	}

	public ResponseEntity<ResponseStructure<String>> getHistory20Images(QuantumShareUser user) {
		List<SocialMediaPosts> list = postsDao.getRecent20Posts(user.getUserId());
		ResponseStructure<String> structure = new ResponseStructure<String>();
		structure.setCode(HttpStatus.OK.value());
		structure.setMessage(null);
		structure.setPlatform(null);
		structure.setStatus(null);
		structure.setData(list);
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
	}

	public ResponseEntity<ResponseStructure<String>> viewAnalytics(QuantumShareUser user, String pid) {
		try {
			SocialMediaPosts post = postsDao.getPost(Integer.parseInt(pid));
			if (post == null) {
				ResponseStructure<String> structure = new ResponseStructure<String>();
				structure.setCode(HttpStatus.NOT_FOUND.value());
				structure.setMessage("Invalid PostId");
				structure.setPlatform(null);
				structure.setStatus("error");
				structure.setData(null);
				return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
			}
			if (post.getPlatformName().equals("facebook")) {
				if (post.getMediaType().startsWith("image")) {
					return facebookImageAnalytics(user, pid);
				} else {
					return facebookVideoAnalytics(user, pid);
				}
			} else if (post.getPlatformName().equals("instagram")) {
				return instagramAnalytics(user, pid);
			} else if (post.getPlatformName().equals("youtube")) {
				return youtubeAnalytics(user, pid);
			}
			return null;
		} catch (JsonMappingException e) {
			throw new CommonException(e.getMessage());
		} catch (JsonProcessingException e) {
			throw new CommonException(e.getMessage());
		}
	}

	private ResponseEntity<ResponseStructure<String>> youtubeAnalytics(QuantumShareUser user, String pid)
			throws JsonMappingException, JsonProcessingException {
		SocialMediaPosts post = postsDao.getPost(Integer.parseInt(pid));
		try {
			YoutubeUser youtubeUser = user.getSocialAccounts().getYoutubeUser();
			if (youtubeUser == null) {
				ResponseStructure<String> structure = new ResponseStructure<>();
				structure.setCode(HttpStatus.NOT_FOUND.value());
				structure.setMessage("This post does not have an associated YouTube Profile.");
				structure.setPlatform("youtube");
				structure.setStatus("error");
				structure.setData(null);
				return new ResponseEntity<>(structure, HttpStatus.NOT_FOUND);
			}

			String apiUrl = "https://www.googleapis.com/youtube/v3/videos?part=snippet,statistics&id="
					+ post.getPostid();
			headers.setBearerAuth(youtubeUser.getYoutubeUserAccessToken());
			HttpEntity<String> entity = config.getHttpEntity(headers);

			ResponseEntity<JsonNode> response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, JsonNode.class);
			Map<String, Object> insights = config.getMap();
			insights.clear();

			JsonNode item = response.getBody().path("items").get(0);
			JsonNode statistics = item.path("statistics");
			JsonNode snippet = item.path("snippet");

			insights.put("commentCount", statistics.path("commentCount").asText());
			insights.put("viewCount", statistics.path("viewCount").asText());
			insights.put("likeCount", statistics.path("likeCount").asText());
			insights.put("dislikeCount", statistics.path("dislikeCount").asText());
			insights.put("favoriteCount", statistics.path("favoriteCount").asText());

			String description = snippet.has("description") ? snippet.path("description").asText()
					: "No description available";
			insights.put("description", description);
			String videoUrl = "https://www.youtube.com/watch?v=" + post.getPostid();
			insights.put("full_picture", videoUrl);
			insights.put("media_type", post.getMediaType());

			ResponseStructure<String> structure = new ResponseStructure<>();
			structure.setCode(HttpStatus.OK.value());
			structure.setData(insights);
			structure.setMessage("YouTube Post analytics");
			structure.setPlatform("youtube");
			structure.setStatus("success");

			return new ResponseEntity<>(structure, HttpStatus.OK);
		} catch (NullPointerException e) {
			throw new CommonException(e.getMessage());
		} catch (HttpClientErrorException e) {
			String errorMessage = e.getResponseBodyAsString();
			JsonNode json = objectMapper.readTree(errorMessage);
			String mesg = json.get("error").get("message").asText();
			if (mesg.contains("Video not found with ID '" + post.getPostid() + "'")) {
				user.getPosts().remove(post);
				userDao.save(user);
				postsDao.deletePosts(post);
				ResponseStructure<String> structure = new ResponseStructure<>();
				structure.setCode(117);
				structure.setMessage("This post is not available on YouTube.");
				structure.setPlatform("youtube");
				structure.setStatus("error");
				structure.setData(null);
				return new ResponseEntity<>(structure, HttpStatus.OK);
			} else {
				throw new CommonException(e.getMessage());
			}
		}
	}

	private ResponseEntity<ResponseStructure<String>> facebookVideoAnalytics(QuantumShareUser user, String pid)
			throws JsonMappingException, JsonProcessingException {
		SocialMediaPosts post = postsDao.getPost(Integer.parseInt(pid));
		try {

			List<FacebookPageDetails> list = user.getSocialAccounts().getFacebookUser().getPageDetails();
			Optional<FacebookPageDetails> filteredPage = list.stream()
					.filter(page -> page.getFbPageId().equals(post.getProfileId())).findFirst();
			FacebookPageDetails page = null;
			if (filteredPage.isPresent()) {
				page = filteredPage.get();
			} else {
				ResponseStructure<String> structure = new ResponseStructure<String>();
				structure.setCode(HttpStatus.NOT_FOUND.value());
				structure.setMessage("This post does not have an associated Facebook Page.");
				structure.setPlatform(null);
				structure.setStatus("error");
				structure.setData(null);
				return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
			}
			String url = "https://graph.facebook.com/v20.0/" + post.getPostid() + "/video_insights"
					+ "?metric=total_video_views,total_video_impressions,total_video_reactions_by_type_total&access_token="
					+ page.getFbPageAceessToken();
			headers.setBearerAuth(page.getFbPageAceessToken());
			HttpEntity<String> entity = config.getHttpEntity(headers);
			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
			JsonNode root = objectMapper.readTree(response.getBody());
			JsonNode data = root.path("data");
			Map<String, Object> insights = config.getMap();
			for (JsonNode node : data) {
				String name = node.path("name").asText();
				Object value = node.path("values").get(0).path("value");
				insights.put(name, value);
			}
			ResponseEntity<JsonNode> response1 = restTemplate.exchange(
					"https://graph.facebook.com/v20.0/" + post.getPostid() + "?fields=description", HttpMethod.GET,
					entity, JsonNode.class);
			String description = response1.getBody().has("description")
					? response1.getBody().get("description").asText()
					: null;
			insights.put("description", description);
			insights.put("full_picture", post.getImageUrl());
			insights.put("media_type", post.getMediaType());
			ResponseStructure<String> structure = new ResponseStructure<String>();
			structure.setCode(HttpStatus.OK.value());
			structure.setData(insights);
			structure.setMessage("Facebook video analytics");
			structure.setPlatform("facebook");
			structure.setStatus("success");
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
		} catch (HttpClientErrorException e) {
			String errorMessage = e.getResponseBodyAsString();
			JsonNode json = objectMapper.readTree(errorMessage);
			String mesg = json.get("error").get("message").asText();
			if (mesg.contains("Unsupported get request. Object with ID")) {
				user.getPosts().remove(post);
				userDao.save(user);
				postsDao.deletePosts(post);

				ResponseStructure<String> structure = new ResponseStructure<String>();
				structure.setCode(117);
				structure.setMessage("This Post is not available in Facebook page");
				structure.setPlatform("facebook");
				structure.setStatus("error");
				structure.setData(null);
				return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
			} else {
				e.printStackTrace();
				throw new CommonException(e.getMessage());
			}

		} catch (JsonMappingException e) {
			throw new CommonException(e.getMessage());
		} catch (JsonProcessingException e) {
			throw new CommonException(e.getMessage());
		}
	}

	private ResponseEntity<ResponseStructure<String>> facebookImageAnalytics(QuantumShareUser user, String pid)
			throws JsonMappingException, JsonProcessingException {
		SocialMediaPosts post = postsDao.getPost(Integer.parseInt(pid));
		try {
			List<FacebookPageDetails> list = user.getSocialAccounts().getFacebookUser().getPageDetails();
			Optional<FacebookPageDetails> filteredPage = list.stream()
					.filter(page -> page.getFbPageId().equals(post.getProfileId())).findFirst();
			FacebookPageDetails page = null;
			if (filteredPage.isPresent()) {
				page = filteredPage.get();
			} else {
				ResponseStructure<String> structure = new ResponseStructure<String>();
				structure.setCode(HttpStatus.NOT_FOUND.value());
				structure.setMessage("This post does not have an associated Facebook Page.");
				structure.setPlatform(null);
				structure.setStatus("error");
				structure.setData(null);
				return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
			}
			String likeUrl = "https://graph.facebook.com/v20.0/" + post.getProfileId() + "_" + post.getPostid()
					+ "/insights?metric=post_reactions_by_type_total&access_token=" + page.getFbPageAceessToken();
			String commentUrl = "https://graph.facebook.com/v20.0/" + post.getProfileId() + "_" + post.getPostid()
					+ "?fields=likes.summary(true)&access_token=" + page.getFbPageAceessToken();
			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<String> entity = config.getHttpEntity(headers);
			ResponseEntity<String> likeResponse = restTemplate.exchange(likeUrl, HttpMethod.GET, entity, String.class);
			ResponseEntity<String> commentResponse = restTemplate.exchange(commentUrl, HttpMethod.GET, entity,
					String.class);
			Map<String, Object> responseData = config.getMap();

			JsonNode likeData = objectMapper.readTree(likeResponse.getBody());
			JsonNode commentData = objectMapper.readTree(commentResponse.getBody());
			JsonNode reactions = likeData.get("data").get(0).get("values").get(0).get("value");
			if (reactions.isEmpty()) {
				responseData.put("reactions", 0);
			} else {
				reactions.fields().forEachRemaining(entry -> {
					responseData.put(entry.getKey(), entry.getValue().asInt());

				});
			}
			int totalComments = commentData.get("likes").get("summary").get("total_count").asInt();
			responseData.put("total_comments", totalComments);

			headers.setBearerAuth(page.getFbPageAceessToken());
			HttpEntity<String> entity1 = config.getHttpEntity(headers);
			ResponseEntity<JsonNode> response1 = restTemplate.exchange("https://graph.facebook.com/v20.0/"
					+ post.getProfileId() + "_" + post.getPostid() + "?fields=message", HttpMethod.GET, entity1,
					JsonNode.class);
			String description = response1.getBody().path("message").asText();
			responseData.put("description", description);
			responseData.put("full_picture", post.getImageUrl());
			responseData.put("media_type", post.getMediaType());

			ResponseStructure<String> structure = new ResponseStructure<String>();
			structure.setCode(HttpStatus.OK.value());
			structure.setData(responseData);
			structure.setMessage("Facebook post analytics");
			structure.setPlatform("facebook");
			structure.setStatus("success");
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
		} catch (org.springframework.web.client.HttpClientErrorException e) {
			String errorMessage = e.getResponseBodyAsString();
			JsonNode json = objectMapper.readTree(errorMessage);
			String mesg = json.get("error").get("message").asText();
			System.out.println("mesg  "+mesg);
			System.out.println(post.getPostid());
			if (mesg.contains("Unsupported get request. Object with ID")) {
				user.getPosts().remove(post);
				userDao.save(user);
				postsDao.deletePosts(post);
				ResponseStructure<String> structure = new ResponseStructure<String>();

				structure.setCode(117);
				structure.setMessage("This Post is not available in Facebook page");
				structure.setPlatform("facebook");
				structure.setStatus("error");
				structure.setData(null);
				return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
			} else {
				System.out.println(e.getResponseBodyAsString());
				System.out.println(e.getClass()+" : "+e.getStatusText());
				e.printStackTrace();
				throw new CommonException(e.getMessage());
			}
		} catch (JsonMappingException e) {
			throw new CommonException(e.getMessage());
		} catch (JsonProcessingException e) {
			throw new CommonException(e.getMessage());
		}
	}

	private ResponseEntity<ResponseStructure<String>> instagramAnalytics(QuantumShareUser user, String pid)
			throws JsonMappingException, JsonProcessingException {
		SocialMediaPosts post = postsDao.getPost(Integer.parseInt(pid));

		try {
			InstagramUser instagramUser = user.getSocialAccounts().getInstagramUser();
			if (instagramUser == null) {
				ResponseStructure<String> structure = new ResponseStructure<String>();
				structure.setCode(HttpStatus.NOT_FOUND.value());
				structure.setMessage("This post does not have an associated Instagram Profile.");
				structure.setPlatform("instagram");
				structure.setStatus("error");
				structure.setData(null);
				return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
			}
			String apiUrl = "https://graph.facebook.com/" + post.getPostid()
					+ "/insights?metric=comments,likes,saved,shares,video_views,reach";
			headers.setBearerAuth(instagramUser.getInstUserAccessToken());
			HttpEntity<String> entity = config.getHttpEntity(headers);
			ResponseEntity<JsonNode> response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, JsonNode.class);
			Map<String, Object> insights = config.getMap();
			insights.clear();
			JsonNode data = response.getBody().path("data");
			for (JsonNode node : data) {
				String name = node.path("name").asText();
				String value = node.path("values").get(0).path("value").asText();
				insights.put(name, value);
			}
			ResponseEntity<JsonNode> response1 = restTemplate.exchange(
					"https://graph.facebook.com/" + post.getPostid() + "?fields=caption", HttpMethod.GET, entity,
					JsonNode.class);
			insights.put("description",
					response1.getBody().has("caption") ? response1.getBody().get("caption").asText() : null);
			insights.put("full_picture", post.getImageUrl());
			insights.put("media_type", post.getMediaType());
			ResponseStructure<String> structure = new ResponseStructure<String>();
			structure.setCode(HttpStatus.OK.value());
			structure.setData(insights);
			structure.setMessage("Instagram Post analytics");
			structure.setPlatform("instagram");
			structure.setStatus("success");
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);

		} catch (NullPointerException e) {
			throw new CommonException(e.getMessage());
		} catch (HttpClientErrorException e) {
			String errorMessage = e.getResponseBodyAsString();
			JsonNode json = objectMapper.readTree(errorMessage);
			String mesg = json.get("error").get("message").asText();
			if (mesg.contains("Unsupported get request. Object with ID '" + post.getPostid() + "' does not exist")) {
				user.getPosts().remove(post);
				userDao.save(user);
				postsDao.deletePosts(post);
				ResponseStructure<String> structure = new ResponseStructure<String>();
				structure.setCode(117);
				structure.setMessage("This Post is not available in Instagram Profile");
				structure.setPlatform("instagram");
				structure.setStatus("error");
				structure.setData(null);
				return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
			} else {
				throw new CommonException(e.getMessage());
			}
		}
	}

}
