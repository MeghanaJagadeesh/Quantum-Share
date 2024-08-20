package com.qp.quantum_share.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.qp.quantum_share.dto.FaceBookUser;

public interface FacebookUserRepository extends JpaRepository<FaceBookUser, Integer> {
	public FaceBookUser findTopByOrderByFbIdDesc();
}
