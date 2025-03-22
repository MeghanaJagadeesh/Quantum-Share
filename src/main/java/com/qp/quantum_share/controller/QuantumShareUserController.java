package com.qp.quantum_share.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import com.qp.quantum_share.helper.CommonMethod;
import com.qp.quantum_share.helper.JwtToken;
import com.qp.quantum_share.repository.StaffRepository;
import com.qp.quantum_share.response.ResponseStructure;
import com.qp.quantum_share.services.AdminService;
import com.qp.quantum_share.services.QuantumShareUserService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/quantum-share/user")
public class QuantumShareUserController {

	@Autowired
	QuantumShareUserService quantumShareUserService;

	@Autowired
	HttpServletRequest request;

	@Autowired
	JwtUtilConfig jwtUtilConfig;

	@Autowired
	QuantumShareUserDao userDao;

	@Autowired
	CommonMethod commonMethod;
	
	@Autowired
	JwtToken jwtToken;
	
	@Autowired
	StaffRepository staffRepository;
	
	@Autowired
	AdminService adminService;

	@Value("${quantumshare.admin.email}")
	private String adminEmail;

	@Value("${quantumshare.admin.password}")
	private String adminPassword;
	
	@PostMapping("/login")
	public ResponseEntity<ResponseStructure<String>> userLogin(@RequestParam String emph,
			@RequestParam String password) {
		ResponseStructure<String> responseStructure = new ResponseStructure<>();

	    if (adminEmail.equals(emph) && adminPassword.equals(password)) {
	        String tokenValue = jwtToken.generateJWTForAdmin(); 
	        responseStructure.setMessage("Admin login successful!");
	        responseStructure.setStatus("success");
	        responseStructure.setCode(122);
	        responseStructure.setData(tokenValue);
	        return ResponseEntity.ok(responseStructure);
	    }
	    try {
	        return quantumShareUserService.login(emph, password); // Call user login service method
	    } catch (RuntimeException e) {
	        responseStructure.setMessage(e.getMessage());
	        responseStructure.setStatus("error");
	        responseStructure.setCode(HttpStatus.UNAUTHORIZED.value());
	        responseStructure.setData(null);
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseStructure);
	    }
	}

	@PostMapping("/signup")
	public ResponseEntity<ResponseStructure<String>> signup(@RequestBody QuantumShareUser userDto) {
		return quantumShareUserService.userSignUp(userDto);
	}

	@GetMapping("/google/verify/email")
	public ResponseEntity<ResponseStructure<String>> emailVerify(@RequestParam String email) {
		return quantumShareUserService.emailVerify(email);
	}

	@PostMapping("/login/google/authentication")
	public ResponseEntity<ResponseStructure<String>> loginWithGoogle(@RequestBody QuantumShareUser userDto) {
		return quantumShareUserService.signInWithGoogle(userDto);
	}

	@PostMapping("/regenerate/password/google/auth")
	public ResponseEntity<ResponseStructure<String>> regeneratePassword(@RequestParam String password,
			@RequestParam String email) {
		return quantumShareUserService.regeneratePassword(password, email);
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
		Object userId = commonMethod.validateToken(request.getHeader("Authorization"));
		return quantumShareUserService.accountOverView(Integer.parseInt(userId.toString()));

	}

	@PostMapping("/account-overview")
	public ResponseEntity<ResponseStructure<String>> accountOverView(@RequestParam(required = false) MultipartFile[] file,
			@RequestParam(required = false) String firstname, @RequestParam(required = false) String lastname,
			@RequestParam(required = false) String email, @RequestParam(required = false) Long phoneNo,
			@RequestParam(required = false) String company) {
		Object userId = commonMethod.validateToken(request.getHeader("Authorization"));
		return quantumShareUserService.accountOverView(Integer.parseInt(userId.toString()), file, firstname, lastname,
				email, phoneNo, company);
	}

	@GetMapping("/info")
	public ResponseEntity<ResponseStructure<String>> fetchUserInfo() {
		Object userId = commonMethod.validateToken(request.getHeader("Authorization"));
		return quantumShareUserService.fetchUserInfo(Integer.parseInt(userId.toString()));
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

	@GetMapping("/app/forgot/password/request")
	public ResponseEntity<ResponseStructure<String>> AppForgetPassword(@RequestParam String email) {
		return quantumShareUserService.AppForgetPassword(email);
	}

	@GetMapping("/test/session")
	public Map<String, Object> test() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("name", "meghana");
		map.put("company", "QP");
		map.put("id", "QSU24001");
		return map;
	}

	@GetMapping("/connected/socialmedia/platforms")
	public ResponseEntity<ResponseStructure<String>> fetchConnectedPlatform() {
		Object userId = commonMethod.validateToken(request.getHeader("Authorization"));
		return quantumShareUserService.fetchConnectedPlatform(Integer.parseInt(userId.toString()));
	}

	@GetMapping("/connected/socialmedia/facebook")
	public ResponseEntity<ResponseStructure<String>> fetchConnectedFB() {
		Object userId = commonMethod.validateToken(request.getHeader("Authorization"));
		return quantumShareUserService.fetchConnectedFb(Integer.parseInt(userId.toString()));
	}

	@GetMapping("/connected/socialmedia/instagram")
	public ResponseEntity<ResponseStructure<String>> fetchConnectedinsta() {
		Object userId = commonMethod.validateToken(request.getHeader("Authorization"));
		return quantumShareUserService.fetchConnectedInsta1(Integer.parseInt(userId.toString()));
	}

	@GetMapping("/connected/socialmedia/telegram")
	public ResponseEntity<ResponseStructure<String>> fetchConnectedTelegram() {
		Object userId = commonMethod.validateToken(request.getHeader("Authorization"));
		return quantumShareUserService.fetchConnectedTelegram(Integer.parseInt(userId.toString()));
	}

	@GetMapping("/connected/socialmedia/linkedIn")
	public ResponseEntity<ResponseStructure<String>> fetchConnectedLinkedIn() {
		Object userId = commonMethod.validateToken(request.getHeader("Authorization"));
		return quantumShareUserService.fetchLinkedIn(Integer.parseInt(userId.toString()));
	}

	@GetMapping("/connected/socialmedia/youtube")
	public ResponseEntity<ResponseStructure<String>> fetchConnectedYoutube() {
		Object userId = commonMethod.validateToken(request.getHeader("Authorization"));
		return quantumShareUserService.fetchConnectedYoutube(Integer.parseInt(userId.toString()));
	}

	@GetMapping("/connected/socialmedia/reddit")
	public ResponseEntity<ResponseStructure<String>> fetchConnectedReddit() {
		Object userId = commonMethod.validateToken(request.getHeader("Authorization"));
		return quantumShareUserService.fetchConnectedReddit(Integer.parseInt(userId.toString()));
	}

	@GetMapping("/connected/socialmedia/twitter")
	public ResponseEntity<ResponseStructure<String>> fetchConnectedTwitter() {
		Object userId = commonMethod.validateToken(request.getHeader("Authorization"));
		return quantumShareUserService.fetchConnectedTwitter(Integer.parseInt(userId.toString()));
	}
	
	@GetMapping("/connected/socialmedia/pinterest")
	public ResponseEntity<ResponseStructure<String>> fetchConnectedPinterest() {
		Object userId = commonMethod.validateToken(request.getHeader("Authorization"));
		return quantumShareUserService.fetchConnectedPinterest(Integer.parseInt(userId.toString()));
	}
}
