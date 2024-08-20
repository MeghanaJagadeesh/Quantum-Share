package com.qp.quantum_share.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.qp.quantum_share.dto.InstagramUser;

public interface InstagramRepository extends JpaRepository<InstagramUser, Integer> {
	public InstagramUser findTopByOrderByInstaIdDesc();
}
