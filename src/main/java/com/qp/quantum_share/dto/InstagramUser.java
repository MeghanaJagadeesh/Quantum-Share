package com.qp.quantum_share.dto;

import org.springframework.stereotype.Component;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
@Component
public class InstagramUser {

	@Id
	private String instaId;
	private String instaUserId;
	private String instaUsername;
	private int follwersCount;

	@Column(length = 4000)
	private String pictureUrl;

	@Column(length = 2000)
	private String instUserAccessToken;
}
