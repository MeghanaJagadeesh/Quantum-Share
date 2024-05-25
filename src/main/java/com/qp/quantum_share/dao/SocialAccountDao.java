package com.qp.quantum_share.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.qp.quantum_share.dto.SocialAccounts;
import com.qp.quantum_share.repository.SocialAccountsRepository;

@Component
public class SocialAccountDao {

	@Autowired
	SocialAccountsRepository accountsRepository;

	public void save(SocialAccounts accounts) {
		accountsRepository.save(accounts);
	}

}
