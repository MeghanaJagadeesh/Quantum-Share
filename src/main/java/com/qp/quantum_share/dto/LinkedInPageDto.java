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
@Table(name = "linkedinpage")
public class LinkedInPageDto {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int pageUid;

    private String linkedinPageURN;
    private String linkedinPageName;
    private String linkedinPageImage;
    private int linkedinPageFollowers;
    
    @Column(length = 1000)
    private String linkedinPageAccessToken;

}
 
