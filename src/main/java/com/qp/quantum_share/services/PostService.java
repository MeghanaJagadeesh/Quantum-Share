package com.qp.quantum_share.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.qp.quantum_share.configuration.ConfigurationClass;
import com.qp.quantum_share.dao.FacebookUserDao;
import com.qp.quantum_share.dao.InstagramUserDao;
import com.qp.quantum_share.dao.PinterestUserDao;
import com.qp.quantum_share.dao.QuantumShareUserDao;
import com.qp.quantum_share.dao.TelegramUserDao;
import com.qp.quantum_share.dao.YoutubeUserDao;
import com.qp.quantum_share.dto.LinkedInPageDto;
import com.qp.quantum_share.dto.LinkedInProfileDto;
import com.qp.quantum_share.dto.MediaPost;
import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.dto.RedditDto;
import com.qp.quantum_share.dto.SocialAccounts;
import com.qp.quantum_share.response.ErrorResponse;
import com.qp.quantum_share.response.ResponseStructure;
import com.qp.quantum_share.response.ResponseWrapper;

import twitter4j.TwitterException;

@Service
public class PostService {

//	@Autowired
//	ResponseStructure<String> structure;

	@Autowired
	FacebookPostService facebookPostService;

	@Autowired
	InstagramService instagramService;

	@Autowired
	FacebookUserDao facebookUserDao;

	@Autowired
	QuantumShareUserDao userDao;

	@Autowired
	ErrorResponse errorResponse;

	@Autowired
	ConfigurationClass config;

	@Autowired
	InstagramUserDao instagramUserDao;

	@Autowired
	TelegramService telegramService;

	@Autowired
	TelegramUserDao telegramUserDao;

	@Autowired
	TwitterService twitterService;

	@Autowired
	LinkedInProfilePostService linkedInProfilePostService;

	@Autowired
	LinkedInProfileDto linkedInProfileDto;

	@Autowired
	YoutubeService youtubeService;

	@Autowired
	YoutubeUserDao youtubeUserDao;

	@Autowired
	RedditService redditService;
	
	@Autowired
	PinterestService pinterestService;
	
	@Autowired
	PinterestUserDao pinterestUserDao;

	public ResponseEntity<List<Object>> postOnFb(MediaPost mediaPost, MultipartFile mediaFile, QuantumShareUser user,
			int userId) {
		SocialAccounts socialAccounts = user.getSocialAccounts();
		List<Object> response = config.getList();
		if (mediaPost.getMediaPlatform().contains("facebook")) {
			if (socialAccounts == null || socialAccounts.getFacebookUser() == null) {
				ResponseStructure<String> structure = new ResponseStructure<String>();
				structure.setMessage("Please connect your facebook account");
				structure.setCode(HttpStatus.NOT_FOUND.value());
				structure.setPlatform("facebook");
				structure.setStatus("error");
				structure.setData(null);
				response.add(structure);
				return new ResponseEntity<List<Object>>(response, HttpStatus.NOT_FOUND);
			}
			if (socialAccounts.getFacebookUser() != null)
				return facebookPostService.postMediaToPage(mediaPost, mediaFile, socialAccounts.getFacebookUser(), user,
						userId);
			else {
				ResponseStructure<String> structure = new ResponseStructure<String>();
				structure.setMessage("Please connect your facebook account");
				structure.setCode(HttpStatus.NOT_FOUND.value());
				structure.setPlatform("facebook");
				structure.setStatus("error");
				structure.setData(null);
				response.add(structure);
				return new ResponseEntity<List<Object>>(response, HttpStatus.NOT_FOUND);
			}
		}
		return null;
	}

