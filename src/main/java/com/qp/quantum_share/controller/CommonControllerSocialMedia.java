package com.qp.quantum_share.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.qp.quantum_share.configuration.JwtUtilConfig;
import com.qp.quantum_share.dao.QuantumShareUserDao;
import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.helper.CommonMethod;
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
	
	@Autowired
	CommonMethod commonMethod;

	// Fetching Group Details
	@GetMapping("/telegram/user/groupDetails")
	public ResponseEntity<ResponseStructure<String>> getGroupDetails() {
		String userId = commonMethod.validateToken(request.getHeader("Authorization")).toString();
		QuantumShareUser user = userDao.fetchUser(Integer.parseInt(userId));
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("User doesn't exists, Please Signup");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		return telegramService.pollTelegramUpdates(user,Integer.parseInt(userId));
	}

}
