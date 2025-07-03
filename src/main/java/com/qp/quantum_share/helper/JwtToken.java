package com.qp.quantum_share.helper;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.dto.Staff;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtToken {

	@Value("${jwt.secret}")
	private String secretKey;
	
	@Value("${quantumshare.admin.email}") 
	private String adminEmail;
	
	@Autowired
	SecurePassword password;

	public String generateJWT(QuantumShareUser user) {
//		Map<String, Object> claims = new HashMap<>();
//		claims.put("userId", user.getUserId());
//		claims.put("email", user.getEmail());
//
//		// Set expiration time to a very large value or never expire
//		// In this example, expiration time is set to January 1, 3000
////		Date expirationDate = new Date(Long.MAX_VALUE);
//
//		
//		Calendar calendar = Calendar.getInstance();
//		calendar.add(Calendar.DAY_OF_YEAR, 15); 
//        Date expirationDate = calendar.getTime();
////        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes());
//        SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
//        String base64Key = Encoders.BASE64.encode(key.getEncoded()); 
//		return Jwts.builder().setClaims(claims).setExpiration(expirationDate)
//				.signWith(key).compact();
		return SecurePassword.encrypt(user.getUserId()+"", "123");
		
	}
	
		public String generateJWT(Staff staff) {
//		        Map<String, Object> claims = new HashMap<>();
//		        claims.put("staffId", staff.getStaffId()); 
//		        claims.put("email", staff.getEmail());
//		        claims.put("role", "staff");
//		        
//		        Calendar calendar = Calendar.getInstance();
//		        calendar.add(Calendar.DAY_OF_YEAR, 15); 
//		        Date expirationDate = calendar.getTime();
//		        
//		        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes());
//				return Jwts.builder().setClaims(claims).setExpiration(expirationDate)
//						.signWith(key).compact();
			return SecurePassword.encrypt(staff.getStaffId()+"", "123");
		    }
		  
		public String generateJWTForAdmin() {
//			    Map<String, Object> claims = new HashMap<>();
//			    claims.put("email", adminEmail);
//
//			    Calendar calendar = Calendar.getInstance();
//			    calendar.add(Calendar.DAY_OF_YEAR, 15);
//			    Date expirationDate = calendar.getTime();
//
//
//			    SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes());
//
//				return Jwts.builder().setClaims(claims).setExpiration(expirationDate)
//						.signWith(key).compact();
			return SecurePassword.encrypt(adminEmail, "123");
			}
}
