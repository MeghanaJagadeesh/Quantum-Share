package com.qp.quantum_share.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.qp.quantum_share.configuration.JwtUtilConfig;
import com.qp.quantum_share.helper.CommonMethod;
import com.qp.quantum_share.response.ResponseStructure;
import com.qp.quantum_share.services.PaymentService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/quantum-share/user")
public class PaymentController {

	@Autowired
	HttpServletRequest request;

//	@Autowired
//	ResponseStructure<String> structure;

	@Autowired
	JwtUtilConfig jwtUtilConfig;

	@Autowired
	PaymentService paymentService;
	
	@Autowired
	CommonMethod commonMethod;

	@GetMapping("/subscription/create/payment")
	public ResponseEntity<ResponseStructure<String>> getSubscription(@RequestParam double amount,
			@RequestParam String packageName) {
		Object userId = commonMethod.validateToken(request.getHeader("Authorization"));
		return paymentService.subscription(amount, Integer.parseInt(userId.toString()), packageName);
	}

	@PostMapping("/payment/callback/handle")
	public ResponseEntity<ResponseStructure<String>> handlePaymentCallback(@RequestParam double amount,
			@RequestParam String razorpay_payment_id, @RequestParam String razorpay_order_id,
			@RequestParam String razorpay_signature) {
		Object userId = commonMethod.validateToken(request.getHeader("Authorization"));
		if (razorpay_payment_id == null) {
			ResponseStructure<String> structure=new ResponseStructure<String>();
			structure.setCode(HttpStatus.UNAUTHORIZED.value());
			structure.setMessage("Missing Payment Id");
			structure.setStatus("error");
			structure.setPlatform(null);
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.UNAUTHORIZED);
		}
		return paymentService.handleCallbackPayment(amount, Integer.parseInt(userId.toString()), razorpay_order_id, razorpay_payment_id,
				razorpay_signature);
	}
}
