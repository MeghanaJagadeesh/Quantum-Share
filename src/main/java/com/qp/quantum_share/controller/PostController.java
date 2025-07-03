package com.qp.quantum_share.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.qp.quantum_share.configuration.ConfigurationClass;
import com.qp.quantum_share.configuration.JwtUtilConfig;
import com.qp.quantum_share.dao.FacebookUserDao;
import com.qp.quantum_share.dao.QuantumShareUserDao;
import com.qp.quantum_share.dto.MediaPost;
import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.exception.CommonException;
import com.qp.quantum_share.helper.CommonMethod;
import com.qp.quantum_share.response.ResponseStructure;
import com.qp.quantum_share.response.ResponseWrapper;
import com.qp.quantum_share.services.PostService;
import com.qp.quantum_share.services.QuantumShareUserTracking;
import com.qp.quantum_share.services.TwitterService;

import jakarta.servlet.http.HttpServletRequest;
import twitter4j.TwitterException;

@RestController
@RequestMapping("/quantum-share")
public class PostController {

	@Autowired
	ResponseStructure<String> structure;

	@Autowired
	PostService postServices;

	@Autowired
	FacebookUserDao facebookUserDao;

	@Autowired
	ConfigurationClass configuration;

	@Autowired
	JwtUtilConfig jwtUtilConfig;

	@Autowired
	HttpServletRequest request;

	@Autowired
	QuantumShareUserDao userDao;

	@Autowired
	TwitterService twitterService;

	@Autowired
	QuantumShareUserTracking userTracking;

	@Autowired
	CommonMethod commonMethod;

	@PostMapping("/post/file/facebook")
	public ResponseEntity<List<Object>> postToFacebook(MultipartFile mediaFile, @ModelAttribute MediaPost mediaPost) {
		List<Object> response = configuration.getList();
		response.clear();
		Object userId1 = commonMethod.validateToken(request.getHeader("Authorization"));
		int userId = Integer.parseInt(userId1.toString());
		QuantumShareUser user = userDao.fetchUser(Integer.parseInt(userId1.toString()));
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("user doesn't exists, please signup");
			structure.setStatus("error");
			structure.setData(null);
			structure.setPlatform("facebook");
			response.add(structure);
			return new ResponseEntity<List<Object>>(response, HttpStatus.NOT_FOUND);
		}
//		if (user.getFirstName().equals(firstname) && user.getEmail().equals(email)) {
//			return postServices.postOnFb(mediaPost, mediaFile, user, userId);
//		}
		Map<String, Object> resp = userTracking.isValidCredit(user);
		if (!(boolean) resp.get("validcredit")) {
			structure.setCode(114);
			structure.setMessage(resp.get("message").toString());
			structure.setPlatform(null);
			structure.setStatus("error");
			structure.setData(null);
			response.add(structure);
			return new ResponseEntity<List<Object>>(response, HttpStatus.NOT_ACCEPTABLE);
		}
		try {
			if (mediaPost.getMediaPlatform() == null || mediaPost.getMediaPlatform() == "") {
				structure.setCode(HttpStatus.BAD_REQUEST.value());
				structure.setStatus("error");
				structure.setMessage("select social media platforms");
				structure.setData(null);
				structure.setPlatform("facebook");
				response.add(structure);
				return new ResponseEntity<List<Object>>(response, HttpStatus.BAD_REQUEST);
			} else {
				return postServices.postOnFb(mediaPost, mediaFile, user, userId);
			}
		} catch (NullPointerException e) {
			throw new NullPointerException(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw new CommonException(e.getMessage());
		}
	}

	@PostMapping("/post/file/instagram")
	public ResponseEntity<ResponseWrapper> postToInsta(MultipartFile[] mediaFile, @ModelAttribute MediaPost mediaPost) {
		Object userId1 = commonMethod.validateToken(request.getHeader("Authorization"));
		int userId = Integer.parseInt(userId1.toString());
		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("user doesn't exists, please signup");
			structure.setStatus("error");
			structure.setData(null);
			structure.setPlatform("instagram");
			return new ResponseEntity<ResponseWrapper>(configuration.getResponseWrapper(structure),
					HttpStatus.NOT_FOUND);
		}
//		if (user.getFirstName().equals(firstname) && user.getEmail().equals(email)) {
//			return postServices.postOnInsta(mediaPost, mediaFile, user, userId);
//		}
		Map<String, Object> resp = userTracking.isValidCredit(user);
		if (!(boolean) resp.get("validcredit")) {
			structure.setCode(114);
			structure.setMessage(resp.get("message").toString());
			structure.setPlatform(null);
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseWrapper>(configuration.getResponseWrapper(structure),
					HttpStatus.NOT_ACCEPTABLE);
		}
		try {
			if (mediaPost.getMediaPlatform() == null || mediaPost.getMediaPlatform() == "") {
				structure.setCode(HttpStatus.BAD_REQUEST.value());
				structure.setStatus("error");
				structure.setMessage("select social media platforms");
				structure.setData(null);
				structure.setPlatform("instagram");
				return new ResponseEntity<ResponseWrapper>(configuration.getResponseWrapper(structure),
						HttpStatus.BAD_REQUEST);
			} else {
				return postServices.postOnInsta(mediaPost, mediaFile, user, userId);
			}
		} catch (NullPointerException e) {
			throw new NullPointerException(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw new CommonException(e.getMessage());
		}
	}

