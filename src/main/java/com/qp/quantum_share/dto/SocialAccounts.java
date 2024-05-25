package com.qp.quantum_share.dto;

import org.springframework.stereotype.Component;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.Data;

@Data
@Entity
@Component
public class SocialAccounts {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int socialAccountId;
	@OneToOne(cascade = CascadeType.ALL)
//	@OneToOne
	private FaceBookUser facebookUser;

	@OneToOne(cascade = CascadeType.ALL)
//	@OneToOne
	private InstagramUser instagramUser;
}
