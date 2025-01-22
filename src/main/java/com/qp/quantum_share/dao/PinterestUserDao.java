package com.qp.quantum_share.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.qp.quantum_share.dto.PinterestUser;
import com.qp.quantum_share.repository.PinterestRepository;

@Component
public class PinterestUserDao {
	
	@Autowired
	PinterestRepository pinterestRepository;

	public void deleteUser(PinterestUser deleteUser) {
		pinterestRepository.delete(deleteUser);
	}

	public PinterestUser findById(int pinterestId) {
		return pinterestRepository.findById(pinterestId).orElse(null);
	}

}