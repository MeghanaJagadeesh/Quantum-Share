package com.qp.quantum_share.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.qp.quantum_share.configuration.JwtUtilConfig;
import com.qp.quantum_share.dao.QuantumShareUserDao;
import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.response.ResponseStructure;
import com.qp.quantum_share.services.QuantumShareUserService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/quantum-share/user")
public class QuantumShareUserController {

	@Autowired
	QuantumShareUserService quantumShareUserService;

	@Autowired
	HttpServletRequest request;
//
//	@Autowired
//	ResponseStructure<String> structure;

	@Autowired
	JwtUtilConfig jwtUtilConfig;

	@Autowired
	QuantumShareUserDao userDao;

	@PostMapping("/login")
	public ResponseEntity<ResponseStructure<String>> userLogin(@RequestParam String emph,
			@RequestParam String password) {
		return quantumShareUserService.login(emph, password);
	}

	@PostMapping("/signup")
	public ResponseEntity<ResponseStructure<String>> signup(@RequestBody QuantumShareUser userDto) {
		return quantumShareUserService.userSignUp(userDto);
	}

	@GetMapping("/google/verify/email")
	public ResponseEntity<String> emailVerify(@RequestParam String email) {
		return quantumShareUserService.emailVerify(email);
	}
	
	@PostMapping("/login/google/authentication")
	public ResponseEntity<ResponseStructure<String>> loginWithGoogle(@RequestBody QuantumShareUser userDto) {
		return quantumShareUserService.signInWithGoogle(userDto);
	}
	
	@PostMapping("/regenerate/password/google/auth")
	public ResponseEntity<ResponseStructure<String>> regeneratePassword(@RequestParam String password,@RequestParam String email) {
		return quantumShareUserService.regeneratePassword(password,email);
	}

//	@GetMapping("/access/remainingdays")
//	public ResponseEntity<PackageResponse> userRemainingDays(@RequestBody User user) {
//		return quantumShareUserService.calculateRemainingPackageDays(user);
//	}

	@GetMapping("/verify")
	public ResponseEntity<ResponseStructure<String>> verifyEmail(@RequestParam("token") String token) {
		return quantumShareUserService.verifyEmail(token);
	}

	@GetMapping("/verify/updated/email")
	public ResponseEntity<ResponseStructure<String>> verifyUpdatedEmail(@RequestParam("token") String token) {
		return quantumShareUserService.verifyUpdatedEmail(token);
	}

	@GetMapping("/account-overview")
	public ResponseEntity<ResponseStructure<String>> accountOverView() {
		String token = request.getHeader("Authorization");
		if (token == null || !token.startsWith("Bearer ")) {
			ResponseStructure<String> structure=new ResponseStructure<String>();
			structure.setCode(115);
			structure.setMessage("Missing or invalid authorization token");
			structure.setStatus("error");
			structure.setPlatform(null);
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.UNAUTHORIZED);
		}
		String jwtToken = token.substring(7); // remove "Bearer " prefix
		int userId = jwtUtilConfig.extractUserId(jwtToken);
		return quantumShareUserService.accountOverView(userId);
	}

	@PostMapping("/account-overview")
	public ResponseEntity<ResponseStructure<String>> accountOverView(@RequestParam(required = false) MultipartFile file,
			@RequestParam(required = false) String firstname, @RequestParam(required = false) String lastname,
			@RequestParam(required = false) String email, @RequestParam(required = false) Long phoneNo,
			@RequestParam(required = false) String company) {
		String token = request.getHeader("Authorization");
		if (token == null || !token.startsWith("Bearer ")) {
			ResponseStructure<String> structure=new ResponseStructure<String>();
			structure.setCode(115);
			structure.setMessage("Missing or invalid authorization token");
			structure.setStatus("error");
			structure.setPlatform(null);
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.UNAUTHORIZED);
		}
		String jwtToken = token.substring(7);
		int userId = jwtUtilConfig.extractUserId(jwtToken);
		return quantumShareUserService.accountOverView(userId, file, firstname, lastname, email, phoneNo, company,
				jwtToken);
	}

	@GetMapping("/info")
	public ResponseEntity<ResponseStructure<String>> fetchUserInfo() {
		String token = request.getHeader("Authorization");
		if (token == null || !token.startsWith("Bearer ")) {
			ResponseStructure<String> structure=new ResponseStructure<String>();
			structure.setCode(115);
			structure.setMessage("Missing or invalid authorization token");
			structure.setStatus("error");
			structure.setPlatform(null);
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.UNAUTHORIZED);
		}
		String jwtToken = token.substring(7); // remove "Bearer " prefix
		int userId = jwtUtilConfig.extractUserId(jwtToken);
		return quantumShareUserService.fetchUserInfo(userId);
	}

	@GetMapping("/forgot/password/request")
	public ResponseEntity<ResponseStructure<String>> forgetPassword(@RequestParam String email) {
		return quantumShareUserService.forgetPassword(email);
	}

	@PostMapping("/update/password/request")
	public ResponseEntity<ResponseStructure<String>> updatePassword(@RequestParam String password,
			@RequestParam("token") String token) {
		return quantumShareUserService.updatePassword(password, token);
	}

	@GetMapping("/test/session")
	public Map<String, Object> test() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("name", "meghana");
		map.put("company", "QP");
		map.put("id", "QSU24001");
		return map;
	}

	// connected platforms

