package com.qp.quantum_share.dto;

import org.springframework.stereotype.Component;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Component
@Table(name = "instagramuser")
public class InstagramUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int instaId;
	private String instaUserId;
	private String instaUsername;
	private int follwersCount;

	@Column(length = 4000)
	private String pictureUrl;

	@Column(length = 2000)
	private String instUserAccessToken;
}
