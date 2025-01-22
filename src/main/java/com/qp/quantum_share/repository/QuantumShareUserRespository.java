package com.qp.quantum_share.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.qp.quantum_share.dto.QuantumShareUser;

public interface QuantumShareUserRespository extends JpaRepository<QuantumShareUser, Integer> {

	public List<QuantumShareUser> findByEmailOrPhoneNo(String email, long phoneNo);

	public QuantumShareUser findTopByOrderByUserIdDesc();

	public QuantumShareUser findByVerificationToken(String token);

	public QuantumShareUser findByEmail(String email);

	@Query("SELECT COUNT(u) FROM QuantumShareUser u")
    long countUsers();
}