	@PostMapping("/post/file/telegram")
	public ResponseEntity<ResponseWrapper> postToTelegram(MultipartFile mediaFile,
			@ModelAttribute MediaPost mediaPost) {
		Object userId1 = commonMethod.validateToken(request.getHeader("Authorization"));
		int userId = Integer.parseInt(userId1.toString());
		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("User doesn't Exists, Please Signup");
			structure.setStatus("error");
			structure.setData(null);
			structure.setPlatform("telegram");
			return new ResponseEntity<ResponseWrapper>(configuration.getResponseWrapper(structure),
					HttpStatus.NOT_FOUND);
		}

//		if (user.getFirstName().equals(firstname) && user.getEmail().equals(email)) {
//			return postServices.postOnTelegram(mediaPost, mediaFile, user, userId);
//		}
		Map<String, Object> resp = userTracking.isValidCredit(user);
		if (!(boolean) resp.get("validcredit")) {
			structure.setCode(114);
			structure.setMessage(resp.get("message").toString());
			structure.setPlatform(null);
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseWrapper>(configuration.getResponseWrapper(structure),
					HttpStatus.NOT_ACCEPTABLE);
		}
		try {
			if (mediaPost.getMediaPlatform() == null || mediaPost.getMediaPlatform() == "") {
				structure.setCode(HttpStatus.BAD_REQUEST.value());
				structure.setStatus("error");
				structure.setMessage("Select Social Media Platforms");
				structure.setData(null);
				structure.setPlatform("telegram");
				return new ResponseEntity<ResponseWrapper>(configuration.getResponseWrapper(structure),
						HttpStatus.BAD_REQUEST);
			} else {
				return postServices.postOnTelegram(mediaPost, mediaFile, user, userId);
			}
		} catch (Exception e) {
			structure.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
			structure.setStatus("error");
			structure.setMessage(e.getMessage());
			structure.setData(null);
			structure.setPlatform("telegram");
			return new ResponseEntity<ResponseWrapper>(configuration.getResponseWrapper(structure),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/post/file/twitter")
	public ResponseEntity<ResponseWrapper> postToTwitter(MultipartFile mediaFile, @ModelAttribute MediaPost mediaPost)
			throws TwitterException {
		Object userId1 = commonMethod.validateToken(request.getHeader("Authorization"));
		int userId = Integer.parseInt(userId1.toString());
		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("user doesn't exists, please signup");
			structure.setStatus("error");
			structure.setData(null);
			structure.setPlatform("instagram");
			return new ResponseEntity<ResponseWrapper>(configuration.getResponseWrapper(structure),
					HttpStatus.NOT_FOUND);
		}
		try {
			if (mediaPost.getMediaPlatform() == null || mediaPost.getMediaPlatform() == "") {
				structure.setCode(HttpStatus.BAD_REQUEST.value());
				structure.setStatus("error");
				structure.setMessage("select social media platforms");
				structure.setData(null);
				structure.setPlatform("twitter");
				return new ResponseEntity<ResponseWrapper>(configuration.getResponseWrapper(structure),
						HttpStatus.BAD_REQUEST);
			} else {
				return postServices.postOnTwitter(mediaPost, mediaFile, user);
			}
		} catch (NullPointerException e) {
			throw new NullPointerException(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw new CommonException(e.getMessage());
		}
	}

	@PostMapping("/post/file/linkedIn")
	public ResponseEntity<ResponseWrapper> createPostTOProfile(MultipartFile mediaFile,
			@ModelAttribute MediaPost mediaPost) {
		if (mediaFile.isEmpty() || mediaFile == null) {
			ResponseStructure<String> structure = new ResponseStructure<String>();
			structure.setMessage("Invalid file type");
			structure.setCode(HttpStatus.NOT_ACCEPTABLE.value());
			structure.setPlatform("LinkedIn");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseWrapper>(configuration.getResponseWrapper(structure),
					HttpStatus.NOT_ACCEPTABLE);

		}
		Object userId1 = commonMethod.validateToken(request.getHeader("Authorization"));
		int userId = Integer.parseInt(userId1.toString());
		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("user doesn't exists, please signup");
			structure.setStatus("error");
			structure.setData(null);
			structure.setPlatform("LinkedIn");
			return new ResponseEntity<ResponseWrapper>(configuration.getResponseWrapper(structure),
					HttpStatus.NOT_FOUND);
		}
		try {
			if (mediaPost.getMediaPlatform() == null || mediaPost.getMediaPlatform() == "") {
				structure.setCode(HttpStatus.BAD_REQUEST.value());
				structure.setStatus("error");
				structure.setMessage("select social media platforms");
				structure.setData(null);
				structure.setPlatform("LinkedIn");
				return new ResponseEntity<ResponseWrapper>(configuration.getResponseWrapper(structure),
						HttpStatus.BAD_REQUEST);
			} else {
				return postServices.prePostOnLinkedIn(mediaPost, mediaFile, user, userId);
			}
		} catch (NullPointerException e) {
			throw new NullPointerException(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw new CommonException(e.getMessage());
		}
	}

	// Youtube
	@PostMapping("/post/file/youtube")
	public ResponseEntity<ResponseWrapper> postToYoutube(MultipartFile mediaFile, @ModelAttribute MediaPost mediaPost) {
		Object userId1 = commonMethod.validateToken(request.getHeader("Authorization"));
		int userId = Integer.parseInt(userId1.toString());
		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("User doesn't Exists, Please Signup");
			structure.setStatus("error");
			structure.setData(null);
			structure.setPlatform("youtube");
			return new ResponseEntity<ResponseWrapper>(configuration.getResponseWrapper(structure),
					HttpStatus.NOT_FOUND);
		}
		try {
			if (mediaPost.getMediaPlatform() == null || mediaPost.getMediaPlatform() == "") {
				structure.setCode(HttpStatus.BAD_REQUEST.value());
				structure.setStatus("error");
				structure.setMessage("Select Social Media Platforms");
				structure.setData(null);
				structure.setPlatform("youtube");
				return new ResponseEntity<ResponseWrapper>(configuration.getResponseWrapper(structure),
						HttpStatus.BAD_REQUEST);
			} else {
				return postServices.postOnYoutube(mediaPost, mediaFile, user.getSocialAccounts(), userId);
			}
		} catch (NullPointerException e) {
			throw new NullPointerException(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw new CommonException(e.getMessage());
		}
	}

	// Reddit

	// REDDIT TEXT POSTING
	@PostMapping("/textPost")
	public ResponseEntity<ResponseStructure<JsonNode>> submitTextPost(@RequestParam("sr") String subreddit,
			@RequestParam("title") String title, @ModelAttribute MediaPost mediaPost, HttpServletRequest request) {

		ResponseStructure<JsonNode> responseStructure = new ResponseStructure<>();
		Object userId1 = commonMethod.validateToken(request.getHeader("Authorization"));
		int userId = Integer.parseInt(userId1.toString());
		QuantumShareUser user = userDao.fetchUser(userId);

		if (user == null) {
			responseStructure.setMessage("User doesn't exist, please sign up");
			responseStructure.setStatus("error");
			responseStructure.setCode(HttpStatus.NOT_FOUND.value());
			responseStructure.setPlatform("Reddit");
			responseStructure.setData(null);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseStructure);
		}

		responseStructure = postServices.submitPost(subreddit, title, user.getSocialAccounts(), mediaPost);

		return ResponseEntity.status(responseStructure.getCode()).body(responseStructure);
	}

	// REDDIT LINK POSTING
	@PostMapping("/post/file/reddit")
	public ResponseEntity<ResponseStructure<JsonNode>> submitLinkPost(@RequestParam("sr") String subreddit,
			MediaPost mediaPost, MultipartFile[] mediaFile) {

		ResponseStructure<JsonNode> responseStructure = new ResponseStructure<>();
		Object userId1 = commonMethod.validateToken(request.getHeader("Authorization"));
		int userId = Integer.parseInt(userId1.toString());
		QuantumShareUser user = userDao.fetchUser(userId);

		if (user == null) {
			responseStructure.setMessage("User doesn't exist, please sign up");
			responseStructure.setStatus("error");
			responseStructure.setCode(HttpStatus.NOT_FOUND.value());
			responseStructure.setPlatform("Reddit");
			responseStructure.setData(null);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseStructure);
		}
		return postServices.PostOnReddit(subreddit, user.getSocialAccounts(), mediaPost, mediaFile,user);
	}

