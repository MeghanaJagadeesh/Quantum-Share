package com.qp.quantum_share.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.qp.quantum_share.configuration.ConfigurationClass;
import com.qp.quantum_share.dao.FacebookUserDao;
import com.qp.quantum_share.dao.InstagramUserDao;
import com.qp.quantum_share.dao.QuantumShareUserDao;
import com.qp.quantum_share.dao.TelegramUserDao;
import com.qp.quantum_share.dto.LinkedInProfileDto;
import com.qp.quantum_share.dto.MediaPost;
import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.dto.SocialAccounts;
import com.qp.quantum_share.response.ErrorResponse;
import com.qp.quantum_share.response.ResponseStructure;
import com.qp.quantum_share.response.ResponseWrapper;

import twitter4j.TwitterException;

@Service
public class PostService {

	@Autowired
	ResponseStructure<String> structure;

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

	public ResponseEntity<List<Object>> postOnFb(MediaPost mediaPost, MultipartFile mediaFile, QuantumShareUser user) {
		SocialAccounts socialAccounts = user.getSocialAccounts();
		List<Object> response = config.getList();
		if (mediaPost.getMediaPlatform().contains("facebook")) {
			if (socialAccounts == null || socialAccounts.getFacebookUser() == null) {
				structure.setMessage("Please connect your facebook account");
				structure.setCode(HttpStatus.NOT_FOUND.value());
				structure.setPlatform("facebook");
				structure.setStatus("error");
				structure.setData(null);
				response.add(structure);
				return new ResponseEntity<List<Object>>(response, HttpStatus.NOT_FOUND);
			}
			if (socialAccounts.getFacebookUser() != null)
				return facebookPostService.postMediaToPage(mediaPost, mediaFile, socialAccounts.getFacebookUser(),
						user);
			else {
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

	public ResponseEntity<ResponseWrapper> postOnInsta(MediaPost mediaPost, MultipartFile mediaFile,
			QuantumShareUser user) {
		SocialAccounts socialAccounts = user.getSocialAccounts();
		if (mediaPost.getMediaPlatform().contains("instagram")) {
			if (socialAccounts == null || socialAccounts.getInstagramUser() == null) {
				structure.setMessage("Please connect your Instagram account");
				structure.setCode(HttpStatus.NOT_FOUND.value());
				structure.setPlatform("instagram");
				structure.setStatus("error");
				structure.setData(null);
				return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(structure), HttpStatus.NOT_FOUND);
			}
			if (socialAccounts.getInstagramUser() != null)
				return instagramService.postMediaToPage(mediaPost, mediaFile, socialAccounts.getInstagramUser(), user);
			else {
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
			QuantumShareUser user) {
		SocialAccounts socialAccounts = user.getSocialAccounts();
		if (mediaPost.getMediaPlatform().contains("telegram")) {
			if (socialAccounts == null || socialAccounts.getTelegramUser() == null) {
				structure.setMessage("Please Connect Your Telegram Account");
				structure.setCode(HttpStatus.NOT_FOUND.value());
				structure.setPlatform("telegram");
				structure.setStatus("error");
				structure.setData(null);
				return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(structure), HttpStatus.NOT_FOUND);
			}
			if (socialAccounts.getTelegramUser() != null) {
				return telegramService.postMediaToGroup(mediaPost, mediaFile, socialAccounts.getTelegramUser(), user);
			} else {
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
				structure.setMessage("Please connect your Twitter account");
				structure.setCode(HttpStatus.NOT_FOUND.value());
				structure.setPlatform("twitter");
				structure.setStatus("error");
				structure.setData(null);
				return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(structure), HttpStatus.NOT_FOUND);
			} else {
				return twitterService.postOnTwitter(mediaPost, mediaFile, socialAccounts.getTwitterUser(), user);
			}
		}
		return null;
	}

	public ResponseEntity<ResponseWrapper> postOnLinkedIn(MediaPost mediaPost, MultipartFile mediaFile,
			SocialAccounts socialAccounts) {

		if (mediaPost.getMediaPlatform().contains("LinkedIn")) {
			if (socialAccounts == null || socialAccounts.getLinkedInProfileDto() == null) {
				structure.setMessage("Please connect your LinkedIn account");
				structure.setCode(HttpStatus.NOT_FOUND.value());
				structure.setPlatform("LinkedIn");
				structure.setStatus("error");
				structure.setData(null);
				return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(structure), HttpStatus.NOT_FOUND);
			}

			LinkedInProfileDto linkedInProfileUser = socialAccounts.getLinkedInProfileDto();
			ResponseStructure<String> response;

			if (mediaFile != null && !mediaFile.isEmpty() && mediaPost.getCaption() != null
					&& !mediaPost.getCaption().isEmpty()) {
				response = linkedInProfilePostService.uploadImageToLinkedIn(mediaFile, mediaPost.getCaption(),
						linkedInProfileUser);
			} else if (mediaPost.getCaption() != null && !mediaPost.getCaption().isEmpty()) {
				response = linkedInProfilePostService.createPostProfile(mediaPost.getCaption(), linkedInProfileUser);
			} else if (mediaFile != null && !mediaFile.isEmpty()) {
				response = linkedInProfilePostService.uploadImageToLinkedIn(mediaFile, "", linkedInProfileUser);
			} else {
				structure.setStatus("Failure");
				structure.setMessage("Please connect your LinkedIn account");
				structure.setCode(HttpStatus.BAD_REQUEST.value());
				return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(structure),
						HttpStatus.BAD_REQUEST);
			}

			// Map the response from ResponseStructure to ResponseWrapper
			structure.setStatus(response.getStatus());
			structure.setMessage(response.getMessage());
			structure.setCode(response.getCode());
			structure.setData(response.getData());
			return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(structure),
					HttpStatus.valueOf(response.getCode()));
		}
		structure.setMessage("Please connect your LinkedIn account");
		structure.setCode(HttpStatus.BAD_REQUEST.value());
		structure.setPlatform("LinkedIn");
		structure.setStatus("error");
		structure.setData(null);
		return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(structure), HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<ResponseWrapper> postOnLinkedInPage(MediaPost mediaPost, MultipartFile mediaFile,
			SocialAccounts socialAccounts) {
		ResponseStructure<String> response;

		if (mediaPost.getMediaPlatform().contains("LinkedIn")) {
			if (socialAccounts == null || socialAccounts.getLinkedInProfileDto() == null) {
				structure.setMessage("Please connect your LinkedIn account");
				structure.setCode(HttpStatus.NOT_FOUND.value());
				structure.setPlatform("LinkedIn");
				structure.setStatus("error");
				structure.setData(null);
				return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(structure), HttpStatus.NOT_FOUND);
			}

			LinkedInProfileDto linkedInProfileUser = socialAccounts.getLinkedInProfileDto();

			if (mediaFile != null && !mediaFile.isEmpty() && mediaPost.getCaption() != null
					&& !mediaPost.getCaption().isEmpty()) {
				// Both file and caption are present
				response = linkedInProfilePostService.uploadImageToLinkedInPage(mediaFile, mediaPost.getCaption(),
						linkedInProfileUser);
			} else if (mediaPost.getCaption() != null && !mediaPost.getCaption().isEmpty()) {
				// Only caption is present
				response = linkedInProfilePostService.createPostPage(mediaPost.getCaption(), linkedInProfileUser);
			} else if (mediaFile != null && !mediaFile.isEmpty()) {
				// Only file is present
				response = linkedInProfilePostService.uploadImageToLinkedInPage(mediaFile, "", linkedInProfileUser);
			} else {
				// Neither file nor caption are present
				structure.setStatus("Failure");
				structure.setMessage("Please connect your LinkedIn account");
				structure.setCode(HttpStatus.BAD_REQUEST.value());
				return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(structure),
						HttpStatus.BAD_REQUEST);
			}

