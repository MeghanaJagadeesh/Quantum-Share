package com.qp.quantum_share.dto;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class SocialMediaPosts {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int pid;
	private String postid;
	private String profileId;
	private String profileName;
	private String platformName;
	private LocalDate postDate;
	private String postTime;
	private String mediaType;
	
	@Column(length = 2000)
	private String imageUrl;

}
