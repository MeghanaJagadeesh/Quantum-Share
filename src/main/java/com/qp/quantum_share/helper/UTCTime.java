package com.qp.quantum_share.helper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

@Component
public class UTCTime {

	public long ConvertScheduledTimeFromLocal(String localDateTime, String userTimeZone) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
		LocalDateTime localDate = LocalDateTime.parse(localDateTime, formatter);
		ZonedDateTime zonedDateTime = localDate.atZone(ZoneId.of(userTimeZone))
                .withZoneSameInstant(ZoneId.of("UTC"));
		Instant scheduledTimeInUTC = zonedDateTime.toInstant();
	    
	    return scheduledTimeInUTC.getEpochSecond();
	}

}
