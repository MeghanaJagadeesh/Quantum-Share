package com.qp.quantum_share.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qp.quantum_share.configuration.JwtUtilConfig;
import com.qp.quantum_share.dao.QuantumShareUserDao;
import com.qp.quantum_share.dto.FaceBookUser;
import com.qp.quantum_share.dto.FacebookPageDetails;
import com.qp.quantum_share.dto.InstagramUser;
import com.qp.quantum_share.dto.LinkedInPageDto;
import com.qp.quantum_share.dto.LinkedInProfileDto;
import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.exception.CommonException;
import com.qp.quantum_share.helper.CommonMethod;
import com.qp.quantum_share.response.ResponseStructure;
import com.qp.quantum_share.services.FacebookAccessTokenService;
import com.qp.quantum_share.services.InstagramService;
import com.qp.quantum_share.services.LinkedInProfileService;
import com.qp.quantum_share.services.PinterestService;
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

	@Autowired
	PinterestService pinterestService;
	
	@Autowired
	ObjectMapper mapper;

	@Value("${linkedin.clientId}")
	private String clientId;

	@Value("${linkedin.redirectUri}")
	private String redirectUri;

	@Value("${linkedin.scope}")
	private String scope;

	@Value("${reddit.client_id}")
	private String reddit_clientId;

	@Value("${reddit.scope}")
	private String reddit_scope;

	@Value("${reddit.redirect_uri}")
	private String reddit_redirect_uri;

	@Autowired
	CommonMethod commonMethod;

	@GetMapping("/facebook/user/verify-token")
	public ResponseEntity<ResponseStructure<String>> callback(@RequestParam(required = false) String code) {
		Object userId1 = commonMethod.validateToken(request.getHeader("Authorization"));
		int userId = Integer.parseInt(userId1.toString());
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

	@PostMapping("/facebook/user/save/pages")
	public ResponseEntity<ResponseStructure<String>> saveSelectedFacebookUser(@RequestBody Map<String, Object> facebok)
			throws JsonMappingException, JsonProcessingException {
		ResponseStructure<String> structure = new ResponseStructure<String>();
		Object userId1 = commonMethod.validateToken(request.getHeader("Authorization"));
		int userId = Integer.parseInt(userId1.toString());
		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("user doesn't exists, please signup");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		FaceBookUser faceBookUser = mapper.convertValue(facebok.get("fbuser"), FaceBookUser.class);
		List<FacebookPageDetails> pageList = mapper.readValue(mapper.writeValueAsString(facebok.get("fbpages")),
				new TypeReference<List<FacebookPageDetails>>() {
				});
		System.out.println("user : " + faceBookUser);
		System.out.println("pages : " + pageList);
		return faceBookAccessTokenService.saveUser(faceBookUser, pageList, user);
	}

	// Instagram
	@GetMapping("/instagram/user/verify-token")
	public ResponseEntity<ResponseStructure<String>> callbackInsta(@RequestParam(required = false) String code) {
		ResponseStructure<String> structure = new ResponseStructure<String>();
		Object userId1 = commonMethod.validateToken(request.getHeader("Authorization"));
		int userId = Integer.parseInt(userId1.toString());
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
		return instagramService.verifyToken(code, user, userId);
	}

	@PostMapping("/instagram/user/save/profile")
	public ResponseEntity<ResponseStructure<String>> saveSelectedInstagramUser(@RequestBody InstagramUser instagramUser){
		ResponseStructure<String> structure = new ResponseStructure<String>();
		Object userId1 = commonMethod.validateToken(request.getHeader("Authorization"));
		int userId = Integer.parseInt(userId1.toString());
		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("user doesn't exists, please signup");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		return instagramService.saveInstaUser(instagramUser, user);
}

	// Twitter Connectivity
	@GetMapping("/twitter/user/connect")
	public ResponseEntity<ResponseStructure<String>> connectTwitter() {
		Object userId1 = commonMethod.validateToken(request.getHeader("Authorization"));
		int userId = Integer.parseInt(userId1.toString());
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
		Object userId1 = commonMethod.validateToken(request.getHeader("Authorization"));
		int userId = Integer.parseInt(userId1.toString());
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

	@GetMapping("/telegram/user/connect")
	public ResponseEntity<ResponseStructure<String>> connectTelegram() {
		Object userId1 = commonMethod.validateToken(request.getHeader("Authorization"));
		int userId = Integer.parseInt(userId1.toString());
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
		Object userId1 = commonMethod.validateToken(request.getHeader("Authorization"));
		int userId = Integer.parseInt(userId1.toString());
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

	@GetMapping("/connect-linkedin")
    public ResponseEntity<ResponseStructure<String>> login() {
		ResponseStructure<String> structure = new ResponseStructure<String>();
		String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            // User is not authenticated or authorized
            // Customize the error response
            structure.setCode(115);
            structure.setMessage("Missing or invalid authorization token");
            structure.setStatus("error");
            structure.setPlatform(null);
            structure.setData(null);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                 .body(structure);
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
		
        // User is authenticated and authorized
        // Generate the authorization URL and return a redirect response
        String authorizationUrl = linkedInProfileService.generateAuthorizationUrl();
        return ResponseEntity.status(HttpStatus.FOUND)
                             .header("Location", authorizationUrl)
                             .build();
    }
	
	@GetMapping("/linkedin/user/connect")
	public ResponseEntity<Map<String, String>> getLinkedInAuthUrl() {
		Map<String, String> authUrlParams = new HashMap<>();
		Object userId1 = commonMethod.validateToken(request.getHeader("Authorization"));
		int userId = Integer.parseInt(userId1.toString());
		QuantumShareUser user = userDao.fetchUser(userId);

		if (user == null) {
			authUrlParams.put("status", "error");
			authUrlParams.put("code", String.valueOf(HttpStatus.NOT_FOUND.value()));
			authUrlParams.put("message", "user doesn't exist, please sign up");
			authUrlParams.put("platform", null);
			authUrlParams.put("data", null);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(authUrlParams);
		}

		Map<String, String> authUrlParamsBody = linkedInProfileService.getLinkedInAuth().getBody();
		if (authUrlParamsBody != null) {
			authUrlParams.putAll(authUrlParamsBody);
		}
		authUrlParams.put("status", "success");
		return ResponseEntity.ok(authUrlParams);
	}

	@PostMapping("/linkedin/callback/success")
	public ResponseEntity<?> callbackEndpoint(@RequestParam("code") String code) {
		ResponseStructure<String> structure = new ResponseStructure<String>();
		try {
			Object userId1 = commonMethod.validateToken(request.getHeader("Authorization"));
			int userId = Integer.parseInt(userId1.toString());
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
			return linkedInProfileService.getPagesAndProfile(code, user, userId);
		} catch (Exception e) {
			throw new CommonException(e.getMessage());
		}
	}

//	private ResponseStructure<?> createErrorStructure(HttpStatus status, String message) {
//		ResponseStructure<?> structure = new ResponseStructure<>();
//		structure.setCode(status.value());
//		structure.setMessage(message);
//		structure.setStatus("error");
//		structure.setPlatform("linkedin");
//		structure.setData(null);
//		return structure;
//	}

	@PostMapping("linkedIn/selected/page")
	public ResponseEntity<Object> saveSelectedPage(@RequestBody Map<String, Object> linkedinPageData,
			@RequestParam("type") String type) {
		ResponseStructure<String> structure = new ResponseStructure<>();
		Object userId1 = commonMethod.validateToken(request.getHeader("Authorization"));
		int userId = Integer.parseInt(userId1.toString());
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
			System.out.println("urn " + linkedinPageData.get("urn").toString());
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
			System.out.println("page");
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
		Object userId1 = commonMethod.validateToken(request.getHeader("Authorization"));
		int userId = Integer.parseInt(userId1.toString());
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
		Object userId1 = commonMethod.validateToken(request.getHeader("Authorization"));
		int userId = Integer.parseInt(userId1.toString());
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
		Map<String, String> authUrlParams = new HashMap<>();
		Object userId1 = commonMethod.validateToken(request.getHeader("Authorization"));
		int userId = Integer.parseInt(userId1.toString());
		QuantumShareUser user = userDao.fetchUser(userId);

		if (user == null) {
			authUrlParams.put("status", "error");
			authUrlParams.put("code", String.valueOf(HttpStatus.NOT_FOUND.value()));
			authUrlParams.put("message", "User doesn't exist, please sign up");
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(authUrlParams);
		}
		authUrlParams.put("client_id", reddit_clientId);
		authUrlParams.put("redirect_uri", reddit_redirect_uri);
		authUrlParams.put("scope", reddit_scope);
		authUrlParams.put("status", "success");
		System.out.println(authUrlParams);
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
		ResponseStructure<Map<String, String>> responseStructure = new ResponseStructure<>();
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

		responseStructure = redditService.getAccessToken(code, user);

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
		ResponseStructure<Map<String, String>> responseStructure = new ResponseStructure<>();
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
		responseStructure = redditService.getAccessToken(code, user);
		if (responseStructure.getStatus().equals("success")) {
			responseStructure.setMessage("Reddit connected successfully");
			responseStructure.setCode(HttpStatus.OK.value());
			responseStructure.setPlatform("Reddit");
		}
		return ResponseEntity.status(responseStructure.getCode()).body(responseStructure);
	}

	
	// Pinterest Connection
	@GetMapping("/pinterest/user/connect")
	public ResponseEntity<ResponseStructure<String>> connectPinterest() {
		Object userId1 = commonMethod.validateToken(request.getHeader("Authorization"));
		int userId = Integer.parseInt(userId1.toString());
		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			ResponseStructure<String> structure = new ResponseStructure<String>();
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("User doesn't exists, Please Signup");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		return pinterestService.getPinterestAuthorizationUrl(user);
	}

	@PostMapping("/pinterest/user/verify-token")
	public ResponseEntity<ResponseStructure<String>> callbackPinterest(@RequestParam(required = false) String code) {
		Object userId1 = commonMethod.validateToken(request.getHeader("Authorization"));
		int userId = Integer.parseInt(userId1.toString());
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
			structure.setPlatform("pinterest");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.BAD_REQUEST);
		}
		return pinterestService.pinterestVerifyToken(code, user, userId);
	}

	
//	@PostMapping("/refresh-token")
//	public ResponseEntity<ResponseStructure<Map<String, String>>> refreshToken() {
//		ResponseStructure<Map<String, String>> responseStructure = new ResponseStructure<>();
//		Object userId1 = commonMethod.validateToken(request.getHeader("Authorization"));
//		int userId = Integer.parseInt(userId1.toString());
//		QuantumShareUser user = userDao.fetchUser(userId);
//		if (user == null) {
//
//			responseStructure.setMessage("User doesn't exist, please sign up");
//			responseStructure.setStatus("error");
//			responseStructure.setCode(HttpStatus.NOT_FOUND.value());
//			responseStructure.setPlatform("Reddit");
//			responseStructure.setData(null);
//			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseStructure);
//		}
//		ResponseEntity<ResponseStructure<Map<String, String>>> serviceResponse = redditService
//				.checkAndRefreshAccessToken(user);
//		if (serviceResponse.getBody().getStatus().equals("success")) {
//			responseStructure.setMessage("Reddit connected successfully");
//			responseStructure.setCode(HttpStatus.OK.value());
//			responseStructure.setPlatform("Reddit");
//		}
//		return ResponseEntity.status(serviceResponse.getBody().getCode()).body(serviceResponse.getBody());
//	}

}
