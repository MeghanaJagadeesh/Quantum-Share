package com.qp.quantum_share.services;



import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class TestService {

	// Replace with your actual WhatsApp Number ID
	private static final String WHATSAPP_NUMBER_ID = "463018520236115";

	// Update API URL with the number ID
	private static final String API_URL = "https://graph.facebook.com/v21.0/" + WHATSAPP_NUMBER_ID + "/messages";

	private static final String ACCESS_TOKEN = "EAAPPo51qkGcBO3AqnZAQwi9aJv0GlqAacP42Pea9geTOcZApKAFpzXVZCl5JnTMpZAWrBnb3BTyjpgsRTZBmoqwPQcDXcDCIHPHMDPBrzQxkTBbNqQoxQwFZA8O0RLsoX9OrHfZAwZCqNoCaeJUMZBV35xbZCy5lTpX4tOYapejrI0zQa5bdZAJROnqcoZBxqgDAxiLrAtR5jPErkoXZCIV2xiZCUZD";

	public void sendBulkMessages(List<String> phoneNumbers) {
		RestTemplate restTemplate = new RestTemplate();

		for (String phone : phoneNumbers) {
			try {
				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_JSON);
				headers.setBearerAuth(ACCESS_TOKEN);

				String requestBody = createMessageBody(phone);
				HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

				ResponseEntity<String> response = restTemplate.postForEntity(API_URL, request, String.class);

				
			} catch (Exception e) {
				System.err.println("Error sending message to: " + phone + " - " + e.getMessage());
			}
		}
	}

	private String createMessageBody(String phoneNumber) {
		return "{\n" + "  \"messaging_product\": \"whatsapp\",\n" + "  \"to\": \"" + phoneNumber + "\",\n"
				+ "  \"type\": \"template\",\n" + "  \"template\": {\n" + "    \"name\": \"new_year_wish\",\n"
				+ "    \"language\": { \"code\": \"en_US\" }\n" + "  }\n" + "}";
	}
}