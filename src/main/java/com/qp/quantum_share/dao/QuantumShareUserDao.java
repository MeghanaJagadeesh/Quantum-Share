package com.qp.quantum_share.dao;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.repository.QuantumShareUserRespository;

@Component
public class QuantumShareUserDao {

	@Autowired
	QuantumShareUserRespository userRespository;

	public void saveUser(QuantumShareUser user) {
		userRespository.save(user);
	}

	public QuantumShareUser fetchUser(int userID) {
		return userRespository.findById(userID).get();
	}

	public List<QuantumShareUser> findByEmailOrPhoneNo(String email, long mobile) {
		return userRespository.findByEmailOrPhoneNo(email, mobile);
	}

//	public String findLastUserId() {
//		QuantumShareUser latestUser = userRespository.findTopByOrderByUserIdDesc();
//		if (latestUser != null) {
//			return latestUser.getUserId();
//		}
//		return null;
//	}

	public void save(QuantumShareUser user) {
		userRespository.save(user);

	}

	public QuantumShareUser findByVerificationToken(String token) {
		return userRespository.findByVerificationToken(token);
	}
}
