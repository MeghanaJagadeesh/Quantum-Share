package com.qp.quantum_share.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.qp.quantum_share.configuration.JwtUtilConfig;
import com.qp.quantum_share.dao.QuantumShareUserDao;
import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.helper.FileConvertion;
import com.qp.quantum_share.helper.PostOnServer;
import com.qp.quantum_share.helper.SecurePassword;
import com.qp.quantum_share.response.ResponseStructure;
import com.qp.quantum_share.services.TestService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping
public class TestClass {

	@Autowired
	HttpServletRequest request;

	@Autowired
	JwtUtilConfig jwtUtilConfig;

	@Autowired
	QuantumShareUserDao linkedinProfileService;

	@Autowired
	TestService testService;

	@Autowired
	ResponseStructure<String> structure;

	@Autowired
	FileConvertion conversion;
	
	@Autowired 
	PostOnServer postOnServer;
	
	@Autowired
	QuantumShareUserDao userDao;

	@GetMapping("/test")
	public String test(@RequestParam String email) {
		QuantumShareUser user = userDao.findByEmail(email);
		return SecurePassword.decrypt(user.getPassword(), "123");
	}

}