//	@GetMapping("/connected/socialmedia/platforms")
//	public ResponseEntity<ResponseStructure<String>> ConnectedSocialMediaPlatform() {
//		String token = request.getHeader("Authorization");
//		if (token == null || !token.startsWith("Bearer ")) {
//			structure.setCode(115);
//			structure.setMessage("Missing or invalid authorization token");
//			structure.setStatus("error");
//			structure.setPlatform(null);
//			structure.setData(null);
//			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.UNAUTHORIZED);
//		}
//		String jwtToken = token.substring(7); // remove "Bearer " prefix
//		int userId = jwtUtilConfig.extractUserId(jwtToken);
//		System.out.println("fb connected "+userId);
//		
//		return quantumShareUserService.ConnectedSocialMediaPlatform(userId);
//	}

	@GetMapping("/connected/socialmedia/facebook")
	public ResponseEntity<ResponseStructure<String>> fetchConnectedFB() {
		String token = request.getHeader("Authorization");
		if (token == null || !token.startsWith("Bearer ")) {
			ResponseStructure<String> structure=new ResponseStructure<String>();
			structure.setCode(115);
			structure.setMessage("Missing or invalid authorization token");
			structure.setStatus("error");
			structure.setPlatform(null);
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.UNAUTHORIZED);
		}
		String jwtToken = token.substring(7); // remove "Bearer " prefix
		int userId = jwtUtilConfig.extractUserId(jwtToken);
		System.out.println("fb connected " + userId);

		return quantumShareUserService.fetchConnectedFb(userId);

	}

	@GetMapping("/connected/socialmedia/instagram")
	public ResponseEntity<ResponseStructure<String>> fetchConnectedinsta() {
		String token = request.getHeader("Authorization");
		if (token == null || !token.startsWith("Bearer ")) {
			ResponseStructure<String> structure=new ResponseStructure<String>();
			structure.setCode(115);
			structure.setMessage("Missing or invalid authorization token");
			structure.setStatus("error");
			structure.setPlatform(null);
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.UNAUTHORIZED);
		}
		String jwtToken = token.substring(7); // remove "Bearer " prefix
		int userId = jwtUtilConfig.extractUserId(jwtToken);
		System.out.println("insta connected " + userId);
		System.out.println();
		ResponseEntity<ResponseStructure<String>> response = quantumShareUserService.fetchConnectedInsta1(userId);
		System.out.println("insta response " + response);
		return response;

	}

	@GetMapping("/connected/socialmedia/telegram")
	public ResponseEntity<ResponseStructure<String>> fetchConnectedTelegram() {
		String token = request.getHeader("Authorization");
		if (token == null || !token.startsWith("Bearer ")) {
			ResponseStructure<String> structure=new ResponseStructure<String>();
			structure.setCode(115);
			structure.setMessage("Missing or invalid authorization token");
			structure.setStatus("error");
			structure.setPlatform(null);
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.UNAUTHORIZED);
		}
		String jwtToken = token.substring(7); // remove "Bearer " prefix
		int userId = jwtUtilConfig.extractUserId(jwtToken);
		System.out.println("telegram connected " + userId);

		ResponseEntity<ResponseStructure<String>> response = quantumShareUserService.fetchConnectedTelegram(userId);
		System.out.println("telegram + " + response);
		return response;
	}

	@GetMapping("/connected/socialmedia/linkedIn")
	public ResponseEntity<ResponseStructure<String>> fetchConnectedLinkedIn() {
		String token = request.getHeader("Authorization");
		if (token == null || !token.startsWith("Bearer ")) {
			ResponseStructure<String> structure=new ResponseStructure<String>();
			structure.setCode(115);
			structure.setMessage("Missing or invalid authorization token");
			structure.setStatus("error");
			structure.setPlatform(null);
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.UNAUTHORIZED);
		}
		String jwtToken = token.substring(7); // remove "Bearer " prefix
		int userId = jwtUtilConfig.extractUserId(jwtToken);
		System.out.println("linkedin connected " + userId);

		ResponseEntity<ResponseStructure<String>> response = quantumShareUserService.fetchLinkedIn(userId);
		System.out.println("linkedin : " + response);
		return response;
	}

	@GetMapping("/connected/socialmedia/youtube")
	public ResponseEntity<ResponseStructure<String>> fetchConnectedYoutube() {
		String token = request.getHeader("Authorization");
		if (token == null || !token.startsWith("Bearer ")) {
			ResponseStructure<String> structure=new ResponseStructure<String>();
			structure.setCode(115);
			structure.setMessage("Missing or invalid authorization token");
			structure.setStatus("error");
			structure.setPlatform(null);
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.UNAUTHORIZED);
		}
		String jwtToken = token.substring(7); // remove "Bearer " prefix
		int userId = jwtUtilConfig.extractUserId(jwtToken);
		System.out.println("youtube connected " + userId);

		ResponseEntity<ResponseStructure<String>> response = quantumShareUserService.fetchConnectedYoutube(userId);
		System.out.println("utube : " + response);
		return response;
		
	}

}
