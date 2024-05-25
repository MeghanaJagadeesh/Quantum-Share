package com.qp.quantum_share.dao;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.qp.quantum_share.dto.FacebookPageDetails;
import com.qp.quantum_share.repository.FacebookPageRepository;

@Component
public class FaceBookPageDao {

	@Autowired
	FacebookPageRepository facebookPageRepository;

	public void savePage(FacebookPageDetails pageDetails) {
		facebookPageRepository.save(pageDetails);
	}

	public void deletePage(List<FacebookPageDetails> pages) {
		for(FacebookPageDetails page:pages) {
			facebookPageRepository.delete(page);
		}
	}

//	public String findLastPageId() {
//		FacebookPageDetails latestPage = facebookPageRepository.findTopByOrderByPageTableIdDesc();
//		if (latestPage != null) {
//			return latestPage.getPageTableId();
//		}
//		return null;
//	}
}
