package com.qp.quantum_share.dao;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.qp.quantum_share.dto.YoutubeUser;
import com.qp.quantum_share.repository.YoutubeRepository;

@Component
public class YoutubeUserDao {
	
	@Autowired
	YoutubeRepository youtubeRepository;

	public void deleteUser(YoutubeUser deleteUser) {
		youtubeRepository.delete(deleteUser);
	}

	public YoutubeUser findById(int youtubeId) {
		return youtubeRepository.findById(youtubeId).orElse(null);
	}

}