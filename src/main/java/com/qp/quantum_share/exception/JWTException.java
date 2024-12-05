package com.qp.quantum_share.exception;

public class JWTException extends RuntimeException {
	String message;

	public JWTException(String message) {

		this.message = message;
	}
}
