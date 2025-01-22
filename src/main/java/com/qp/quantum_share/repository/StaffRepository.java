package com.qp.quantum_share.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.qp.quantum_share.dto.Staff;

public interface StaffRepository extends JpaRepository<Staff, Integer>{

	boolean existsByEmail(String email);
    void deleteByEmail(String email); 
    Staff findByEmail(String email);
}
