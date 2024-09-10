package com.qp.quantum_share.dto;

import java.util.List;

import org.springframework.stereotype.Component;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Data;

@Entity
@Component
@Data
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

	    @OneToMany(mappedBy = "profile", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	    private List<LinkedInPageDto> pages;


}
