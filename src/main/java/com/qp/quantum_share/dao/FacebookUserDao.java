package com.qp.quantum_share.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.qp.quantum_share.dto.FaceBookUser;
import com.qp.quantum_share.repository.FacebookUserRepository;

@Component
public class FacebookUserDao {
	@Autowired
	FacebookUserRepository repository;

//	public String findLastUserId() {
//
//		FaceBookUser latestUser = repository.findTopByOrderByFbIdDesc();
//		if (latestUser != null) {
//			return latestUser.getFbId();
//		}
//		return null;
//	}

	public void saveUser(FaceBookUser user) {
		repository.save(user);
	}

	public FaceBookUser findById(int id) {
		return repository.findById(id).orElse(null);
	}

	public void deleteFbUser(FaceBookUser faceBookUser) {
		repository.delete(faceBookUser);
	}

}
