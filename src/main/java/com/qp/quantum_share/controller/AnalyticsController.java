package com.qp.quantum_share.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.qp.quantum_share.configuration.JwtUtilConfig;
import com.qp.quantum_share.dao.QuantumShareUserDao;
import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.helper.CommonMethod;
import com.qp.quantum_share.response.ResponseStructure;
import com.qp.quantum_share.services.AnalyticsPostService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("quatumshare/socialmedia")
public class AnalyticsController {

	@Autowired
	HttpServletRequest request;

//	@Autowired
//	ResponseStructure<String> structure;

	@Autowired
	JwtUtilConfig jwtUtilConfig;

	@Autowired
	QuantumShareUserDao userDao;

	@Autowired
	AnalyticsPostService analyticsPostService;
	
	@Autowired
	CommonMethod commonMethod;

	@GetMapping("/history")
	public ResponseEntity<ResponseStructure<String>> getPostHistory() {
		Object userId = commonMethod.validateToken(request.getHeader("Authorization"));
		QuantumShareUser user = userDao.fetchUser(Integer.parseInt(userId.toString()));
		return analyticsPostService.getHistory(user);
	}

	@GetMapping("/get/recent/post")
	public ResponseEntity<ResponseStructure<String>> getRecentPosts(@RequestParam(required = false) String postId) {
		Object userId = commonMethod.validateToken(request.getHeader("Authorization"));
		if (postId == null) {
			ResponseStructure<String> structure=new ResponseStructure<String>();
			structure.setCode(HttpStatus.BAD_REQUEST.value());
			structure.setMessage("Required PostId");
			structure.setStatus("error");
			structure.setPlatform(null);
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.BAD_REQUEST);
		}
		QuantumShareUser user = userDao.fetchUser(Integer.parseInt(userId.toString()));
		return analyticsPostService.getRecentPost(postId, user);
	}
	
	@GetMapping("/history/viewMore")
	public ResponseEntity<ResponseStructure<String>> getPostHistory20Images() {
		Object userId = commonMethod.validateToken(request.getHeader("Authorization"));
		QuantumShareUser user = userDao.fetchUser(Integer.parseInt(userId.toString()));
		return analyticsPostService.getHistory20Images(user);
	}
	
	@GetMapping("/view/analytics")
	public ResponseEntity<ResponseStructure<String>> viewAnalytics(@RequestParam String pid) {
		Object userId = commonMethod.validateToken(request.getHeader("Authorization"));
		if (pid == null) {
			ResponseStructure<String> structure=new ResponseStructure<String>();
			structure.setCode(HttpStatus.BAD_REQUEST.value());
			structure.setMessage("Required PostId");
			structure.setStatus("error");
			structure.setPlatform(null);
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.BAD_REQUEST);
		}
		QuantumShareUser user = userDao.fetchUser(Integer.parseInt(userId.toString()));
		return analyticsPostService.viewAnalytics(user,pid);
	}
}