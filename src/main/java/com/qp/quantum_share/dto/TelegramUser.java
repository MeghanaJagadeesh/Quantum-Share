package com.qp.quantum_share.dto;

import org.springframework.stereotype.Component;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
@Component
public class TelegramUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int telegramId;
	private String telegramCode;
	private long telegramChatId;
	private String telegramGroupName;
	private int telegramGroupMembersCount;
	
	@Column(length = 4000)
	private String telegramProfileUrl;
	
}