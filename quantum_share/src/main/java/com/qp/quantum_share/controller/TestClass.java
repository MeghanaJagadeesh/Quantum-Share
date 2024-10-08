package com.qp.quantum_share.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.qp.quantum_share.configuration.JwtUtilConfig;
import com.qp.quantum_share.dao.QuantumShareUserDao;
import com.qp.quantum_share.response.ResponseStructure;
import com.qp.quantum_share.services.TestService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/quatumshare")
public class TestClass {

	@Autowired
	HttpServletRequest request;

	@Autowired
	JwtUtilConfig jwtUtilConfig;

	@Autowired
	QuantumShareUserDao userDao;
	
	@Autowired
	TestService testService;
	
	@Autowired
	ResponseStructure<String> structure;
	
//	@PostMapping("/post")
//	public String postVideo(MultipartFile mediaFile, @RequestParam String caption) throws IOException {
//		String token = request.getHeader("Authorization");
//		System.out.println("controller");
//		System.err.println(mediaFile.getContentType()+"  "+caption);
//		String jwtToken = token.substring(7); // remove "Bearer " prefix
//		int userId = jwtUtilConfig.extractUserId(jwtToken);
//		QuantumShareUser user = userDao.fetchUser(userId);
//		FacebookPageDetails page = user.getSocialAccounts().getFacebookUser().getPageDetails().get(0);
//		return testService.postVideoToPage(page,mediaFile,caption);
//	}
//
//	@GetMapping("/fetch/all/post")
//	public ResponseEntity<ResponseStructure<String>> fetchPost(@RequestParam String platform) {
////		System.out.println(pid);
//		String token = request.getHeader("Authorization");
//		if (token == null || !token.startsWith("Bearer ")) {
//			structure.setCode(115);
//			structure.setMessage("Missing or invalid authorization token");
//			structure.setStatus("error");
//			structure.setPlatform(null);
//			structure.setData(null);
//			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.UNAUTHORIZED);
//		}
//		if (platform == null) {
//			structure.setCode(HttpStatus.BAD_REQUEST.value());
//			structure.setMessage("Required platform name");
//			structure.setStatus("error");
//			structure.setPlatform(null);
//			structure.setData(null);
//			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.BAD_REQUEST);
//		}
//		String jwtToken = token.substring(7); // remove "Bearer " prefix
//		int userId = jwtUtilConfig.extractUserId(jwtToken);
//		QuantumShareUser user = userDao.fetchUser(userId);
//		return testService.fetchPosts(user,platform);
//	}
	
	@GetMapping("/check")
	public String test(MultipartFile file) {
		System.out.println(file.getContentType());
		return file.getContentType();
	}
}
