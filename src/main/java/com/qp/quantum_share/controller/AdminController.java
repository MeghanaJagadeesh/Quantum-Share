package com.qp.quantum_share.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.qp.quantum_share.dto.Staff;
import com.qp.quantum_share.helper.CommonMethod;
import com.qp.quantum_share.response.ResponseStructure;
import com.qp.quantum_share.services.AdminService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/quantumshare/admin")
public class AdminController {

	@Autowired
	AdminService adminService;

	@Autowired
	Staff staff;

	@Autowired
	HttpServletRequest request;

	@Autowired
	CommonMethod commonMethod;

	@Value("${quantumshare.admin.email}")
	private String adminEmail;

	@Value("${quantumshare.admin.password}")
	private String adminPassword;

	// Fetching Total User Count
	@GetMapping("/userCount")
	public ResponseEntity<ResponseStructure<String>> getUserCount() {
		ResponseStructure<String> structure = new ResponseStructure<String>();
		Object mail = commonMethod.validateAdminToken(request.getHeader("Authorization"));
		if (mail.equals(adminEmail)) {
			return adminService.getUserCount();
		} else {
			structure.setCode(HttpStatus.UNAUTHORIZED.value());
			structure.setData(null);
			structure.setMessage("invalid admin credentials");
			structure.setPlatform(null);
			structure.setStatus("error");
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.UNAUTHORIZED);
		}
	}

	@PostMapping("/increaseCredits")
	public ResponseEntity<ResponseStructure<String>> increaseCredits(@RequestParam String email,
			@RequestParam int additionalCredits) {
		ResponseStructure<String> structure = new ResponseStructure<String>();
		Object mail = commonMethod.validateAdminToken(request.getHeader("Authorization"));
		if (mail.equals(adminEmail)) {
			return adminService.increaseCredits(email, additionalCredits);
		} else {
			structure.setCode(HttpStatus.UNAUTHORIZED.value());
			structure.setData(null);
			structure.setMessage("invalid admin credentials");
			structure.setPlatform(null);
			structure.setStatus("error");
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.UNAUTHORIZED);
		}
	}

	// Deleting the user by email
	@DeleteMapping("/deleteUser")
	public ResponseEntity<ResponseStructure<String>> deleteUser(@RequestParam String email) {
		System.out.println("service");
		ResponseStructure<String> structure = new ResponseStructure<String>();
		Object mail = commonMethod.validateAdminToken(request.getHeader("Authorization"));
		System.out.println(mail);
		if (mail.equals(adminEmail)) {
			return adminService.deleteUserByEmail(email);
		} else {
			structure.setCode(HttpStatus.UNAUTHORIZED.value());
			structure.setData(null);
			structure.setMessage("invalid admin credentials");
			structure.setPlatform(null);
			structure.setStatus("error");
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.UNAUTHORIZED);
		}
	}

	// ADDING THE STAFF MEMBER
	@PostMapping("/addStaff")
	public ResponseEntity<ResponseStructure<String>> addStaff(@RequestBody Staff staff) {
		ResponseStructure<String> structure = new ResponseStructure<String>();
		Object mail = commonMethod.validateAdminToken(request.getHeader("Authorization"));
		if (mail.equals(adminEmail)) {
			return adminService.addStaff(staff);
		} else {
			structure.setCode(HttpStatus.UNAUTHORIZED.value());
			structure.setData(null);
			structure.setMessage("invalid admin credentials");
			structure.setPlatform(null);
			structure.setStatus("error");
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.UNAUTHORIZED);
		}
	}

	@DeleteMapping("/removeStaff")
	public ResponseEntity<ResponseStructure<String>> removeStaff(@RequestParam String email) {
		ResponseStructure<String> structure = new ResponseStructure<String>();
		Object mail = commonMethod.validateAdminToken(request.getHeader("Authorization"));
		if (mail.equals(adminEmail)) {
			return adminService.removeStaffByEmail(email);
		} else {
			structure.setCode(HttpStatus.UNAUTHORIZED.value());
			structure.setData(null);
			structure.setMessage("invalid admin credentials");
			structure.setPlatform(null);
			structure.setStatus("error");
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.UNAUTHORIZED);
		}
	}
}