package com.qp.quantum_share.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.qp.quantum_share.configuration.JwtUtilConfig;
import com.qp.quantum_share.dao.QuantumShareUserDao;
import com.qp.quantum_share.dto.FacebookPageDetails;
import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.services.TestService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/test")
public class TestClass {

	@Autowired
	HttpServletRequest request;

	@Autowired
	JwtUtilConfig jwtUtilConfig;

	@Autowired
	QuantumShareUserDao userDao;
	
	@Autowired
	TestService testService;
	
	@PostMapping("/post")
	public String postVideo(MultipartFile mediaFile, @RequestParam String caption) throws IOException {
		String token = request.getHeader("Authorization");
		System.out.println("controller");
		System.err.println(mediaFile.getContentType()+"  "+caption);
		String jwtToken = token.substring(7); // remove "Bearer " prefix
		int userId = jwtUtilConfig.extractUserId(jwtToken);
		QuantumShareUser user = userDao.fetchUser(userId);
		FacebookPageDetails page = user.getSocialAccounts().getFacebookUser().getPageDetails().get(0);
		return testService.postVideoToPage(page,mediaFile,caption);
	}

}
