package com.qp.quantum_share.services;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.qp.quantum_share.dao.AdminDao;
import com.qp.quantum_share.dto.CreditSystem;
import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.dto.Staff;
import com.qp.quantum_share.exception.CommonException;
import com.qp.quantum_share.helper.JwtToken;
import com.qp.quantum_share.repository.QuantumShareUserRespository;
import com.qp.quantum_share.repository.StaffRepository;
import com.qp.quantum_share.response.ResponseStructure;

import jakarta.transaction.Transactional;

@Service
public class AdminService {

	@Autowired
	QuantumShareUserRespository userRespository;

	@Autowired
	AdminDao adminDao;

	@Autowired
	JwtToken token;

	@Autowired
	private StaffRepository staffRepository;

	public ResponseEntity<ResponseStructure<String>> getUserCount() {
		ResponseStructure<String> responseStructure = new ResponseStructure<>();

		try {
			long count = adminDao.getUserCount();
			responseStructure.setMessage("User count fetched successfully");
			responseStructure.setStatus("success");
			responseStructure.setCode(HttpStatus.OK.value());
			responseStructure.setData(count);

			return ResponseEntity.ok(responseStructure);
		} catch (Exception e) {
			throw new CommonException(e.getMessage());
		}
	}

	public ResponseEntity<ResponseStructure<String>> deleteUserByEmail(String email) {
		ResponseStructure<String> responseStructure = new ResponseStructure<String>();
		try {
			adminDao.deleteByEmail(email);

			responseStructure.setMessage("User deleted successfully");
			responseStructure.setStatus("success");
			responseStructure.setCode(HttpStatus.OK.value());
			responseStructure.setData(null);
			return ResponseEntity.ok(responseStructure);
		} catch (RuntimeException e) {
			throw new CommonException(e.getMessage());
		}
	}

	public ResponseEntity<ResponseStructure<String>> increaseCredits(String email, int additionalCredits) {
		try {
			ResponseStructure<String> responseStructure = new ResponseStructure<String>();

			QuantumShareUser user = adminDao.findUserByEmail(email);
			CreditSystem credits = user.getCreditSystem();
			if (credits == null) {
				credits = new CreditSystem();
				user.setCreditSystem(credits);
			}
			credits.setTotalAppliedCredit(credits.getTotalAppliedCredit() + additionalCredits);
			credits.setCreditedDate(LocalDate.now());
			credits.setCreditedTime(LocalTime.now());
			credits.setRemainingCredit(credits.getRemainingCredit() + additionalCredits);
			adminDao.saveUser(user);

			responseStructure.setMessage("Credits increased successfully.");
			responseStructure.setStatus("success");
			responseStructure.setCode(HttpStatus.OK.value());
			responseStructure.setData("Added " + additionalCredits + " credits to " + email);
			return ResponseEntity.ok(responseStructure);
		} catch (Exception e) {
			throw new CommonException(e.getMessage());
		}
	}

	// ADD STAFF MEMBERS
	public ResponseEntity<ResponseStructure<String>> addStaff(Staff staff) {
		try {
			ResponseStructure<String> structure = new ResponseStructure<String>();
			if (staffRepository.existsByEmail(staff.getEmail())) {
				structure.setCode(HttpStatus.BAD_REQUEST.value());
				structure.setData(staff.getEmail());
				structure.setMessage("Staff member already exists.");
				structure.setPlatform(null);
				structure.setStatus("error");
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(structure);
			}
			staffRepository.save(staff);
			structure.setMessage("Staff member added successfully.");
			structure.setStatus("success");
			structure.setCode(HttpStatus.OK.value());
			structure.setData("Added staff member: " + staff.getName());
			return ResponseEntity.ok(structure);
		} catch (Exception e) {
			throw new CommonException(e.getMessage());
		}
	}

	public boolean isStaff(String email) {
		Staff staff = staffRepository.findByEmail(email);
		if (staff != null)
			return true;
		else
			return false;
	}

	// STAFF LOGIN
	public ResponseEntity<ResponseStructure<String>> staffLogin(String email, String password) {
		ResponseStructure<String> structure = new ResponseStructure<>();

		Staff staff = staffRepository.findByEmail(email);

		if (staff == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("Invalid email");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<>(structure, HttpStatus.NOT_FOUND);
		}
		if (staff.getPassword() == null || staff.getPassword().isEmpty()) {
			structure.setCode(120);
			structure.setMessage("Password is not set.");
			structure.setStatus("error");
			structure.setData(staff.getEmail());
			return new ResponseEntity<>(structure, HttpStatus.NOT_FOUND);
		}

		if (staff.getPassword().equals(password)) {
			String tokenValue = token.generateJWT(staff);
			structure.setCode(HttpStatus.OK.value());
			structure.setMessage("Staff Login Successful");
			structure.setStatus("success");
			structure.setData(tokenValue);
			return new ResponseEntity<>(structure, HttpStatus.OK);
		} else {
			structure.setCode(HttpStatus.NOT_ACCEPTABLE.value());
			structure.setMessage("Invalid Password");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<>(structure, HttpStatus.NOT_ACCEPTABLE);
		}
	}

	@Transactional
	public ResponseEntity<ResponseStructure<String>> removeStaffByEmail(String email) {
		try {
			ResponseStructure<String> structure = new ResponseStructure<String>();
			if (!staffRepository.existsByEmail(email)) {
				structure.setCode(HttpStatus.NOT_FOUND.value());
				structure.setData(email);
				structure.setMessage("Staff member not found.");
				structure.setPlatform(null);
				structure.setStatus("error");
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(structure);
			}
			staffRepository.deleteByEmail(email);
			structure.setMessage("Staff member deleted successfully.");
			structure.setStatus("success");
			structure.setCode(HttpStatus.OK.value());
			structure.setData("Deleted staff member id: " + email);
			return ResponseEntity.ok(structure);
		} catch (Exception e) {
			throw new CommonException(e.getMessage());
		}
	}
}
