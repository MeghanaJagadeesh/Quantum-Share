package com.qp.quantum_share.dto;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class TwitterUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int twitterId;
	private long twitterUserId;
	private String name;
	private String userName;
	private String picture_url;
	private int follower_count;
	
	
	
}
