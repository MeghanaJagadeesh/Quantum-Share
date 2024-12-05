package com.qp.quantum_share.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.qp.quantum_share.exception.JWTException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;

@Component
public class JwtUtilConfig {
	
	@Value("${jwt.secret}")
	private String secretKey;

	@SuppressWarnings("deprecation")
	public Claims extractAllClaims(String token) {
		return Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody();
	}

	public int extractUserId(String token) {
		try {
		return extractAllClaims(token).get("userId", Integer.class);
		}catch (ExpiredJwtException e) {
			throw new JWTException(e.getMessage());
		}
	}

	public String extractEmail(String token) {
		return extractAllClaims(token).get("email", String.class);
	}
}
