package com.qp.quantum_share.services;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.qp.quantum_share.dto.FacebookPageDetails;

@Service
public class TestService {

	public String postVideoToPage(FacebookPageDetails page, MultipartFile videoFile, String message) {
		try {
			String url = "https://graph.facebook.com/v20.0/" + page.getFbPageId() + "/videos";

			// Prepare headers
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.MULTIPART_FORM_DATA);
			headers.setBearerAuth(page.getFbPageAceessToken());

			// Prepare body
			MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
			body.add("file", videoFile.getResource());
			body.add("description", message);

			HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

			// Send request
			RestTemplate restTemplate = new RestTemplate();
			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
			System.out.println(response);
			// Check response status and return appropriate result
			if (response.getStatusCode().is2xxSuccessful()) {
				return "Video posted successfully: " + response.getBody();
			} else {
				return "Failed to post video: " + response.getBody();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return "Exception occurred while posting video: " + e.getMessage();
		}
	}
}
