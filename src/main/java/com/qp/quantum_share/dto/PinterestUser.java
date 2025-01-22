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
@Table(name = "pinterestuser")
public class PinterestUser {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int pinterestId;
	private String pinterestProfileId;
	private String pinterestProfileName;
    private int pinterestFollowersCount;
    private String pinterestBoardDetails;
    
    @Column(length = 4000)
    private String pinterestProfileImage;
    
    @Column(length = 2000)
	private String pinterestUserAccessToken;
    
    @Column(length = 2000) 
    private String pinterestUserRefreshToken;
    
    private Instant pinterestUserTokenIssuedTime;

}