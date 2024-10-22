package com.qp.quantum_share.controller;

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
import org.springframework.web.servlet.view.RedirectView;

import com.qp.quantum_share.configuration.JwtUtilConfig;
import com.qp.quantum_share.dao.QuantumShareUserDao;
import com.qp.quantum_share.dto.LinkedInPageDto;
import com.qp.quantum_share.dto.LinkedInProfileDto;
import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.exception.CommonException;
import com.qp.quantum_share.response.ResponseStructure;
import com.qp.quantum_share.services.FacebookAccessTokenService;
import com.qp.quantum_share.services.InstagramService;
import com.qp.quantum_share.services.LinkedInProfileService;
import com.qp.quantum_share.services.RedditService;
import com.qp.quantum_share.services.TelegramService;
import com.qp.quantum_share.services.TwitterService;
import com.qp.quantum_share.services.YoutubeService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/quantum-share")
public class SocialMediaLoginController {

//	@Autowired
//	ResponseStructure<String> structure;

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

	@Autowired
	YoutubeService youtubeService;
	
	@Autowired
	RedditService redditService;

	@Value("${linkedin.clientId}")
	private String clientId;

	@Value("${linkedin.redirectUri}")
	private String redirectUri;

	@Value("${linkedin.scope}")
	private String scope;

	// facebook Login
	@PostMapping("/facebook/user/verify-token")
	public ResponseEntity<ResponseStructure<String>> callback(@RequestParam(required = false) String code) {
		System.out.println("*****************1********************");
		System.out.println("code :" + code);
		String token = request.getHeader("Authorization");
		System.out.println("jwt :" + token);
		if (token == null || !token.startsWith("Bearer ")) {
			ResponseStructure<String> structure = new ResponseStructure<String>();
			structure.setCode(115);
			structure.setMessage("Missing or invalid authorization token");
			structure.setStatus("error");
			structure.setPlatform(null);
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.UNAUTHORIZED);
		}
		String jwtToken = token.substring(7); // remove "Bearer " prefix
		int userId = jwtUtilConfig.extractUserId(jwtToken);
		System.out.println("fb verify token " + userId);
		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			ResponseStructure<String> structure = new ResponseStructure<String>();
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("user doesn't exists, please signup");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		if (code == null) {
			ResponseStructure<String> structure = new ResponseStructure<String>();
			structure.setCode(HttpStatus.BAD_REQUEST.value());
			structure.setMessage("Please accept all the permission while login");
			structure.setPlatform("facebook");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.BAD_REQUEST);
		}
		return faceBookAccessTokenService.verifyToken(code, user, userId);
	}

	// Instagram
	@PostMapping("/instagram/user/verify-token")
	public ResponseEntity<ResponseStructure<String>> callbackInsta(@RequestParam(required = false) String code) {
		String token = request.getHeader("Authorization");
		if (token == null || !token.startsWith("Bearer ")) {
			ResponseStructure<String> structure = new ResponseStructure<String>();
			structure.setCode(115);
			structure.setMessage("Missing or invalid authorization token");
			structure.setStatus("error");
			structure.setPlatform(null);
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.UNAUTHORIZED);
		}
		String jwtToken = token.substring(7);
		int userId = jwtUtilConfig.extractUserId(jwtToken);
		System.out.println("insta verify token " + userId);

		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			ResponseStructure<String> structure = new ResponseStructure<String>();
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("user doesn't exists, please signup");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		if (code == null) {
			ResponseStructure<String> structure = new ResponseStructure<String>();
			structure.setCode(HttpStatus.BAD_REQUEST.value());
			structure.setMessage("Please accept all the permission while login");
			structure.setPlatform("instagram");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.BAD_REQUEST);
		}
		return instagramService.verifyToken(code, user, userId);
	}

	// Twitter Connectivity
	@GetMapping("/twitter/user/connect")
	public ResponseEntity<ResponseStructure<String>> connectTwitter() {
		String token = request.getHeader("Authorization");
		if (token == null || !token.startsWith("Bearer ")) {
			ResponseStructure<String> structure = new ResponseStructure<String>();
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
			ResponseStructure<String> structure = new ResponseStructure<String>();
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
			ResponseStructure<String> structure = new ResponseStructure<String>();
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
			ResponseStructure<String> structure = new ResponseStructure<String>();
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("user doesn't exists, please signup");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		if (code == null) {
			ResponseStructure<String> structure = new ResponseStructure<String>();
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
			ResponseStructure<String> structure = new ResponseStructure<String>();
			structure.setCode(115);
			structure.setMessage("Missing or invalid authorization token");
			structure.setStatus("error");
			structure.setPlatform(null);
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.UNAUTHORIZED);
		}
		String jwtToken = token.substring(7);
		int userId = jwtUtilConfig.extractUserId(jwtToken);
		System.out.println("telegram verify token " + userId);

		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			ResponseStructure<String> structure = new ResponseStructure<String>();
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
			ResponseStructure<String> structure = new ResponseStructure<String>();
			structure.setCode(115);
			structure.setMessage("Missing or invalid authorization token");
			structure.setStatus("error");
			structure.setPlatform(null);
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.UNAUTHORIZED);
		}
		String jwtToken = token.substring(7);
		int userId = jwtUtilConfig.extractUserId(jwtToken);
		System.out.println("telegram auth " + userId);

		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			ResponseStructure<String> structure = new ResponseStructure<String>();
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("User doesn't exists, Please Signup");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		return telegramService.pollTelegramUpdates(user, userId);
	}

