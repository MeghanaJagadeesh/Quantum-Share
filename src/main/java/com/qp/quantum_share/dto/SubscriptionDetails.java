package com.qp.quantum_share.dto;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Component;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Component
@Table(name = "subscriptiondetails")
public class SubscriptionDetails {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	int id;
	boolean subscribed;
	String nameOfPackage;
	double packageAmount;
	Instant subscriptionDate;
	int subscriptiondays;
	
	@OneToMany(cascade = CascadeType.ALL)
	List<PaymentDetails> payments;

}
