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
@Table(name = "youtubeuser")
public class YoutubeUser {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int youtubeId;
	private String youtubeChannelId;
	private String channelName;
    private int subscriberCount;
    
    @Column(length = 4000)
    private String channelImageUrl;
    
    @Column(length = 2000)
	private String youtubeUserAccessToken;
    
    @Column(length = 2000) 
    private String youtubeUserRefreshToken;
    
    private Instant ytUserTokenIssuedTime;

}