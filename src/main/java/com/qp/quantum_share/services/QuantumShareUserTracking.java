package com.qp.quantum_share.services;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.qp.quantum_share.configuration.ConfigurationClass;
import com.qp.quantum_share.dao.QuantumShareUserDao;
import com.qp.quantum_share.dto.CreditSystem;
import com.qp.quantum_share.dto.QuantumShareUser;

@Service
public class QuantumShareUserTracking {

	@Autowired
	ConfigurationClass configure;

	@Value("${quantumshare.freeCredit}")
	private int freeCredit;

	@Value("${quantumshare.subscribeCredit}")
	private int subscribeCredit;

	@Autowired
	QuantumShareUserDao userDao;

	public Map<String, Object> isValidCredit(QuantumShareUser user) {
		Map<String, Object> map = configure.getMap();
		if (user.isTrial()) {
			CreditSystem credits = user.getCreditSystem();
			if (credits.getRemainingCredit() <= 0) {
				map.put("validcredit", false);
				map.put("message", "credit depleted. Please upgrade to a subscription.");
				return map;
			} else {
				map.put("validcredit", true);
				return map;
			}
		} else if (user.getSubscriptionDetails()!=null&&user.getSubscriptionDetails().isSubscribed()) {
			map.put("validcredit", true);
			return map;
		} else {
			map.put("validcredit", false);
			map.put("message", "Package has been expired!! Please Subscribe Your package.");
			return map;
		}
	}

	public boolean applyCredit(QuantumShareUser user) {
		CreditSystem credits = user.getCreditSystem();
		boolean result = false;

		if (credits == null) {
			return false;
		}
		LocalDate creditedDate = credits.getCreditedDate();
		if (user.isTrial()) {
			System.out.println("User is in trial mode.");
			if (creditedDate.isBefore(LocalDate.now())) {
				credits.setTotalAppliedCredit(freeCredit);
				credits.setCreditedDate(LocalDate.now());
				credits.setCreditedTime(LocalTime.now());
				credits.setRemainingCredit(freeCredit);
				user.setCreditSystem(credits);
				userDao.save(user);
			}
			result = true;
		} else if (user.getSubscriptionDetails() != null && user.getSubscriptionDetails().isSubscribed()) {
			System.out.println("User is subscribed.");
			if (creditedDate.isBefore(LocalDate.now())) {
				credits.setTotalAppliedCredit(subscribeCredit);
				credits.setCreditedDate(LocalDate.now());
				credits.setCreditedTime(LocalTime.now());
				credits.setRemainingCredit(subscribeCredit);
				user.setCreditSystem(credits);
				userDao.save(user);
			}
			result = true;
		} else {
			System.out.println("User is neither in trial mode nor subscribed.");
			result = false;
		}

		System.out.println("Final result: " + result);
		return result;
	}

}
