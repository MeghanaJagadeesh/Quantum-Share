package com.qp.quantum_share.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.repository.QuantumShareUserRespository;

@Component
public class AdminDao {

	@Autowired
	QuantumShareUserRespository userRespository;
	
	public long getUserCount() {
		return userRespository.countUsers();
	}
	
	 public void deleteByEmail(String email) {
	        QuantumShareUser existingUser = userRespository.findByEmail(email);
	        if (existingUser == null) {
	            throw new RuntimeException("User not found with email: " + email);
	        }
	        userRespository.delete(existingUser);
	    }
	
	 public QuantumShareUser findUserByEmail(String email) {
		    QuantumShareUser user = userRespository.findByEmail(email); 
		    if (user == null) {
		        throw new RuntimeException("User not found with email: " + email); 
		    }
		    return user; 
		}


	 
	 public void saveUser(QuantumShareUser user) {
			userRespository.save(user);
		}
}
