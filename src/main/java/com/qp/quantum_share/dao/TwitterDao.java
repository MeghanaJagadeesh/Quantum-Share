package com.qp.quantum_share.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.qp.quantum_share.dto.TwitterUser;
import com.qp.quantum_share.repository.TwitterRepository;

@Component
public class TwitterDao {

	@Autowired
	TwitterRepository twitterRepository;

	public TwitterUser findById(int tgId) {
		return twitterRepository.findById(tgId).orElse(null);
	}

	public void deleteUser(TwitterUser user) {
		twitterRepository.delete(user);
	}

}
