package com.qp.quantum_share.controller;

import java.util.List;

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
import com.qp.quantum_share.services.HashtagService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/quantum-share")
public class HashtagController {
	
	@Autowired
	HashtagService hashtagService;

	@Autowired
	HttpServletRequest request;

	@Autowired
	JwtUtilConfig jwtUtilConfig;

	@Autowired
	QuantumShareUserDao userDao;
	
	@Autowired
	CommonMethod commonMethod;

	@GetMapping("/Hashtag-suggestions")
	public ResponseEntity<ResponseStructure<String>> getHashtagSuggestions(@RequestParam String query) {
		ResponseStructure<String> structure = new ResponseStructure<String>();
		Object userId = commonMethod.validateToken(request.getHeader("Authorization"));
		QuantumShareUser user = userDao.fetchUser(Integer.parseInt(userId.toString()));
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("user doesn't exists, please signup");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}

		List<String> hashtags = hashtagService.getHashtagSuggestions(query); // Call the service method to get filtered
																				// hashtags

		if (hashtags != null && !hashtags.isEmpty()) {
			structure.setCode(HttpStatus.OK.value());
			structure.setMessage(null);
			structure.setStatus("sucess");
			structure.setData(hashtags);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
		} else {
			structure.setCode(HttpStatus.OK.value());
			structure.setMessage("No hashtag found");
			structure.setStatus(null);
			structure.setData(hashtags);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
		}
	}

}
