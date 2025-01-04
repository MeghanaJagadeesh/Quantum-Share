package com.qp.quantum_share.dto;

import java.time.Instant;

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
@Table(name = "twitteruser")
public class TwitterUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int twitterId;
	private long twitterUserId;
	private String name;
	private String userName;
	private String picture_url;
	private int follower_count;
	private Instant tokenGenerationTime;

	@Column(length = 2000)
	private String access_token;

	@Column(length = 2000)
	private String refresh_token;
	
	
}
