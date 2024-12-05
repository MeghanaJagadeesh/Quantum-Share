package com.qp.quantum_share.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.qp.quantum_share.dto.LinkedInPageDto;

@Repository
public interface LinkedInPageRepository extends JpaRepository<LinkedInPageDto, Integer> {

}