// LinkedInConnect
	@GetMapping("/linkedin/connect")
	public ResponseEntity<ResponseStructure<String>> login() {

		String token = request.getHeader("Authorization");
		if (token == null || !token.startsWith("Bearer ")) {
			ResponseStructure<String> structure = new ResponseStructure<String>();
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
			ResponseStructure<String> structure = new ResponseStructure<String>();
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
			authUrlParams.put("status", "error");
			authUrlParams.put("code", String.valueOf(HttpStatus.NOT_FOUND.value()));
			authUrlParams.put("message", "user doesn't exist, please sign up");
			authUrlParams.put("platform", null);
			authUrlParams.put("data", null);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(authUrlParams);
		}

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
	public ResponseEntity<?> callbackEndpoint(@RequestParam("code") String code) {
		try {
			String token = request.getHeader("Authorization");
			if (token == null || !token.startsWith("Bearer ")) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(createErrorStructure(HttpStatus.UNAUTHORIZED, "Missing or invalid authorization token"));
			}

			String jwtToken = token.substring(7); // remove "Bearer " prefix
			int userId = jwtUtilConfig.extractUserId(jwtToken);
			System.out.println("linkedin callback " + userId);

			QuantumShareUser user = userDao.fetchUser(userId);

			if (user == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(createErrorStructure(HttpStatus.NOT_FOUND, "User doesn't exist, please sign up"));
			}

			if (code == null) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(createErrorStructure(HttpStatus.BAD_REQUEST,
						"Please accept all the permissions while logging in"));
			}
			return linkedInProfileService.getPagesAndProfile(code, user, userId);
		} catch (Exception e) {
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
	public ResponseEntity<Object> saveSelectedPage(@RequestBody Map<String, Object> linkedinPageData,
			@RequestParam("type") String type) {
		ResponseStructure<String> structure = new ResponseStructure<>();
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
		System.out.println("linkedin select page " + userId);

		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("User doesn't exist, please sign up");
			structure.setStatus("error");
			structure.setData(Collections.emptyMap());
			return new ResponseEntity<>(structure, HttpStatus.NOT_FOUND);
		}
		if ("profile".equals(type)) {
			LinkedInProfileDto profile = new LinkedInProfileDto();
			profile.setLinkedinProfileAccessToken(linkedinPageData.get("accessToken").toString());
			profile.setLinkedinProfileUserName(linkedinPageData.get("name").toString());
			profile.setLinkedinProfileURN(linkedinPageData.get("urn").toString());
			profile.setLinkedinProfileImage(linkedinPageData.get("profile_image").toString());
			return linkedInProfileService.saveLinkedInProfile(profile, user, userId);
		} else if ("page".equals(type)) {
			LinkedInPageDto page = new LinkedInPageDto();
			page.setLinkedinPageAccessToken(linkedinPageData.get("accessToken").toString());
			page.setLinkedinPageName(linkedinPageData.get("name").toString());
			page.setLinkedinPageURN(linkedinPageData.get("urn").toString());
			page.setLinkedinPageImage(linkedinPageData.get("profile_image").toString());
			return linkedInProfileService.saveSelectedPage(page, user, userId);
		} else {
			structure.setCode(HttpStatus.BAD_GATEWAY.value());
			structure.setMessage("Please specify the type");
			structure.setStatus("error");
			structure.setPlatform(null);
			structure.setData(null);
			return new ResponseEntity<>(structure, HttpStatus.BAD_GATEWAY);

		}
	}

	// Youtube Connection
	@GetMapping("/youtube/user/connect")
	public ResponseEntity<ResponseStructure<String>> connectYoutube() {
		String token = request.getHeader("Authorization");
		if (token == null || !token.startsWith("Bearer ")) {
			ResponseStructure<String> structure = new ResponseStructure<String>();
			structure.setCode(115);
			structure.setMessage("Missing or invalid authorization token");
			structure.setStatus("error");
			structure.setPlatform(null);
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.UNAUTHORIZED);
		}
		String jwtToken = token.substring(7);
		int userId = jwtUtilConfig.extractUserId(jwtToken);
		System.out.println("youtube xonnect " + userId);

		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			ResponseStructure<String> structure = new ResponseStructure<String>();
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("User doesn't exists, Please Signup");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		return youtubeService.getAuthorizationUrl(user);
	}

	// Youtube
	@PostMapping("/youtube/user/verify-token")
	public ResponseEntity<ResponseStructure<String>> callbackYoutube(@RequestParam(required = false) String code) {
		String token = request.getHeader("Authorization");
		if (token == null || !token.startsWith("Bearer ")) {
			ResponseStructure<String> structure = new ResponseStructure<String>();
			structure.setCode(115);
			structure.setMessage("Missing or invalid authorization token");
			structure.setStatus("error");
			structure.setPlatform(null);
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.UNAUTHORIZED);
		}
		String jwtToken = token.substring(7);
		int userId = jwtUtilConfig.extractUserId(jwtToken);
		System.out.println("youtube verify token " + userId);

		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			ResponseStructure<String> structure = new ResponseStructure<String>();
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("User doesn't Exists, Please Signup");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		if (code == null) {
			ResponseStructure<String> structure = new ResponseStructure<String>();
			structure.setCode(HttpStatus.BAD_REQUEST.value());
			structure.setMessage("Please accept all the permission while login");
			structure.setPlatform("youtube");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.BAD_REQUEST);
		}
		return youtubeService.verifyToken(code, user, userId);
	}
	
	 @GetMapping("/connect-reddit")
	    public RedirectView authorize() {
	        String authorizationUrl = redditService.getAuthorizationUrl();
	        return new RedirectView(authorizationUrl);
	    }
	    
	    
	    @GetMapping("/connect/reddit")
	    public ResponseEntity<Map<String, String>> getRedditAuthUrl(HttpServletRequest request) {
	        String token = request.getHeader("Authorization");
	        Map<String, String> authUrlParams = new HashMap<>();
	        
	        if (token == null || !token.startsWith("Bearer ")) {
	            authUrlParams.put("status", "error");
	            authUrlParams.put("code", "115");
	            authUrlParams.put("message", "Missing or invalid authorization token");
	            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(authUrlParams);
	        }

	        String jwtToken = token.substring(7); // remove "Bearer " prefix
	        int userId = jwtUtilConfig.extractUserId(jwtToken);
	        QuantumShareUser user = userDao.fetchUser(userId);

	        if (user == null) {
	            authUrlParams.put("status", "error");
	            authUrlParams.put("code", String.valueOf(HttpStatus.NOT_FOUND.value()));
	            authUrlParams.put("message", "User doesn't exist, please sign up");
	            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(authUrlParams);
	        }
	        authUrlParams.put("client_id", clientId);
	        authUrlParams.put("redirect_uri", redirectUri);
	        authUrlParams.put("scope", scope);
	        authUrlParams.put("status", "success");

	        return ResponseEntity.ok(authUrlParams);
		    }

		    public ResponseEntity<Map<String, String>> getRedditAuth() {
		        Map<String, String> authUrlParams = new HashMap<>();
		        authUrlParams.put("client_id", clientId);
		        authUrlParams.put("response_type", "code");
		        authUrlParams.put("state", "string");
		        authUrlParams.put("redirect_uri", redirectUri);
		        authUrlParams.put("duration", "permanent");
		        authUrlParams.put("scope", scope);
		        return ResponseEntity.ok(authUrlParams);
		    }
		 
		 @GetMapping("/callback-redirect")
		 public ResponseEntity<ResponseStructure<Map<String, String>>> handleRedirect(@RequestParam("code") String code) {
			 String token = request.getHeader("Authorization");
		        ResponseStructure<Map<String, String>> responseStructure = new ResponseStructure<>();

		        if (token == null || !token.startsWith("Bearer ")) {
		            responseStructure.setMessage("Missing or invalid authorization token");
		            responseStructure.setStatus("error");
		            responseStructure.setCode(HttpStatus.UNAUTHORIZED.value());
		            responseStructure.setPlatform("Reddit");
		            responseStructure.setData(null);
		            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseStructure);
		        }

		        String jwtToken = token.substring(7); // remove "Bearer " prefix
		        int userId = jwtUtilConfig.extractUserId(jwtToken);
		        QuantumShareUser user = userDao.fetchUser(userId);

		        if (user == null) {
		            responseStructure.setMessage("User doesn't exist, please sign up");
		            responseStructure.setStatus("error");
		            responseStructure.setCode(HttpStatus.NOT_FOUND.value());
		            responseStructure.setPlatform("Reddit");
		            responseStructure.setData(null);
		            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseStructure);
		        }

		        responseStructure = redditService.getAccessToken(code,user);

		        // Customize the response structure
		        if (responseStructure.getStatus().equals("success")) {
		            responseStructure.setMessage("Reddit connected successfully");
		            responseStructure.setCode(HttpStatus.OK.value());
		            responseStructure.setPlatform("Reddit");
		        }

		        return ResponseEntity.status(responseStructure.getCode()).body(responseStructure);
		    
		    }
		 
		 
		 @PostMapping("/callback/reddit")
		 public ResponseEntity<ResponseStructure<Map<String, String>>> handleRedirectUrl(@RequestParam("code") String code) {
			 String token = request.getHeader("Authorization");
		        ResponseStructure<Map<String, String>> responseStructure = new ResponseStructure<>();

		      
		        
		        if (token == null || !token.startsWith("Bearer ")) {
		            responseStructure.setMessage("Missing or invalid authorization token");
		            responseStructure.setStatus("error");
		            responseStructure.setCode(HttpStatus.UNAUTHORIZED.value());
		            responseStructure.setPlatform("Reddit");
		            responseStructure.setData(null);
		            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseStructure);
		        }

		        String jwtToken = token.substring(7); // remove "Bearer " prefix
		        
		        
		        
		        int userId = jwtUtilConfig.extractUserId(jwtToken);
		        QuantumShareUser user = userDao.fetchUser(userId);
		       
		        if (user == null) {
		        	
		            responseStructure.setMessage("User doesn't exist, please sign up");
		            responseStructure.setStatus("error");
		            responseStructure.setCode(HttpStatus.NOT_FOUND.value());
		            responseStructure.setPlatform("Reddit");
		            responseStructure.setData(null);
		            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseStructure);
		        }
		       
		        responseStructure = redditService.getAccessToken(code,user);

		        // Customize the response structure
		        if (responseStructure.getStatus().equals("success")) {
		            responseStructure.setMessage("Reddit connected successfully");
		            responseStructure.setCode(HttpStatus.OK.value());
		            responseStructure.setPlatform("Reddit");
		        }

		        return ResponseEntity.status(responseStructure.getCode()).body(responseStructure);
		    
		    } 
		 
		 @PostMapping("/refreshtoken")
		 public ResponseEntity<ResponseStructure<Map<String, String>>> refreshToken() {
			 String token = request.getHeader("Authorization");
		        ResponseStructure<Map<String, String>> responseStructure = new ResponseStructure<>();

		        System.out.println("Controller request 1");
		        
		        if (token == null || !token.startsWith("Bearer ")) {
		            responseStructure.setMessage("Missing or invalid authorization token");
		            responseStructure.setStatus("error");
		            responseStructure.setCode(HttpStatus.UNAUTHORIZED.value());
		            responseStructure.setPlatform("Reddit");
		            responseStructure.setData(null);
		            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseStructure);
		        }

		        String jwtToken = token.substring(7); // remove "Bearer " prefix
		        
		        
		        
		        int userId = jwtUtilConfig.extractUserId(jwtToken);
		        QuantumShareUser user = userDao.fetchUser(userId);
		       
		        if (user == null) {
		        	
		            responseStructure.setMessage("User doesn't exist, please sign up");
		            responseStructure.setStatus("error");
		            responseStructure.setCode(HttpStatus.NOT_FOUND.value());
		            responseStructure.setPlatform("Reddit");
		            responseStructure.setData(null);
		            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseStructure);
		        }

		     System.out.println("Controller request 2");

		     // Call the service method to check and refresh access token
		     ResponseEntity<ResponseStructure<Map<String, String>>> serviceResponse = redditService.checkAndRefreshAccessToken(user);

		     // Customize the response structure based on service response
		     if (serviceResponse.getBody().getStatus().equals("success")) {
		         responseStructure.setMessage("Reddit connected successfully");
		         responseStructure.setCode(HttpStatus.OK.value());
		         responseStructure.setPlatform("Reddit");
		     }

		     return ResponseEntity.status(serviceResponse.getBody().getCode()).body(serviceResponse.getBody());
		 }

}