	// Pinterest
	@PostMapping("/post/file/pinterest")
	public ResponseEntity<ResponseWrapper> postToPinterest(MultipartFile mediaFile,
			@ModelAttribute MediaPost mediaPost) {
		Object userId1 = commonMethod.validateToken(request.getHeader("Authorization"));
		int userId = Integer.parseInt(userId1.toString());
		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("User doesn't Exists, Please Signup");
			structure.setStatus("error");
			structure.setData(null);
			structure.setPlatform("pinterest");
			return new ResponseEntity<ResponseWrapper>(configuration.getResponseWrapper(structure),
					HttpStatus.NOT_FOUND);
		}
		try {
			if (mediaPost.getMediaPlatform() == null || mediaPost.getMediaPlatform() == "") {
				structure.setCode(HttpStatus.BAD_REQUEST.value());
				structure.setStatus("error");
				structure.setMessage("Select Social Media Platforms");
				structure.setData(null);
				structure.setPlatform("pinterest");
				return new ResponseEntity<ResponseWrapper>(configuration.getResponseWrapper(structure),
						HttpStatus.BAD_REQUEST);
			} else {
				return postServices.postOnPinterest(mediaPost, mediaFile, user.getSocialAccounts(), userId);
			}
		} catch (NullPointerException e) {
			throw new NullPointerException(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw new CommonException(e.getMessage());
		}
	}

}
