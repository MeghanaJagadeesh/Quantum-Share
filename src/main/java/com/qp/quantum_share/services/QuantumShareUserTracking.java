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
		} else if (user.getSubscriptionDetails().isSubscribed()) {
			map.put("validcredit", true);
			return map;
		} else {
			map.put("validcredit", false);
			map.put("message", "Package has been expired!! Please Subscribe Your package.");
			return map;
		}
	}

	
	public void applyCredit(QuantumShareUser user) {
		CreditSystem credits = user.getCreditSystem();
		if(credits!=null) {
			LocalDate creditedDate = credits.getCreditedDate();
			if(creditedDate.isBefore(LocalDate.now())) {
				credits.setTotalAppliedCredit(freeCredit);
				credits.setCreditedDate(LocalDate.now());
				credits.setCreditedTime(LocalTime.now());
				credits.setRemainingCredit(freeCredit);
				user.setCreditSystem(credits);
				userDao.save(user);
			}
		}
	}
}
