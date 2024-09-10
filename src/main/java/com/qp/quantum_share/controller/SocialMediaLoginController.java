package com.qp.quantum_share.controller;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.qp.quantum_share.configuration.JwtUtilConfig;
import com.qp.quantum_share.dao.QuantumShareUserDao;
import com.qp.quantum_share.dto.LinkedInPageDto;
import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.exception.CommonException;
import com.qp.quantum_share.response.ResponseStructure;
import com.qp.quantum_share.services.FacebookAccessTokenService;
import com.qp.quantum_share.services.InstagramService;
import com.qp.quantum_share.services.LinkedInProfileService;
import com.qp.quantum_share.services.TelegramService;
import com.qp.quantum_share.services.TwitterService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/quantum-share")
public class SocialMediaLoginController {

	@Autowired
	ResponseStructure<String> structure;

	@Autowired
	FacebookAccessTokenService faceBookAccessTokenService;

	@Autowired
	QuantumShareUserDao userDao;

	@Autowired
	InstagramService instagramService;

	@Autowired
	JwtUtilConfig jwtUtilConfig;

	@Autowired
	HttpServletRequest request;

	@Autowired
	TelegramService telegramService;

	@Autowired
	TwitterService twitterService;

	@Autowired
	LinkedInProfileService linkedInProfileService;

	@Value("${linkedin.clientId}")
	private String clientId;

	@Value("${linkedin.redirectUri}")
	private String redirectUri;

	@Value("${linkedin.scope}")
	private String scope;

