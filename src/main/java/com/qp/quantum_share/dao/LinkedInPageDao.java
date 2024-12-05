package com.qp.quantum_share.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qp.quantum_share.dto.LinkedInPageDto;
import com.qp.quantum_share.repository.LinkedInPageRepository;

@Service
public class LinkedInPageDao {

	@Autowired
	LinkedInPageRepository linkedInPageRepository;

	public LinkedInPageDto save(LinkedInPageDto linkedInPageDto) {
		return linkedInPageRepository.save(linkedInPageDto);
	}

	public void deletePage(LinkedInPageDto linkedInPageDto) {
		linkedInPageRepository.delete(linkedInPageDto);
	}
}

