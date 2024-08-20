package com.qp.quantum_share.dto;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Data;

@Entity
@Data
@Component
public class SubscriptionDetails {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	int id;
	boolean subscribed;
	String nameOfPackage;
	double packageAmount;
	LocalDate subscriptionDate;
	int subscriptiondays;
	
	@OneToMany(cascade = CascadeType.ALL)
	List<PaymentDetails> payments;

}
