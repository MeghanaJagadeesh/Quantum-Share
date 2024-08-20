package com.qp.quantum_share.services;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.qp.quantum_share.configuration.ConfigurationClass;
import com.qp.quantum_share.dao.QuantumShareUserDao;
import com.qp.quantum_share.dto.PaymentDetails;
import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.dto.SubscriptionDetails;
import com.qp.quantum_share.exception.CommonException;
import com.qp.quantum_share.response.ResponseStructure;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

@Service
public class PaymentService {

	@Autowired
	QuantumShareUserDao userDao;

	@Autowired
	ResponseStructure<String> structure;

	@Autowired
	SubscriptionDetails subscriptionDetails;

	@Autowired
	ConfigurationClass configure;

	@Value("${quantumshare.standardDays}")
	private int standardDays;
	
	@Value("${quantumshare.razorpay.key_id}")
	private String key_id;
	
	@Value("${quantumshare.razorpay.key_secret}")
	private String key_secret;

	public ResponseEntity<ResponseStructure<String>> subscription(double amount, int userId, String packageName) {
		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("user doesn't exists, please login");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		try {
			RazorpayClient client = new RazorpayClient(key_id, key_secret);
			JSONObject json = new JSONObject();
			json.put("amount", amount * 100);
			json.put("currency", "INR");

			Order order = client.orders.create(json);
			if (user.getSubscriptionDetails() == null) {
				subscriptionDetails.setNameOfPackage(packageName);
				subscriptionDetails.setPackageAmount(amount);
				if (packageName.equals("standard"))
					subscriptionDetails.setSubscriptiondays(standardDays);
				user.setSubscriptionDetails(subscriptionDetails);
				userDao.save(user);
			} else {
				SubscriptionDetails subscription = user.getSubscriptionDetails();
				subscription.setPackageAmount(amount);
				subscription.setNameOfPackage(packageName);
				if (packageName.equals("standard"))
					subscriptionDetails.setSubscriptiondays(standardDays);
				
				user.setSubscriptionDetails(subscription);
				userDao.save(user);
			}

			Map<String, Object> map = configure.getMap();
			map.put("key", key_id);
			map.put("amount", amount);
			map.put("currency", "INR");
			map.put("name", "Quantum Share");
			map.put("image", "https://quantumshare.quantumparadigm.in/vedio/Quantum_Share_Logo%20(2).png");
			map.put("order_id", order.get("id"));

			Map<String, Object> userMap = configure.getMap();
			userMap.put("name", user.getFirstName() + " " + user.getLastName());
			userMap.put("email", user.getEmail());
			userMap.put("contact", user.getPhoneNo());
			userMap.put("address", user.getCompany());

			Map<String, Object> createPayment = configure.getMap();
			createPayment.put("payment", map);
			createPayment.put("user", userMap);

			structure.setCode(HttpStatus.CREATED.value());
			structure.setMessage(null);
			structure.setPlatform(null);
			structure.setStatus("created");
			structure.setData(createPayment);

			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.CREATED);
		} catch (RazorpayException e) {
			throw new CommonException(e.getMessage());
		}
	}

	public ResponseEntity<ResponseStructure<String>> handleCallbackPayment(double amount, int userId,
			String razorpay_order_id, String razorpay_payment_id, String razorpay_signature) {
		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("user doesn't exists, please signup");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		SubscriptionDetails subscription = user.getSubscriptionDetails();
		if (subscription != null) {
			List<PaymentDetails> paymentList = user.getSubscriptionDetails().getPayments();
			if (paymentList.isEmpty() || paymentList == null) {
				paymentList = configure.getPaymentList();
			}
			subscription.setSubscribed(true);
			subscription.setSubscriptionDate(LocalDate.now());

			PaymentDetails paymentDetails = configure.paymentDetails();
			paymentDetails.setOrder_id(razorpay_order_id);
			paymentDetails.setPayment_id(razorpay_payment_id);
			paymentDetails.setSignature(razorpay_signature);
			paymentDetails.setPaymentStatus(true);
			paymentDetails.setAmount(amount);
			paymentDetails.setPaymentDate(LocalDate.now());

			paymentList.add(paymentDetails);
			subscription.setPayments(paymentList);
			user.setSubscriptionDetails(subscription);
			userDao.save(user);

			Map<String, Object> map = configure.getMap();
			map.put("username", user.getFirstName() + " " + user.getLastName());
			map.put("phone", user.getPhoneNo());
			map.put("email", user.getEmail());
			map.put("company", user.getCompany());
			map.put("subscription", paymentDetails);

			structure.setCode(HttpStatus.OK.value());
			structure.setData(map);
			structure.setPlatform(null);
			structure.setStatus("success");
			structure.setMessage("Payment Successful");

			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
		}
		return null;
	}
}
