
package com.qp.quantum_share.dto;

import java.util.List;

import org.springframework.stereotype.Component;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Data;

@Data
@Entity
@Component
public class FaceBookUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int fbId;
	private String fbuserId;
	private String fbuserUsername;
	private String firstName;
	private String lastName;
	private String email;
	private String birthday;
	private int noOfFbPages;

	@Column(length = 4000)
	private String pictureUrl;

	@Column(length = 2000)
	private String userAccessToken;

	@OneToMany(cascade=CascadeType.ALL)
	private List<FacebookPageDetails> pageDetails;

}
