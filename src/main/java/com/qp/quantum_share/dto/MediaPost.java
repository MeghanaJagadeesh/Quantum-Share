package com.qp.quantum_share.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class MediaPost {
	private String mediaPlatform;
	private String caption;
	private String title;
	private String visibility;
	private LocalDateTime scheduledTime;
}
