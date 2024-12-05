package com.qp.quantum_share.dto;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Component
@Data
@Entity
public class RedditDto {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	private int redditUid;
	private String redditUsername;
	private String redditUserImage;
	@Column(length = 1000)
	private String redditAccessToken;
	private String redditRefreshToken;
	@Column 
	private Instant tokenIssuedTime; 
	private int redditSubscribers;
	
}
