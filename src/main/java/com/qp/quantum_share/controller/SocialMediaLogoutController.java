package com.qp.quantum_share.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.qp.quantum_share.configuration.JwtUtilConfig;
import com.qp.quantum_share.dao.QuantumShareUserDao;
import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.response.ResponseStructure;
import com.qp.quantum_share.services.SocialMediaLogoutService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/quantum-share")
public class SocialMediaLogoutController {

	@Autowired
	HttpServletRequest request;

	@Autowired
	JwtUtilConfig jwtUtilConfig;

//	@Autowired
//	ResponseStructure<String> structure;

	@Autowired
	QuantumShareUserDao userDao;

	@Autowired
	SocialMediaLogoutService logoutService;

	@GetMapping("/disconnect/facebook")
	public ResponseEntity<ResponseStructure<String>> disconnectFacebook() {
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
		System.out.println("fb disconnect " + userId);

		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			ResponseStructure<String> structure = new ResponseStructure<String>();
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("user doesn't exists, please signup");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		return logoutService.disconnectFacebook(user);
	}

	@GetMapping("/disconnect/instagram")
	public ResponseEntity<ResponseStructure<String>> disconnectInstagram() {
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
		System.out.println("inst disconnect " + userId);

		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			ResponseStructure<String> structure = new ResponseStructure<String>();
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("user doesn't exists, please signup");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		return logoutService.disconnectInstagram(user);
	}

	// Telegram
	@GetMapping("/disconnect/telegram")
	public ResponseEntity<ResponseStructure<String>> disconnectTelegram() {
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
		System.out.println("tele disconnect " + userId);

		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			ResponseStructure<String> structure = new ResponseStructure<String>();
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("user doesn't exists, please signup");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		return logoutService.disconnectTelegram(user);
	}

	@GetMapping("/disconnect/linkedin")
	public ResponseEntity<ResponseStructure<String>> disconnectLinkedIn() {
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
		Integer userId = jwtUtilConfig.extractUserId(jwtToken);
		System.out.println("linkedin disconnect " + userId);

		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			ResponseStructure<String> structure = new ResponseStructure<String>();
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("user doesn't exists, please signup");
			structure.setStatus("error");
			structure.setData(null);
			structure.setPlatform("linkedin");
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		return logoutService.disconnectLinkedIn(user);
	}

	// Youtube
	@GetMapping("/disconnect/youtube")
	public ResponseEntity<ResponseStructure<String>> disconnectYoutube() {
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
		System.out.println("youtube disconnect " + userId);

		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			ResponseStructure<String> structure = new ResponseStructure<String>();

			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("user doesn't exists, please signup");
			structure.setStatus("error");
			structure.setData(null);
			structure.setPlatform("youtube");
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		return logoutService.disconnectYoutube(user);
	}
	
	//REDDIT DISCONNECT
		@GetMapping("/disconnect/reddit")
		public ResponseEntity<ResponseStructure<String>> disconnectRedditAccount() {
		    ResponseStructure<String> responseStructure = new ResponseStructure<>();
		    String token = request.getHeader("Authorization");

		    if (token == null || !token.startsWith("Bearer ")) {
		        responseStructure.setCode(HttpStatus.UNAUTHORIZED.value());
		        responseStructure.setMessage("Missing or invalid authorization token");
		        responseStructure.setStatus("error");
		        responseStructure.setPlatform(null);
		        responseStructure.setData(null);
		        return new ResponseEntity<>(responseStructure, HttpStatus.UNAUTHORIZED);
		    }

		    String jwtToken = token.substring(7); // remove "Bearer " prefix
		    int userId = jwtUtilConfig.extractUserId(jwtToken);
		    QuantumShareUser user = userDao.fetchUser(userId);

		    if (user == null) {
		        responseStructure.setCode(HttpStatus.NOT_FOUND.value());
		        responseStructure.setMessage("User doesn't exist, please sign up");
		        responseStructure.setStatus("error");
		        responseStructure.setData(null);
		        return new ResponseEntity<>(responseStructure, HttpStatus.NOT_FOUND);
		    }

		    // Call the service method to disconnect the Reddit account
		    return logoutService.disconnectRedditAccount(user);
		}
	
}
