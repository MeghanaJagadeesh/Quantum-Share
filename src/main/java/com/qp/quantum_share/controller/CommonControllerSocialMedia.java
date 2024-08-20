package com.qp.quantum_share.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.qp.quantum_share.configuration.JwtUtilConfig;
import com.qp.quantum_share.dao.QuantumShareUserDao;
import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.response.ResponseStructure;
import com.qp.quantum_share.services.TelegramService;

import jakarta.servlet.http.HttpServletRequest;

@RequestMapping("/quantum-share")
public class CommonControllerSocialMedia {

	@Autowired
	HttpServletRequest request;

	@Autowired
	ResponseStructure<String> structure;

	@Autowired
	JwtUtilConfig jwtUtilConfig;

	@Autowired
	QuantumShareUserDao userDao;
	
	@Autowired
	TelegramService telegramService;

	// Fetching Group Details
	@GetMapping("/telegram/user/groupDetails")
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

}
