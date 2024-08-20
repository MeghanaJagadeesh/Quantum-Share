package com.qp.quantum_share.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.qp.quantum_share.dto.InstagramUser;
import com.qp.quantum_share.repository.InstagramRepository;

@Component
public class InstagramUserDao {
	
	@Autowired
	InstagramRepository instagramRepository;
	
//	public String findLastUserId() {
//
//		InstagramUser latestUser = instagramRepository.findTopByOrderByInstaIdDesc();
//		if (latestUser != null) {
//			return latestUser.getInstaId();
//		}
//		return null;
//	}

	public void save(InstagramUser instagramUser) {
		instagramRepository.save(instagramUser);
		
	}

	public InstagramUser findById(int id) {
		return instagramRepository.findById(id).orElse(null);
	}
	
	public void deleteUser(InstagramUser user) {
		instagramRepository.delete(user);
	}

}
