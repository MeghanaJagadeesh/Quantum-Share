package com.qp.quantum_share.services;

import java.util.List;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.qp.quantum_share.configuration.ConfigurationClass;
import com.qp.quantum_share.dao.FaceBookPageDao;
import com.qp.quantum_share.dao.FacebookUserDao;
import com.qp.quantum_share.dao.InstagramUserDao;
import com.qp.quantum_share.dao.LinkedInPageDao;
import com.qp.quantum_share.dao.LinkedInProfileDao;
import com.qp.quantum_share.dao.PinterestUserDao;
import com.qp.quantum_share.dao.PostsDao;
import com.qp.quantum_share.dao.QuantumShareUserDao;
import com.qp.quantum_share.dao.RedditDao;
import com.qp.quantum_share.dao.SocialAccountDao;
import com.qp.quantum_share.dao.TelegramUserDao;
import com.qp.quantum_share.dao.TwitterDao;
import com.qp.quantum_share.dao.YoutubeUserDao;
import com.qp.quantum_share.dto.FaceBookUser;
import com.qp.quantum_share.dto.FacebookPageDetails;
import com.qp.quantum_share.dto.InstagramUser;
import com.qp.quantum_share.dto.LinkedInPageDto;
import com.qp.quantum_share.dto.LinkedInProfileDto;
import com.qp.quantum_share.dto.PinterestUser;
import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.dto.RedditDto;
import com.qp.quantum_share.dto.SocialAccounts;
import com.qp.quantum_share.dto.TelegramUser;
import com.qp.quantum_share.dto.TwitterUser;
import com.qp.quantum_share.dto.YoutubeUser;
import com.qp.quantum_share.response.ResponseStructure;

@Service
public class SocialMediaLogoutService {

	@Autowired
	FacebookUserDao facebookUserDao;

	@Autowired
	FaceBookPageDao pageDao;

	@Autowired
	ConfigurationClass configurationClass;

	@Autowired
	PinterestUserDao pinterestUserDao;

	@Autowired
	SocialAccountDao accountDao;

	@Autowired
	QuantumShareUserDao userDao;

	@Autowired
	InstagramUserDao instagramUserDao;

	@Autowired
	TelegramUserDao telegramUserDao;

	@Autowired
	RedditDao redditDao;

	@Autowired
	TwitterDao twitterDao;

	@Autowired
	PostsDao postsDao;

	@Autowired
	AnalyticsPostService analyticsPostService;

	@Autowired
	LinkedInProfileDao linkedInProfileDao;

	@Autowired
	LinkedInPageDao linkedInPageDao;

	@Autowired
	SocialAccountDao socialAccountDao;

	@Autowired
	YoutubeUserDao youtubeUserDao;

	@Value("${twitter.client_id}")
	private String client_id;

	@Value("${twitter.accessToken}")
	private String accessToken;

	@Autowired
	RestTemplate restTemplate;

	public ResponseEntity<ResponseStructure<String>> disconnectFacebook(QuantumShareUser user) {
		SocialAccounts accounts = user.getSocialAccounts();
		if (accounts == null || accounts.getFacebookUser() == null) {
			ResponseStructure<String> structure = new ResponseStructure<String>();
			structure.setCode(404); // Or a custom code for Facebook not linked
			structure.setMessage("Facebook account not linked to this user");
			structure.setStatus("error");
			structure.setData(null);
			structure.setPlatform("facebook");
			return new ResponseEntity<>(structure, HttpStatus.NOT_FOUND);
		}

		FaceBookUser deleteUser = accounts.getFacebookUser();

		Hibernate.initialize(deleteUser.getPageDetails());
		List<FacebookPageDetails> pages = deleteUser.getPageDetails();
		accounts.getFacebookUser().setPageDetails(null);
		accounts.setFacebookUser(null);
		user.setSocialAccounts(accounts);
		userDao.save(user);
		facebookUserDao.deleteFbUser(deleteUser);
		pageDao.deletePage(pages);

		analyticsPostService.deletePosts(user, "facebook");

		ResponseStructure<String> structure = new ResponseStructure<String>();
		structure.setCode(HttpStatus.OK.value());
		structure.setMessage("Facebook Disconnected Successfully");
		structure.setPlatform("facebook");
		structure.setStatus("success");
		structure.setData(null);
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
	}

