package com.qp.quantum_share.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class CreditSystem {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int creditid;
	private LocalDate creditedDate;
	private LocalTime creditedTime;
	private int totalAppliedCredit;
	private int remainingCredit;
}
