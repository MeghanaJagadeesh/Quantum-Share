package com.qp.quantum_share.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.qp.quantum_share.dto.FacebookPageDetails;

public interface FacebookPageRepository extends JpaRepository<FacebookPageDetails, Integer> {

	public FacebookPageDetails findTopByOrderByPageTableIdDesc();
}