	// facebook Login
	@PostMapping("/facebook/user/verify-token")
	public ResponseEntity<ResponseStructure<String>> callback(@RequestParam(required = false) String code) {
		String token = request.getHeader("Authorization");
		if (token == null || !token.startsWith("Bearer ")) {
			structure.setCode(115);
			structure.setMessage("Missing or invalid authorization token");
			structure.setStatus("error");
			structure.setPlatform(null);
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.UNAUTHORIZED);
		}
		String jwtToken = token.substring(7); // remove "Bearer " prefix
		int userId = jwtUtilConfig.extractUserId(jwtToken);
		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("user doesn't exists, please signup");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		if (code == null) {
			structure.setCode(HttpStatus.BAD_REQUEST.value());
			structure.setMessage("Please accept all the permission while login");
			structure.setPlatform("facebook");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.BAD_REQUEST);
		}
		return faceBookAccessTokenService.verifyToken(code, user);
	}

	// Instagram
	@PostMapping("/instagram/user/verify-token")
	public ResponseEntity<ResponseStructure<String>> callbackInsta(@RequestParam(required = false) String code) {
		String token = request.getHeader("Authorization");
		if (token == null || !token.startsWith("Bearer ")) {
			structure.setCode(115);
			structure.setMessage("Missing or invalid authorization token");
			structure.setStatus("error");
			structure.setPlatform(null);
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.UNAUTHORIZED);
		}
		String jwtToken = token.substring(7);
		int userId = jwtUtilConfig.extractUserId(jwtToken);
		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("user doesn't exists, please signup");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		if (code == null) {
			structure.setCode(HttpStatus.BAD_REQUEST.value());
			structure.setMessage("Please accept all the permission while login");
			structure.setPlatform("instagram");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.BAD_REQUEST);
		}
		return instagramService.verifyToken(code, user);
	}

	// Twitter Connectivity
	@GetMapping("/twitter/user/connect")
	public ResponseEntity<ResponseStructure<String>> connectTwitter() {
		String token = request.getHeader("Authorization");
		if (token == null || !token.startsWith("Bearer ")) {
			structure.setCode(115);
			structure.setMessage("Missing or invalid authorization token");
			structure.setStatus("error");
			structure.setPlatform(null);
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.UNAUTHORIZED);
		}
		String jwtToken = token.substring(7);
		int userId = jwtUtilConfig.extractUserId(jwtToken);
		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("User doesn't exists, Please Signup");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		return twitterService.getAuthorizationUrl(user);
	}

	@PostMapping("/twitter/user/verify-token")
	public ResponseEntity<ResponseStructure<String>> callbackTwitter(@RequestParam(required = false) String code) {
		String token = request.getHeader("Authorization");
		if (token == null || !token.startsWith("Bearer ")) {
			structure.setCode(115);
			structure.setMessage("Missing or invalid authorization token");
			structure.setStatus("error");
			structure.setPlatform(null);
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.UNAUTHORIZED);
		}
		String jwtToken = token.substring(7); // remove "Bearer " prefix
		int userId = jwtUtilConfig.extractUserId(jwtToken);
		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("user doesn't exists, please signup");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		if (code == null) {
			structure.setCode(HttpStatus.BAD_REQUEST.value());
			structure.setMessage("Please accept all the permission while login");
			structure.setPlatform("facebook");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.BAD_REQUEST);
		}
		return twitterService.verifyToken(code, user);
	}

	// telegram login
	@GetMapping("/telegram/user/connect")
	public ResponseEntity<ResponseStructure<String>> connectTelegram() {
		String token = request.getHeader("Authorization");
		if (token == null || !token.startsWith("Bearer ")) {
			structure.setCode(115);
			structure.setMessage("Missing or invalid authorization token");
			structure.setStatus("error");
			structure.setPlatform(null);
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.UNAUTHORIZED);
		}
		String jwtToken = token.substring(7);
		int userId = jwtUtilConfig.extractUserId(jwtToken);
		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("User doesn't exists, Please Signup");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		return telegramService.generateTelegramCode(user);
	}

	// Fetching Group Details
	@GetMapping("/telegram/user/authorization")
	public ResponseEntity<ResponseStructure<String>> getGroupDetails() {
		String token = request.getHeader("Authorization");
		if (token == null || !token.startsWith("Bearer ")) {
			structure.setCode(115);
			structure.setMessage("Missing or invalid authorization token");
			structure.setStatus("error");
			structure.setPlatform(null);
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.UNAUTHORIZED);
		}
		String jwtToken = token.substring(7);
		int userId = jwtUtilConfig.extractUserId(jwtToken);
		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("User doesn't exists, Please Signup");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		return telegramService.pollTelegramUpdates(user);
	}

	// LinkedInConnect
	@GetMapping("/linkedin/connect")
	public ResponseEntity<ResponseStructure<String>> login() {

		String token = request.getHeader("Authorization");
		if (token == null || !token.startsWith("Bearer ")) {
			structure.setCode(115);
			structure.setMessage("Missing or invalid authorization token");
			structure.setStatus("error");
			structure.setPlatform(null);
			structure.setData(null);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(structure);
		}
		String jwtToken = token.substring(7); // remove "Bearer " prefix
		Integer userId = jwtUtilConfig.extractUserId(jwtToken);
		QuantumShareUser user = userDao.fetchUser(userId);

		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("user doesn't exists, please signup");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		String authorizationUrl = linkedInProfileService.generateAuthorizationUrl();
		return ResponseEntity.status(HttpStatus.FOUND).header("Location", authorizationUrl).build();
	}

	@GetMapping("/linkedin/user/connect")
	public ResponseEntity<Map<String, String>> getLinkedInAuthUrl() {
		String token = request.getHeader("Authorization");
		Map<String, String> authUrlParams = new HashMap<>();
		if (token == null || !token.startsWith("Bearer ")) {
			// User is not authenticated or authorized
			// Customize the error response
			authUrlParams.put("status", "error");
			authUrlParams.put("code", "115");
			authUrlParams.put("message", "Missing or invalid authorization token");
			authUrlParams.put("platform", null);
			authUrlParams.put("data", null);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(authUrlParams);
		}

		String jwtToken = token.substring(7); // remove "Bearer " prefix
		int userId = jwtUtilConfig.extractUserId(jwtToken);
		QuantumShareUser user = userDao.fetchUser(userId);

		if (user == null) {
			// User is not found
			// Customize the error response
			authUrlParams.put("status", "error");
			authUrlParams.put("code", String.valueOf(HttpStatus.NOT_FOUND.value()));
			authUrlParams.put("message", "user doesn't exist, please sign up");
			authUrlParams.put("platform", null);
			authUrlParams.put("data", null);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(authUrlParams);
		}

		// User is authenticated and authorized
		// Generate the authorization URL and return a redirect response
		Map<String, String> authUrlParamsBody = getLinkedInAuth().getBody();
		if (authUrlParamsBody != null) {
			authUrlParams.putAll(authUrlParamsBody);
		}
		authUrlParams.put("status", "success");
		return ResponseEntity.ok(authUrlParams);
	}

	public ResponseEntity<Map<String, String>> getLinkedInAuth() {
		Map<String, String> authUrlParams = new HashMap<>();
		authUrlParams.put("clientId", clientId);
		authUrlParams.put("redirectUri", redirectUri);
		authUrlParams.put("scope", scope);
		return ResponseEntity.ok(authUrlParams);
	}

	@PostMapping("/linkedin/callback/success")
	public ResponseEntity<?> callbackEndpoint(@RequestParam("code") String code, @RequestParam("type") String type) {
		try {
			System.out.println("code = " + code);
			System.out.println("type = " + type);
			String token = request.getHeader("Authorization");
			if (token == null || !token.startsWith("Bearer ")) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(createErrorStructure(HttpStatus.UNAUTHORIZED, "Missing or invalid authorization token"));
			}

			String jwtToken = token.substring(7); // remove "Bearer " prefix
			int userId = jwtUtilConfig.extractUserId(jwtToken);
			QuantumShareUser user = userDao.fetchUser(userId);

			if (user == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(createErrorStructure(HttpStatus.NOT_FOUND, "User doesn't exist, please sign up"));
			}

			if (code == null) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(createErrorStructure(HttpStatus.BAD_REQUEST,
						"Please accept all the permissions while logging in"));
			}
			if ("profile".equals(type)) {
				return linkedInProfileService.getUserInfoWithToken(code, user);
			} else if ("page".equals(type)) {
				return linkedInProfileService.getOrganizationsDetailsByProfile(code, user);
			} else {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(createErrorStructure(HttpStatus.BAD_REQUEST, "Unknown connection type"));
			}
		} catch (IOException e) {
			throw new CommonException(e.getMessage());
		}
	}

	private ResponseStructure<?> createErrorStructure(HttpStatus status, String message) {
		ResponseStructure<?> structure = new ResponseStructure<>();
		structure.setCode(status.value());
		structure.setMessage(message);
		structure.setStatus("error");
		structure.setPlatform("linkedin");
		structure.setData(null);
		return structure;
	}

	@PostMapping("linkedIn/selected/page")
	public ResponseEntity<ResponseStructure<Map<String, Object>>> saveSelectedPage(
			@RequestBody LinkedInPageDto selectedLinkedInPageDto) {
		ResponseStructure<Map<String, Object>> structure = new ResponseStructure<>();
		System.out.println("controller");
		String token = request.getHeader("Authorization");
		if (token == null || !token.startsWith("Bearer ")) {
			structure.setCode(115);
			structure.setMessage("Missing or invalid authorization token");
			structure.setStatus("error");
			structure.setPlatform(null);
			structure.setData(Collections.emptyMap());
			return new ResponseEntity<>(structure, HttpStatus.UNAUTHORIZED);
		}

		String jwtToken = token.substring(7); // remove "Bearer " prefix
		int userId = jwtUtilConfig.extractUserId(jwtToken);
		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("User doesn't exist, please sign up");
			structure.setStatus("error");
			structure.setData(Collections.emptyMap());
			return new ResponseEntity<>(structure, HttpStatus.NOT_FOUND);
		}

		return linkedInProfileService.saveSelectedPage(selectedLinkedInPageDto, user);
	}

}