			// Map the response from ResponseStructure to ResponseWrapper
			structure.setStatus(response.getStatus());
			structure.setMessage(response.getMessage());
			structure.setCode(response.getCode());
			structure.setData(response.getData());
			return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(structure),
					HttpStatus.valueOf(response.getCode()));
		}

		structure.setMessage("Please connect your LinkedIn account");
		structure.setCode(HttpStatus.BAD_REQUEST.value());
		structure.setPlatform("LinkedIn");
		structure.setStatus("error");
		structure.setData(null);
		return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(structure), HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<ResponseWrapper> prePostOnLinkedIn(MediaPost mediaPost, MultipartFile mediaFile,
			QuantumShareUser user) {
		LinkedInProfileDto linkedInProfile = user.getSocialAccounts().getLinkedInProfileDto();
		if (linkedInProfile.getLinkedinProfileURN() != null) {
			return postOnLinkedIn(mediaPost, mediaFile, user.getSocialAccounts());
		} else if (linkedInProfile.getPages().get(0).getLinkedinPageURN() != null) {
			return postOnLinkedInPage(mediaPost, mediaFile, user.getSocialAccounts());
		} else {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("user has not connected LinkedIn profile");
			structure.setPlatform("linkedIn");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(structure), HttpStatus.NOT_FOUND);
		}
	}

}
