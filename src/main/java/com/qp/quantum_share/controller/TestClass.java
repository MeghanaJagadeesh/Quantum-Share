package com.qp.quantum_share.controller;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.qp.quantum_share.dao.QuantumShareUserDao;
import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.helper.CommonMethod;
import com.qp.quantum_share.helper.UTCTime;
import com.qp.quantum_share.response.ResponseStructure;
import com.qp.quantum_share.services.TestService;

@RestController
@RequestMapping("/test")
public class TestClass {
	@Autowired
	TestService service;

	@Autowired
	CommonMethod commonMethod;

	@Autowired
	QuantumShareUserDao userDao;

	@GetMapping("/date")
	public void convertdate(@RequestHeader("Authorization") String token) {
		Object userId = commonMethod.validateToken(token);
		QuantumShareUser user = userDao.fetchUser(Integer.parseInt(userId.toString()));

		service.fetchAnalytics(user);
	}
}
