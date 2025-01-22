package com.qp.quantum_share.controller;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.qp.quantum_share.helper.UTCTime;

@RestController
@RequestMapping("/test")
public class TestClass {
	@Autowired
	UTCTime utcTime;

	@GetMapping("/date")
	public void convertdate() {
		 String dateString = "15-01-2025 15:30";
		 String userTimeZone="America/New_York";
		 System.out.println(utcTime.ConvertScheduledTimeFromLocal(dateString, userTimeZone));
	        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
	        
	        LocalDateTime dateTime = LocalDateTime.parse(dateString, formatter);
	        System.out.println(dateTime+" "+dateString.toString());
	        long unixTime = dateTime.atZone(ZoneId.systemDefault()).toEpochSecond();
	        
	        System.out.println("Unix Time in Seconds: " + unixTime);
	        
	}
}