	public ResponseEntity<ResponseWrapper> postOnInsta(MediaPost mediaPost, MultipartFile[] mediaFile,
			QuantumShareUser user, int userId) {
		SocialAccounts socialAccounts = user.getSocialAccounts();
		if (mediaPost.getMediaPlatform().contains("instagram")) {
			if (socialAccounts == null || socialAccounts.getInstagramUser() == null) {
				ResponseStructure<String> structure = new ResponseStructure<String>();
				structure.setMessage("Please connect your Instagram account");
				structure.setCode(HttpStatus.NOT_FOUND.value());
				structure.setPlatform("instagram");
				structure.setStatus("error");
				structure.setData(null);
				return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(structure), HttpStatus.NOT_FOUND);
			}
			if (socialAccounts.getInstagramUser() != null)
				return instagramService.postMediaToPage(mediaPost, mediaFile, socialAccounts.getInstagramUser(),
						userId);
			else {
				ResponseStructure<String> structure = new ResponseStructure<String>();
				structure.setMessage("Please connect your Instagram account");
				structure.setCode(HttpStatus.NOT_FOUND.value());
				structure.setPlatform("facebook");
				structure.setStatus("error");
				structure.setData(null);
				return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(structure), HttpStatus.NOT_FOUND);
			}
		}
		return null;
	}

	public ResponseEntity<ResponseWrapper> postOnTelegram(MediaPost mediaPost, MultipartFile mediaFile,
			QuantumShareUser user, int userId) {
		SocialAccounts socialAccounts = user.getSocialAccounts();
		if (mediaPost.getMediaPlatform().contains("telegram")) {
			if (socialAccounts == null || socialAccounts.getTelegramUser() == null) {
				ResponseStructure<String> structure = new ResponseStructure<String>();
				structure.setMessage("Please Connect Your Telegram Account");
				structure.setCode(HttpStatus.NOT_FOUND.value());
				structure.setPlatform("telegram");
				structure.setStatus("error");
				structure.setData(null);
				return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(structure), HttpStatus.NOT_FOUND);
			}
			if (socialAccounts.getTelegramUser() != null) {
				return telegramService.postMediaToGroup(mediaPost, mediaFile, socialAccounts.getTelegramUser(), userId);
			} else {
				ResponseStructure<String> structure = new ResponseStructure<String>();
				structure.setMessage("Please Connect Your Telegram Account");
				structure.setCode(HttpStatus.NOT_FOUND.value());
				structure.setPlatform("telegram");
				structure.setStatus("error");
				structure.setData(null);
				return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(structure), HttpStatus.NOT_FOUND);
			}
		}
		return null;
	}

	public ResponseEntity<ResponseWrapper> postOnTwitter(MediaPost mediaPost, MultipartFile mediaFile,
			QuantumShareUser user) throws TwitterException {
		SocialAccounts socialAccounts = user.getSocialAccounts();
		if (mediaPost.getMediaPlatform().contains("twitter")) {
			if (socialAccounts == null || socialAccounts.getTwitterUser() == null) {
				ResponseStructure<String> structure = new ResponseStructure<String>();
				structure.setMessage("Please connect your Twitter account");
				structure.setCode(HttpStatus.NOT_FOUND.value());
				structure.setPlatform("twitter");
				structure.setStatus("error");
				structure.setData(null);
				return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(structure), HttpStatus.NOT_FOUND);
			} else {
				return twitterService.postOnTwitter(mediaPost, mediaFile, socialAccounts.getTwitterUser(), user,user.getUserId());
				}
		}
		return null;
	}

	public ResponseEntity<ResponseWrapper> postOnLinkedIn(MediaPost mediaPost, MultipartFile mediaFile,
			QuantumShareUser user, int userId) {

		if (mediaPost.getMediaPlatform().contains("LinkedIn")) {
			if (user == null || user.getSocialAccounts().getLinkedInProfileDto() == null) {
				ResponseStructure<String> structure = new ResponseStructure<String>();
				structure.setMessage("Please connect your LinkedIn account");
				structure.setCode(HttpStatus.NOT_FOUND.value());
				structure.setPlatform("LinkedIn");
				structure.setStatus("error");
				structure.setData(null);
				return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(structure), HttpStatus.NOT_FOUND);
			}
			LinkedInProfileDto linkedInProfileUser = user.getSocialAccounts().getLinkedInProfileDto();
			ResponseStructure<String> response;

			if (mediaFile != null && !mediaFile.isEmpty() && mediaPost.getCaption() != null
					&& !mediaPost.getCaption().isEmpty()) {
				response = linkedInProfilePostService.uploadImageToLinkedIn(mediaFile, mediaPost.getCaption(),
						linkedInProfileUser, userId);
				} else if (mediaPost.getCaption() != null && !mediaPost.getCaption().isEmpty()) {
				response = linkedInProfilePostService.createPostProfile(mediaPost.getCaption(), linkedInProfileUser,
						userId);

				} else if (mediaFile != null && !mediaFile.isEmpty()) {
				response = linkedInProfilePostService.uploadImageToLinkedIn(mediaFile, "", linkedInProfileUser, userId);
				} else {
				ResponseStructure<String> structure = new ResponseStructure<String>();
				structure.setStatus("Failure");
				structure.setMessage("Please connect your LinkedIn account");
				structure.setCode(HttpStatus.BAD_REQUEST.value());
				return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(structure),
						HttpStatus.BAD_REQUEST);
			}
			ResponseStructure<String> structure = new ResponseStructure<String>();

			// Map the response from ResponseStructure to ResponseWrapper
			structure.setStatus(response.getStatus());
			structure.setMessage(response.getMessage());
			structure.setCode(response.getCode());
			structure.setData(response.getData());
			structure.setPlatform("linkedin");
			return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(structure),
					HttpStatus.valueOf(response.getCode()));
		}
		ResponseStructure<String> structure = new ResponseStructure<String>();
		structure.setMessage("Please connect your LinkedIn account");
		structure.setCode(HttpStatus.BAD_REQUEST.value());
		structure.setPlatform("LinkedIn");
		structure.setStatus("error");
		structure.setData(null);
		return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(structure), HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<ResponseWrapper> postOnLinkedInPage(MediaPost mediaPost, MultipartFile mediaFile,
			QuantumShareUser user, int userId) {
		ResponseStructure<String> response;
		System.err.println(user);
		System.err.println(user.getSocialAccounts().getLinkedInPages());
		if (mediaPost.getMediaPlatform().contains("LinkedIn")) {
			if (user == null || user.getSocialAccounts().getLinkedInPages() == null) {
				ResponseStructure<String> structure = new ResponseStructure<String>();
				structure.setMessage("Please connect your LinkedIn account");
				structure.setCode(HttpStatus.NOT_FOUND.value());
				structure.setPlatform("LinkedIn");
				structure.setStatus("error");
				structure.setData(null);
				return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(structure), HttpStatus.NOT_FOUND);
			}

			LinkedInPageDto linkedInPageUser = user.getSocialAccounts().getLinkedInPages();
			String caption = mediaPost.getCaption();
			if (caption == null) {
				caption = "";
			}
			response = linkedInProfilePostService.uploadImageToLinkedInPage(mediaFile, caption, linkedInPageUser,
					userId);
			return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(response),
					HttpStatus.valueOf(response.getCode()));
		}
		return null;
	}
	

	public ResponseEntity<ResponseWrapper> prePostOnLinkedIn(MediaPost mediaPost, MultipartFile mediaFile,
			QuantumShareUser user, int userId) {
		SocialAccounts accounts = user.getSocialAccounts();
		if (!accounts.isLinkedInPagePresent()) {
			return postOnLinkedIn(mediaPost, mediaFile, user, userId);
		} else if (accounts.isLinkedInPagePresent()) {
			return postOnLinkedInPage(mediaPost, mediaFile, user, userId);
		} else {
			ResponseStructure<String> structure = new ResponseStructure<String>();
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("user has not connected LinkedIn profile");
			structure.setPlatform("linkedIn");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(structure), HttpStatus.NOT_FOUND);
		}
	}

	// Youtube
	public ResponseEntity<ResponseWrapper> postOnYoutube(MediaPost mediaPost, MultipartFile mediaFile,
			SocialAccounts socialAccounts, int userId) {
		ResponseStructure<String> structure = new ResponseStructure<String>();
		if (mediaPost.getMediaPlatform().contains("youtube")) {
			if (socialAccounts == null || socialAccounts.getYoutubeUser() == null) {
				structure.setMessage("Please Connect Your Youtube Account");
				structure.setCode(HttpStatus.NOT_FOUND.value());
				structure.setPlatform("youtube");
				structure.setStatus("error");
				structure.setData(null);
				return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(structure), HttpStatus.NOT_FOUND);
			}
			if (socialAccounts.getYoutubeUser() != null) {
				return youtubeService.postMediaToChannel(mediaPost, mediaFile,
						youtubeUserDao.findById(socialAccounts.getYoutubeUser().getYoutubeId()), userId);
			} else {
				structure.setMessage("Please Connect Your Youtube Account");
				structure.setCode(HttpStatus.NOT_FOUND.value());
				structure.setPlatform("youtube");
				structure.setStatus("error");
				structure.setData(null);
				return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(structure), HttpStatus.NOT_FOUND);
			}
		}
		return null;
	}

	// Reddit
	public ResponseStructure<JsonNode> submitPost(String subreddit, String title, SocialAccounts socialAccounts,
			MediaPost mediaPost) {

		String text = mediaPost.getCaption();
		ResponseStructure<JsonNode> responseStructure = new ResponseStructure<>();

		if (mediaPost.getMediaPlatform() == null || mediaPost.getMediaPlatform().isEmpty()) {
			responseStructure.setMessage("Please select the media platform");
			responseStructure.setStatus("error");
			responseStructure.setCode(HttpStatus.BAD_REQUEST.value());
			responseStructure.setPlatform("Reddit");
			responseStructure.setData(null);
			return responseStructure;
		}
		if (mediaPost.getMediaPlatform().contains("Reddit")) {
			if (socialAccounts == null || socialAccounts.getRedditDto() == null) {
				responseStructure.setMessage("Please connect your Reddit account");
				responseStructure.setStatus("error");
				responseStructure.setCode(HttpStatus.NOT_FOUND.value());
				responseStructure.setPlatform("Reddit");
				responseStructure.setData(null);
				return responseStructure;
			}

			RedditDto redditUser = socialAccounts.getRedditDto();

			if (subreddit == null || subreddit.trim().isEmpty()) {
				responseStructure.setMessage("Subreddit is required");
				responseStructure.setStatus("error");
				responseStructure.setCode(HttpStatus.BAD_REQUEST.value());
				responseStructure.setPlatform("Reddit");
				responseStructure.setData(null);
				return responseStructure;
			}

			if (title == null || title.trim().isEmpty()) {
				responseStructure.setMessage("Title is required");
				responseStructure.setStatus("error");
				responseStructure.setCode(HttpStatus.BAD_REQUEST.value());
				responseStructure.setPlatform("Reddit");
				responseStructure.setData(null);
				return responseStructure;
			}

			if (text == null || text.trim().isEmpty()) {
				responseStructure.setMessage("Text is required");
				responseStructure.setStatus("error");
				responseStructure.setCode(HttpStatus.BAD_REQUEST.value());
				responseStructure.setPlatform("Reddit");
				responseStructure.setData(null);
				return responseStructure;
			}
			System.out.println(subreddit + " " + title + " " + text + " " + redditUser);
			// If all parameters are present and not empty, proceed to submit the post
			responseStructure = redditService.submitPost(subreddit, title, text, redditUser);

			// Customize the response structure
			if (responseStructure.getStatus().equals("success")) {
				responseStructure.setMessage("Text post submitted successfully");
				responseStructure.setCode(HttpStatus.OK.value());
				responseStructure.setPlatform("Reddit");
			}

			return responseStructure;
		} else {
			responseStructure.setMessage("Please connect your Reddit account");
			responseStructure.setStatus("error");
			responseStructure.setCode(HttpStatus.NOT_FOUND.value());
			responseStructure.setPlatform("Reddit");
			responseStructure.setData(null);
			return responseStructure;
		}
	}

	public ResponseEntity<ResponseStructure<JsonNode>> PostOnReddit(String subreddit, SocialAccounts socialAccounts, MediaPost mediaPost, MultipartFile[] file, QuantumShareUser user) {
		ResponseStructure<JsonNode> responseStructure = new ResponseStructure<>();
		MultipartFile mediafile=file[0];
		if(mediafile.isEmpty()||mediafile==null) {
			responseStructure.setMessage("Invalid file type");
			responseStructure.setCode(HttpStatus.NOT_ACCEPTABLE.value());
			responseStructure.setPlatform("Reddit");
			responseStructure.setStatus("error");
			responseStructure.setData(null);
			return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(responseStructure);
		}
		if (mediaPost.getMediaPlatform() == null || mediaPost.getMediaPlatform().isEmpty()) {
			responseStructure.setMessage("Please select the media platform");
			responseStructure.setStatus("error");
			responseStructure.setCode(HttpStatus.BAD_REQUEST.value());
			responseStructure.setPlatform("Reddit");
			responseStructure.setData(null);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseStructure);
		}

		if (mediaPost.getMediaPlatform().contains("Reddit")) {
			if (socialAccounts == null || socialAccounts.getRedditDto() == null) {
				responseStructure.setMessage("Please connect your Reddit account");
				responseStructure.setStatus("error");
				responseStructure.setCode(HttpStatus.NOT_FOUND.value());
				responseStructure.setPlatform("Reddit");
				responseStructure.setData(null);
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseStructure);
			}
			RedditDto redditUser = socialAccounts.getRedditDto();
			if (subreddit == null || subreddit.trim().isEmpty()) {
				responseStructure.setMessage("Subreddit is required");
				responseStructure.setStatus("error");
				responseStructure.setCode(HttpStatus.BAD_REQUEST.value());
				responseStructure.setPlatform("Reddit");
				responseStructure.setData(null);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseStructure);
			}
			String title = mediaPost.getTitle();
			if (title == null || title.trim().isEmpty()) {
				responseStructure.setMessage("Title is required");
				responseStructure.setStatus("error");
				responseStructure.setCode(HttpStatus.BAD_REQUEST.value());
				responseStructure.setPlatform("Reddit");
				responseStructure.setData(null);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseStructure);
			}
			System.out.println("post service");
			responseStructure = redditService.PostOnReddit(subreddit, title, user,file, redditUser);
			return ResponseEntity.status(responseStructure.getCode()).body(responseStructure);
		} else {
			responseStructure.setMessage("Please connect your Reddit account");
			responseStructure.setStatus("error");
			responseStructure.setCode(HttpStatus.NOT_FOUND.value());
			responseStructure.setPlatform("Reddit");
			responseStructure.setData(null);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseStructure);
		}
	}
	
	
	// Pinterest	
	public ResponseEntity<ResponseWrapper> postOnPinterest(MediaPost mediaPost, MultipartFile mediaFile,
			SocialAccounts socialAccounts, int userId) {
		ResponseStructure<String> structure = new ResponseStructure<String>();
		if (mediaPost.getMediaPlatform().contains("pinterest")) {
			if (socialAccounts == null || socialAccounts.getPinterestUser() == null) {
				structure.setMessage("Please Connect Your Pinterest Account");
				structure.setCode(HttpStatus.NOT_FOUND.value());
				structure.setPlatform("pinterest");
				structure.setStatus("error");
				structure.setData(null);
				return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(structure), HttpStatus.NOT_FOUND);
			}
			if (socialAccounts.getPinterestUser() != null) {
				return pinterestService.postMediaToProfile(mediaPost, mediaFile,
						pinterestUserDao.findById(socialAccounts.getPinterestUser().getPinterestId()), userId);
			} else {
				structure.setMessage("Please Connect Your Pinterest Account");
				structure.setCode(HttpStatus.NOT_FOUND.value());
				structure.setPlatform("pinterest");
				structure.setStatus("error");
				structure.setData(null);
				return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(structure), HttpStatus.NOT_FOUND);
			}
		}
		return null;
	}


}
