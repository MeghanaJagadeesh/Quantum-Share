package com.qp.quantum_share.services;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import com.qp.quantum_share.dto.LinkedInPageDto;
import com.qp.quantum_share.dto.LinkedInProfileDto;
import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.dto.RedditDto;
import com.qp.quantum_share.dto.SocialMediaPosts;
import com.qp.quantum_share.dto.TwitterUser;
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
		System.out.println("instanta");
		post.setPostDate(Instant.now());
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

	public void savePost(String id, String profileid, QuantumShareUser qsuser, String contentType, String platform,
			String profileName, String imageUrl) {
		SocialMediaPosts post = config.getsocialMediaPosts();
		post.setPostid(id);
		post.setMediaType(contentType);
		post.setPlatformName(platform);
		post.setPostDate(Instant.now());
		post.setProfileId(profileid);

		LocalTime localTime = LocalTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
		String formattedTime = localTime.format(formatter);
		post.setPostTime(formattedTime);
		post.setProfileName(profileName);
		post.setImageUrl(imageUrl);

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
		System.out.println(post);
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
			System.out.println("instagram");
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
//			structure.setData(post);
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
		System.err.println(response);
		post.setImageUrl(response.getBody().has("media_url") ? response.getBody().get("media_url").asText() : null);
		System.out.println("before save");
		userDao.save(user);
		System.out.println("after save");
		ResponseStructure<String> structure = new ResponseStructure<String>();
		structure.setCode(HttpStatus.OK.value());
//		structure.setData(post);
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
//			structure.setData(post);
			structure.setMessage(null);
			structure.setPlatform("facebook");
			structure.setStatus("success");
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
		} catch (HttpClientErrorException e) {
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
		System.out.println("history");
		List<SocialMediaPosts> list = postsDao.getRecentPosts(user.getUserId());
		ResponseStructure<String> structure = new ResponseStructure<String>();
		structure.setCode(HttpStatus.OK.value());
		structure.setMessage(null);
		structure.setPlatform(null);
		structure.setStatus(null);
		System.out.println("bf");
		structure.setData(list);
		System.out.println("af");
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
			} else if (post.getPlatformName().equals("twitter")) {
				return twitterAnalytics(user, pid);
			} else if (post.getPlatformName().equals("reddit")) {
				return redditAnalytics(user, pid);
			} else if (post.getPlatformName().equals("linkedin")) {
				return LinkedInAnalytics(user, pid);
			}
			return null;
		} catch (JsonMappingException e) {
			throw new CommonException(e.getMessage());
		} catch (JsonProcessingException e) {
			throw new CommonException(e.getMessage());
		}
	}

	private ResponseEntity<ResponseStructure<String>> LinkedInAnalytics(QuantumShareUser user, String pid) {
		SocialMediaPosts post = postsDao.getPost(Integer.parseInt(pid));
		if (user.getSocialAccounts().isLinkedInPagePresent()) {
			LinkedInPageDto linkedInPage = user.getSocialAccounts().getLinkedInPages();

			return getLinkedPagePostAnalytics(linkedInPage.getLinkedinPageAccessToken(), user, post);
		} else {
			LinkedInProfileDto profile = user.getSocialAccounts().getLinkedInProfileDto();
			return getLinkedPagePostAnalytics(profile.getLinkedinProfileAccessToken(), user, post);
		}

	}

	private ResponseEntity<ResponseStructure<String>> getLinkedPagePostAnalytics(String access_token,
			QuantumShareUser user, SocialMediaPosts post) {
		try {
			Map<String, Object> insight = config.getMap();
			String apiUrl = "https://api.linkedin.com/rest/socialMetadata/";
			HttpHeaders headers = new HttpHeaders();
			headers.setBearerAuth(access_token);
			headers.set("Linkedin-Version", "202411");

			HttpEntity<String> requestEntity = config.getHttpEntity(headers);
			ResponseEntity<JsonNode> response = restTemplate.exchange(apiUrl + post.getPostid(), HttpMethod.GET,
					requestEntity, JsonNode.class);
			if (response.getStatusCode().is2xxSuccessful()) {
				JsonNode body = response.getBody();
				JsonNode reactionSummaries = body.path("reactionSummaries");
				JsonNode commentSummary = body.path("commentSummary");

				reactionSummaries.fields().forEachRemaining(entry -> {
					String reactionType = entry.getKey().toLowerCase();
					int count = entry.getValue().path("count").asInt();
					insight.put(reactionType, count);
				});
				insight.put("comments", commentSummary.path("count").asInt());
			}
			if (post.getMediaType().equals("video")) {
				String viewUrl = "https://api.linkedin.com/rest/videoAnalytics?q=entity&entity=" + post.getPostid()
						+ "&type=VIDEO_VIEW&aggregation=ALL";
				ResponseEntity<JsonNode> response3 = restTemplate.exchange(viewUrl, HttpMethod.GET, requestEntity,
						JsonNode.class);
				if (response3.getStatusCode().is2xxSuccessful()) {
					JsonNode data = response3.getBody().path("elements");
					if (data != null && data.isArray() && data.size() > 0) {
						insight.put("views", data.get(0).path("value").asInt());
					}
				}
			}
			ResponseStructure<String> structure = new ResponseStructure<>();
			structure.setCode(HttpStatus.OK.value());
			structure.setData(insight);
			structure.setMessage("LinkedIn Post analytics");
			structure.setPlatform("linkedin");
			structure.setStatus("success");
			return new ResponseEntity<>(structure, HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			throw new CommonException(e.getMessage());
		}
	}

	private ResponseEntity<ResponseStructure<String>> twitterAnalytics(QuantumShareUser user, String pid) {
		try {
			SocialMediaPosts post = postsDao.getPost(Integer.parseInt(pid));
			TwitterUser twitterUser = user.getSocialAccounts().getTwitterUser();
			if (twitterUser == null) {
				ResponseStructure<String> structure = new ResponseStructure<>();
				structure.setCode(HttpStatus.NOT_FOUND.value());
				structure.setMessage("This post does not have an associated Twitter Profile.");
				structure.setPlatform("twitter");
				structure.setStatus("error");
				structure.setData(null);
				return new ResponseEntity<>(structure, HttpStatus.NOT_FOUND);
			}
			String apiUrl = "https://api.twitter.com/2/tweets?ids=" + post.getPostid()
					+ "&tweet.fields=public_metrics&expansions=attachments.media_keys&media.fields=public_metrics";
			HttpHeaders headers = new HttpHeaders();
			headers.setBearerAuth(twitterUser.getAccess_token());
			HttpEntity<String> requestEntiry = config.getHttpEntity(headers);
			ResponseEntity<JsonNode> response = restTemplate.exchange(apiUrl, HttpMethod.GET, requestEntiry,
					JsonNode.class);
			JsonNode responseBody = response.getBody();
			Map<String, Object> insight = config.getMap();
			if (response.getStatusCode().is2xxSuccessful()) {
				JsonNode publicMetrics = responseBody.path("data").get(0).path("public_metrics");
				insight.put("retweetCount", publicMetrics.path("retweet_count").asInt());
				insight.put("replyCount", publicMetrics.path("reply_count").asInt());
				insight.put("likeCount", publicMetrics.path("like_count").asInt());
				insight.put("bookmarkCount", publicMetrics.path("bookmark_count").asInt());
				insight.put("impressionCount", publicMetrics.path("impression_count").asInt());

				JsonNode mediaArray = responseBody.path("includes").path("media");
				if (mediaArray.isArray() && mediaArray.size() > 0) {
					JsonNode media = mediaArray.get(0);
					insight.put("viewCount", media.path("public_metrics").path("view_count").asInt());
				}

				String text = responseBody.path("data").get(0).path("text").asText();

				Pattern pattern = Pattern.compile("(.+?)\\s(https?://\\S+)$");
				Matcher matcher = pattern.matcher(text);
				String description = "";

				if (matcher.find()) {
					description = matcher.group(1).trim();
				}
				insight.put("full_picture", post.getImageUrl());
				insight.put("media_type", post.getMediaType());
				insight.put("description", description);

			}
			ResponseStructure<String> structure = new ResponseStructure<>();
			structure.setCode(HttpStatus.OK.value());
			structure.setData(insight);
			structure.setMessage("Twitter Post analytics");
			structure.setPlatform("twitter");
			structure.setStatus("success");

			return new ResponseEntity<>(structure, HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
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
			System.out.println("likes : "+likeData);
			System.out.println("commentData : "+commentData);
			System.out.println("reactions : "+reactions);
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
			String contentType = post.getMediaType();
			String apiUrl;
			if (contentType.equals("image")) {
				apiUrl = "https://graph.facebook.com/" + post.getPostid()
						+ "/insights?metric=comments,likes,saved,shares,reach";
			} else {
				apiUrl = "https://graph.facebook.com/" + post.getPostid()
						+ "/insights?metric=comments,likes,saved,shares,ig_reels_video_view_total_time,reach";
			}
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

	public ResponseEntity<ResponseStructure<String>> redditAnalytics(QuantumShareUser user, String pid) {
		ResponseStructure<String> responseStructure = new ResponseStructure<>();
		SocialMediaPosts post = postsDao.getPost(Integer.parseInt(pid));
		RedditDto redditDto = user.getSocialAccounts().getRedditDto();
		if (redditDto == null) {
			responseStructure.setMessage("No Reddit account linked");
			responseStructure.setStatus("error");
			responseStructure.setCode(HttpStatus.BAD_REQUEST.value());
			responseStructure.setPlatform("Reddit");
			return new ResponseEntity<ResponseStructure<String>>(responseStructure, HttpStatus.NOT_FOUND);
		}
		String accessToken = redditDto.getRedditAccessToken();
		String url = "https://oauth.reddit.com/comments/" + post.getPostid();

		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", "Bearer " + accessToken);
		headers.set("User-Agent", "web:NmIDntOHG8nO6qeCzU2wDw:v1.0.0(by /u/Quantum_1824)");

		HttpEntity<String> entity = new HttpEntity<>(headers);

		try {
			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode rootNode = objectMapper.readTree(response.getBody());

			if (rootNode.isArray() && rootNode.size() >= 2) {
				JsonNode postDataNode = rootNode.get(0).path("data").path("children");
				JsonNode commentDataNode = rootNode.get(1).path("data").path("children");

				// Initialize fallback/default values
				int numComments = 0, ups = 0, numCrossposts = 0;
				String subreddit = "", postUrl = "", title = "";

				// Extract post data
				if (postDataNode.isArray() && postDataNode.size() > 0) {
					JsonNode postData = postDataNode.get(0).path("data");
					numComments = postData.path("num_comments").asInt(0);
					numCrossposts = postData.path("num_crossposts").asInt(0);
					subreddit = postData.path("subreddit").asText("");
					postUrl = postData.path("url").asText("");
					title = postData.path("title").asText(); // Extract title
				}

				// Extract comment data
				if (commentDataNode.isArray() && commentDataNode.size() > 0) {
					JsonNode commentData = commentDataNode.get(0).path("data");
					ups = commentData.path("ups").asInt(0);
				}

				// Build response data
				Map<String, String> responseData = Map.of("comments", String.valueOf(numComments), "likes",
						String.valueOf(ups), "shares", String.valueOf(numCrossposts), "subreddit_name", subreddit,
						"full_picture", postUrl, "description", title, // Include title
						"media_type", post.getMediaType() // Include selftext
				);

				responseStructure.setMessage("Data fetched successfully");
				responseStructure.setStatus("success");
				responseStructure.setCode(HttpStatus.OK.value());
				responseStructure.setPlatform("Reddit");
				responseStructure.setData(responseData);
				return ResponseEntity.ok(responseStructure);

			} else {
				responseStructure.setMessage("Invalid Reddit post data or missing comments");
				responseStructure.setStatus("error");
				responseStructure.setCode(HttpStatus.NOT_FOUND.value());
				responseStructure.setPlatform("Reddit");
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseStructure);
			}
		} catch (Exception e) {
			e.printStackTrace();
			responseStructure.setMessage("Error fetching or processing Reddit data");
			responseStructure.setStatus("error");
			responseStructure.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
			responseStructure.setPlatform("Reddit");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseStructure);
		}
	}

//	public ResponseEntity<ResponseStructure<String>> getCompleteAnalytics(QuantumShareUser user) {
//		ResponseStructure<String> structure=new ResponseStructure<String>();
//		List<SocialMediaPosts> posts = user.getPosts();
//		Map<Object, List<SocialMediaPosts>> groupedPosts = posts.stream()
//                .collect(Collectors.groupingBy(
//                        post -> LocalDate.ofInstant(post.getPostDate(), ZoneId.systemDefault())
//                ));
//
//        Map<Object, Integer> list=new HashMap<Object, Integer>();
//        groupedPosts.forEach((date, postList) -> {
//        	list.put(date, postList.size());
//            System.out.println("Date: " + date + " - Number of posts: " + postList.size());
//            postList.forEach(post -> System.out.println("  " + post));
//        });
//        
//        structure.setCode(HttpStatus.OK.value());
//        structure.setData(list);
//        structure.setMessage(null);
//        structure.setPlatform(null);
//        structure.setStatus("success");
//        return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
//	}

	public ResponseEntity<ResponseStructure<Map<String, Map<String, Integer>>>> getCompleteAnalytics(
			QuantumShareUser user) {
		ResponseStructure<Map<String, Map<String, Integer>>> structure = new ResponseStructure<>();
		List<SocialMediaPosts> posts = user.getPosts();
		Map<LocalDate, List<SocialMediaPosts>> groupedPosts = posts.stream().collect(
				Collectors.groupingBy(post -> LocalDate.ofInstant(post.getPostDate(), ZoneId.systemDefault())));

		if (groupedPosts.isEmpty()) {
			structure.setCode(HttpStatus.OK.value());
			structure.setData(new HashMap<>());
			structure.setMessage("No posts available");
			structure.setPlatform(null);
			structure.setStatus("success");
			return new ResponseEntity<>(structure, HttpStatus.OK);
		}

		LocalDate earliestDate = Collections.min(groupedPosts.keySet());
		LocalDate latestDate = Collections.max(groupedPosts.keySet());

		Map<String, Integer> maxCounts = new HashMap<>();
		Map<String, Integer> dailyCounts = new HashMap<>();

		for (LocalDate date = earliestDate; !date.isAfter(latestDate); date = date.plusDays(1)) {
			String dateString = date.toString();
			dailyCounts.put(dateString, 0);
		}

		groupedPosts.forEach((date, postList) -> {
			String dateString = date.toString();
			int postCount = postList.size();

			dailyCounts.put(dateString, postCount);

			if (postCount > 0) {
				maxCounts.put(dateString, postCount);
			}

			
		});

		Map<String, Map<String, Integer>> responseData = new HashMap<>();
		responseData.put("max", maxCounts);
		responseData.put("daily", dailyCounts);

		structure.setCode(HttpStatus.OK.value());
		structure.setData(responseData);
		structure.setMessage(null);
		structure.setPlatform(null);
		structure.setStatus("success");

		return new ResponseEntity<>(structure, HttpStatus.OK);
	}
}
