package com.qp.quantum_share.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
@RequestMapping("/api/whatsapp")
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
	
	@Autowired
	TestService service;

	@GetMapping("/test")
	public String test(@RequestParam String email) {
		QuantumShareUser user = userDao.findByEmail(email);
		return SecurePassword.decrypt(user.getPassword(), "123");
	}

	@PostMapping("/sendBulk")
	public String sendBulkMessages(@RequestBody List<String> phoneNumbers) {
		service.sendBulkMessages(phoneNumbers);
		return "Messages sent successfully!";
	}
}
