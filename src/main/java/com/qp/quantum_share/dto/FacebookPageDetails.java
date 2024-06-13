package com.qp.quantum_share.dto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class FacebookPageDetails {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int pageTableId;
	private String fbPageId;
	private String pageName;

	@Column(length = 2000)
	private String fbPageAceessToken;

	private String instagramId;

//	public FacebookPageDetails(String fbPageId, String pageName, String fbPageAceessToken,
//			String instagramId) {
//		this.fbPageId = fbPageId;
//		this.pageName = pageName;
//		this.fbPageAceessToken = fbPageAceessToken;
//		this.instagramId = instagramId;
//	}

	
}
