package com.qp.quantum_share.helper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.qp.quantum_share.configuration.JwtUtilConfig;
import com.qp.quantum_share.response.ResponseStructure;

@Service
public class CommonMethod {

	@Autowired
	JwtUtilConfig jwtUtilConfig;

	public Object validateToken(String token) {
		if (token == null || !token.startsWith("Bearer ")) {
			ResponseStructure<String> structure = new ResponseStructure<String>();
			structure.setCode(115);
			structure.setMessage("Missing or invalid authorization token");
			structure.setStatus("error");
			structure.setPlatform(null);
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.UNAUTHORIZED);
		}
		String jwtToken = token.substring(7);
		int userId = jwtUtilConfig.extractUserId(jwtToken);
		return userId;
	}
	

}
