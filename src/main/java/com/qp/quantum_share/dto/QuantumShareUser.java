package com.qp.quantum_share.dto;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.Data;

@Data
@Entity
@Component
public class QuantumShareUser {

	@Id
	@Column(unique = true)
	private String userId;
	private String firstName;
	private String lastName;
	private String email;
	private long phoneNo;
	private String password;
	private String company;
	private boolean verified;
	private String verificationToken;
	private LocalDate signUpDate;
	private String profilePic;
	
	@OneToOne(cascade=CascadeType.ALL)
//	@OneToOne
	private SocialAccounts socialAccounts;
}