	public ResponseEntity<ResponseStructure<String>> disconnectInstagram(QuantumShareUser user) {
		SocialAccounts accounts = user.getSocialAccounts();
		ResponseStructure<String> structure = new ResponseStructure<String>();

		if (accounts == null || accounts.getInstagramUser() == null) {
			structure.setCode(404);
			structure.setMessage("Instagram account not linked to this user");
			structure.setStatus("error");
			structure.setData(null);
			structure.setPlatform("instagram");
			return new ResponseEntity<>(structure, HttpStatus.NOT_FOUND);
		}
		InstagramUser deleteUser = accounts.getInstagramUser();
		accounts.setInstagramUser(null);
		user.setSocialAccounts(accounts);
		userDao.save(user);

		instagramUserDao.deleteUser(deleteUser);
		analyticsPostService.deletePosts(user, "instagram");
		structure.setCode(HttpStatus.OK.value());
		structure.setMessage("Instagram Disconnected Successfully");
		structure.setPlatform("instagram");
		structure.setStatus("success");
		structure.setData(null);
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);

	}

	// Telegram
	public ResponseEntity<ResponseStructure<String>> disconnectTelegram(QuantumShareUser user) {
		SocialAccounts accounts = user.getSocialAccounts();
		ResponseStructure<String> structure = new ResponseStructure<String>();
		if (accounts == null || accounts.getTelegramUser() == null) {
			structure.setCode(404);
			structure.setMessage("Telegram account not linked to this user");
			structure.setStatus("error");
			structure.setData(null);
			structure.setPlatform("telegram");
			return new ResponseEntity<>(structure, HttpStatus.NOT_FOUND);
		}
		TelegramUser deleteUser = accounts.getTelegramUser();
		accounts.setTelegramUser(null);
		user.setSocialAccounts(accounts);
		userDao.save(user);

		telegramUserDao.deleteUser(deleteUser);

		structure.setCode(HttpStatus.OK.value());
		structure.setMessage("Telegram Disconnected Successfully");
		structure.setPlatform("telegram");
		structure.setStatus("success");
		structure.setData(null);
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
	}

	public ResponseEntity<ResponseStructure<String>> disconnectLinkedIn(QuantumShareUser user) {
		SocialAccounts accounts = user.getSocialAccounts();
		ResponseStructure<String> structure = new ResponseStructure<String>();
		if (accounts == null || (!accounts.isLinkedInPagePresent() && accounts.getLinkedInProfileDto() == null)) {
			structure.setCode(404);
			structure.setMessage("LinkedIn account not linked to this user");
			structure.setStatus("error");
			structure.setData(null);
			structure.setPlatform("LinkedIn");
			return new ResponseEntity<>(structure, HttpStatus.NOT_FOUND);
		}
		LinkedInProfileDto deleteUser;
		if (!accounts.isLinkedInPagePresent()) {
			deleteUser = accounts.getLinkedInProfileDto();
			accounts.setLinkedInProfileDto(null);
			user.setSocialAccounts(accounts);
			userDao.save(user);

			linkedInProfileDao.deleteUser(deleteUser);
			structure.setCode(HttpStatus.OK.value());
			structure.setMessage("LinkedIn Profile Disconnected Successfully");
			structure.setPlatform("LinkedIn");
			structure.setStatus("success");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
		} else if (accounts.isLinkedInPagePresent()) {
			LinkedInPageDto pages = accounts.getLinkedInPages();
			if (pages == null) {
				structure.setCode(HttpStatus.BAD_REQUEST.value());
				structure.setMessage("No LinkedIn pages found for user");
				structure.setStatus("error");
				structure.setPlatform("LinkedIn");
				structure.setData(null);
				return ResponseEntity.badRequest().body(structure);
			}
			accounts.setLinkedInPages(null);
			accounts.setLinkedInPagePresent(false);
			user.setSocialAccounts(accounts);
			userDao.save(user);

			linkedInPageDao.deletePage(pages);
			analyticsPostService.deletePosts(user, "linkedin");
			ResponseStructure<String> response = new ResponseStructure<>();
			response.setCode(HttpStatus.OK.value());
			response.setMessage("LinkedIn Page Disconnected Successfully");
			response.setStatus("success");
			response.setPlatform("LinkedIn");
			response.setData(" disconnected successfully");
			return ResponseEntity.ok(response);
		}
		return null;

	}

	// Youtube
	public ResponseEntity<ResponseStructure<String>> disconnectYoutube(QuantumShareUser user) {
		SocialAccounts accounts = user.getSocialAccounts();
		ResponseStructure<String> structure = new ResponseStructure<String>();
		if (accounts == null || accounts.getYoutubeUser() == null) {
			structure.setCode(404);
			structure.setMessage("Youtube account not linked to this user");
			structure.setStatus("error");
			structure.setData(null);
			structure.setPlatform("youtube");
			return new ResponseEntity<>(structure, HttpStatus.NOT_FOUND);
		}
		YoutubeUser deleteUser = accounts.getYoutubeUser();
		accounts.setYoutubeUser(null);
		user.setSocialAccounts(accounts);
		userDao.save(user);

		youtubeUserDao.deleteUser(deleteUser);
		analyticsPostService.deletePosts(user, "youtube");
		structure.setCode(HttpStatus.OK.value());
		structure.setMessage("Youtube Disconnected Successfully");
		structure.setPlatform("youtube");
		structure.setStatus("success");
		structure.setData(null);
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
	}

	// DISCONNECT REDDIT
	public ResponseEntity<ResponseStructure<String>> disconnectRedditAccount(QuantumShareUser user) {
		ResponseStructure<String> responseStructure = new ResponseStructure<>();
		SocialAccounts accounts = user.getSocialAccounts();

		if (accounts == null || accounts.getRedditDto() == null) {
			responseStructure.setCode(HttpStatus.NOT_FOUND.value());
			responseStructure.setMessage("Reddit account not linked to this user");
			responseStructure.setStatus("error");
			responseStructure.setData(null);
			responseStructure.setPlatform("Reddit");
			return new ResponseEntity<>(responseStructure, HttpStatus.NOT_FOUND);
		}

		RedditDto deleteUser = accounts.getRedditDto();
		accounts.setRedditDto(null);
		user.setSocialAccounts(accounts);
		userDao.save(user);
		redditDao.deleteUser(deleteUser);
		analyticsPostService.deletePosts(user, "reddit");
		responseStructure.setCode(HttpStatus.OK.value());
		responseStructure.setMessage("Reddit account disconnected successfully");
		responseStructure.setPlatform("Reddit");
		responseStructure.setStatus("success");
		responseStructure.setData(null);
		return new ResponseEntity<>(responseStructure, HttpStatus.OK);
	}

	public ResponseEntity<ResponseStructure<String>> disconnectTwitterAccount(QuantumShareUser user) {
		ResponseStructure<String> responseStructure = new ResponseStructure<>();
		SocialAccounts accounts = user.getSocialAccounts();
		if (accounts == null || accounts.getTwitterUser() == null) {
			responseStructure.setCode(HttpStatus.NOT_FOUND.value());
			responseStructure.setMessage("Twitter account not linked to this user");
			responseStructure.setStatus("error");
			responseStructure.setData(null);
			responseStructure.setPlatform("Twitter");
			return new ResponseEntity<>(responseStructure, HttpStatus.NOT_FOUND);
		}
		HttpHeaders headers = new HttpHeaders();
		TwitterUser deleteUser = accounts.getTwitterUser();
		String revokeUrl = "https://api.x.com/2/oauth2/revoke";
		MultiValueMap<String, Object> multiValueMap = configurationClass.getMultiValueMap();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		multiValueMap.add("token", accessToken);
		multiValueMap.add("token_type_hint", "access_token");
		multiValueMap.add("client_id", client_id);

		HttpEntity<MultiValueMap<String, Object>> httpRequest = configurationClass.getHttpEntityWithMap(multiValueMap,
				headers);
		ResponseEntity<JsonNode> response;
		try {
			response = restTemplate.exchange(revokeUrl, HttpMethod.POST, httpRequest, JsonNode.class);
		} catch (Exception e) {
			e.printStackTrace();
			responseStructure.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
			responseStructure.setMessage("An error occurred while disconnecting Twitter account: " + e.getMessage());
			responseStructure.setPlatform("Twitter");
			responseStructure.setStatus("error");
			responseStructure.setData(null);
			return new ResponseEntity<>(responseStructure, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		if (response.getStatusCode().is2xxSuccessful()) {
			accounts.setTwitterUser(null);
			user.setSocialAccounts(accounts);
			userDao.save(user);
			twitterDao.deleteUser(deleteUser);
			analyticsPostService.deletePosts(user, "twitter");

			responseStructure.setCode(HttpStatus.OK.value());
			responseStructure.setMessage("Twitter account disconnected successfully");
			responseStructure.setPlatform("twitter");
			responseStructure.setStatus("success");
			responseStructure.setData(null);
			return new ResponseEntity<>(responseStructure, HttpStatus.OK);
		} else {
			responseStructure.setCode(HttpStatus.BAD_REQUEST.value());
			responseStructure.setMessage("Failed to revoke Twitter account");
			responseStructure.setPlatform("Twitter");
			responseStructure.setStatus("error");
			responseStructure.setData(null);
			return new ResponseEntity<>(responseStructure, HttpStatus.BAD_REQUEST);

		}
	}
	
	
	   // Pinterest
		public ResponseEntity<ResponseStructure<String>> disconnectPinterest(QuantumShareUser user) {
			SocialAccounts accounts = user.getSocialAccounts();
			ResponseStructure<String> structure = new ResponseStructure<String>();
			if (accounts == null || accounts.getPinterestUser() == null) {
				structure.setCode(404);
				structure.setMessage("Pinterest account not linked to this user");
				structure.setStatus("error");
				structure.setData(null);
				structure.setPlatform("pinterest");
				return new ResponseEntity<>(structure, HttpStatus.NOT_FOUND);
			}
			PinterestUser deleteUser = accounts.getPinterestUser();
			accounts.setPinterestUser(null);
			user.setSocialAccounts(accounts);
			userDao.save(user);

			pinterestUserDao.deleteUser(deleteUser);
			analyticsPostService.deletePosts(user, "pinterest");
			structure.setCode(HttpStatus.OK.value());
			structure.setMessage("Pinterest Disconnected Successfully");
			structure.setPlatform("pinterest");
			structure.setStatus("success");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
		}

}
