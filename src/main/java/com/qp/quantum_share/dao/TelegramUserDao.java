package com.qp.quantum_share.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.qp.quantum_share.dto.TelegramUser;
import com.qp.quantum_share.repository.TelegramRepository;

@Component
public class TelegramUserDao {

	@Autowired
	TelegramRepository telegramRepository;

	public TelegramUser findById(int tgId) {
		return telegramRepository.findById(tgId).orElse(null);
	}

	public void deleteUser(TelegramUser user) {
		telegramRepository.delete(user);		
	}

}