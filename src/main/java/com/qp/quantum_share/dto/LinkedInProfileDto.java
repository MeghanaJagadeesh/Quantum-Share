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
@Component
@Data
@Table(name = "linkedinprofile")
public class LinkedInProfileDto {

	 	@Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private int profileUid;

	    private String linkedinProfileURN;
	    private String linkedinProfileUserName;
	    private String linkedinProfileEmail;
	    private String linkedinProfileImage;

	    @Column(length = 1000)
	    private String linkedinProfileAccessToken;



}
