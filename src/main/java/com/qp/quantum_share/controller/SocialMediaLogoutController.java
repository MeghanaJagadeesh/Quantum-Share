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
import com.qp.quantum_share.helper.CommonMethod;
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

	@Autowired
	CommonMethod commonMethod;	

	@GetMapping("/disconnect/facebook")
	public ResponseEntity<ResponseStructure<String>> disconnectFacebook() {
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
		return logoutService.disconnectFacebook(user);
	}

	@GetMapping("/disconnect/instagram")
	public ResponseEntity<ResponseStructure<String>> disconnectInstagram() {
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
		return logoutService.disconnectInstagram(user);
	}

	// Telegram
	@GetMapping("/disconnect/telegram")
	public ResponseEntity<ResponseStructure<String>> disconnectTelegram() {
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
		return logoutService.disconnectTelegram(user);
	}

	@GetMapping("/disconnect/linkedin")
	public ResponseEntity<ResponseStructure<String>> disconnectLinkedIn() {
		Object userId1 = commonMethod.validateToken(request.getHeader("Authorization"));
		int userId = Integer.parseInt(userId1.toString());
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

	@GetMapping("/disconnect/youtube")
	public ResponseEntity<ResponseStructure<String>> disconnectYoutube() {
		Object userId1 = commonMethod.validateToken(request.getHeader("Authorization"));
		int userId = Integer.parseInt(userId1.toString());
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

	@GetMapping("/disconnect/reddit")
	public ResponseEntity<ResponseStructure<String>> disconnectRedditAccount() {
		ResponseStructure<String> responseStructure = new ResponseStructure<>();
		Object userId1 = commonMethod.validateToken(request.getHeader("Authorization"));
		int userId = Integer.parseInt(userId1.toString());
		QuantumShareUser user = userDao.fetchUser(userId);

		if (user == null) {
			responseStructure.setCode(HttpStatus.NOT_FOUND.value());
			responseStructure.setMessage("User doesn't exist, please sign up");
			responseStructure.setStatus("error");
			responseStructure.setData(null);
			return new ResponseEntity<>(responseStructure, HttpStatus.NOT_FOUND);
		}
		return logoutService.disconnectRedditAccount(user);
	}
	
	@GetMapping("/disconnect/twitter")
	public ResponseEntity<ResponseStructure<String>> disconnectTwitterAccount() {
		ResponseStructure<String> responseStructure = new ResponseStructure<>();
		Object userId1 = commonMethod.validateToken(request.getHeader("Authorization"));
		int userId = Integer.parseInt(userId1.toString());
		QuantumShareUser user = userDao.fetchUser(userId);

		if (user == null) {
			responseStructure.setCode(HttpStatus.NOT_FOUND.value());
			responseStructure.setMessage("User doesn't exist, please sign up");
			responseStructure.setStatus("error");
			responseStructure.setData(null);
			return new ResponseEntity<>(responseStructure, HttpStatus.NOT_FOUND);
		}
		return logoutService.disconnectTwitterAccount(user);
	}
	
	
	@GetMapping("/disconnect/pinterest")
	public ResponseEntity<ResponseStructure<String>> disconnectPinterest() {
		Object userId1 = commonMethod.validateToken(request.getHeader("Authorization"));
		int userId = Integer.parseInt(userId1.toString());
		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			ResponseStructure<String> structure = new ResponseStructure<String>();

			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("user doesn't exists, please signup");
			structure.setStatus("error");
			structure.setData(null);
			structure.setPlatform("pinterest");
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		return logoutService.disconnectPinterest(user);
	}

}
