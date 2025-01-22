package com.qp.quantum_share.services;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qp.quantum_share.response.ResponseStructure;

@Service
public class AiService {

//	 @Value("${openai.api.key}")
//	 private String openAIKey;

	@Value("${gemini.api.key}")
	private String apiKey;

	@Value("${gemini.api.defaultText}")
	private String defaultText;

	@Value("${stability.api.url}")
	private String apiUrl;

	@Value("${stability.api.token}")
	private String apiToken;

//	@Autowired
//	ResponseStructure<String> responseStructure;

	@Autowired
	ResponseStructure<byte[]> responsedStructure;

	@Autowired
	RestTemplate restTemplate;

	@Autowired
	HttpHeaders headers;

	@Autowired
	HttpEntity<String> httpEntity;

	@Autowired
	ObjectMapper objectMapper;

	public ResponseStructure<String> generateContent(String userQuestion) {
	    String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;

	    // Construct request body
	    String requestBody = "{\n" +
	            "  \"contents\": [\n" +
	            "    {\n" +
	            "      \"role\": \"user\",\n" +
	            "      \"parts\": [\n" +
	            "        {\n" +
	            "          \"text\": \"what is your name\"\n" +
	            "        }\n" +
	            "      ]\n" +
	            "    },\n" +
	            "    {\n" +
	            "      \"role\": \"model\",\n" +
	            "      \"parts\": [\n" +
	            "        {\n" +
	            "          \"text\": \"" + defaultText + "\"\n" +
	            "        }\n" +
	            "      ]\n" +
	            "    },\n" +
	            "    {\n" +
	            "      \"role\": \"user\",\n" +
	            "      \"parts\": [\n" +
	            "        {\n" +
	            "          \"text\": \"" + userQuestion + "\"\n" +
	            "        }\n" +
	            "      ]\n" +
	            "    }\n" +
	            "  ]\n" +
	            "}";

	    // Set headers
	    HttpHeaders headers = new HttpHeaders();
	    headers.set("Content-Type", "application/json");

	    // Create HttpEntity with body and headers
	    HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

	    // Make POST request
	    ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);

	    try {
	        // Parse JSON response
	        ObjectMapper objectMapper = new ObjectMapper();
	        JsonNode rootNode = objectMapper.readTree(response.getBody());
	        JsonNode textNode = rootNode.path("candidates").get(0).path("content").path("parts").get(0).path("text");

	        // Extract and format response
	        ResponseStructure<String> responseStructure = new ResponseStructure<>();
	        responseStructure.setStatus("success");
	        responseStructure.setMessage("AI content generated successfully");
	        responseStructure.setData(textNode.asText());
	        return responseStructure;

	    } catch (Exception e) {
	        // Handle parsing or other exceptions
	        ResponseStructure<String> responseStructure = new ResponseStructure<>();
	        responseStructure.setStatus("error");
	        responseStructure.setMessage("Failed to parse AI response");
	        responseStructure.setData(null);
	        responseStructure.setCode(200);
	        return responseStructure;
	    }
	}

	    public ResponseStructure<String> handleEmptyOrNullRequest() {
	        ResponseStructure<String> responseStructure = new ResponseStructure<>();
	        responseStructure.setStatus("error");
	        responseStructure.setMessage("User message cannot be null or empty");
	        responseStructure.setData(null);
	        return responseStructure;
	    }

	    public ResponseStructure<String> handleExceededLimits() {
	        ResponseStructure<String> responseStructure = new ResponseStructure<>();
	        responseStructure.setStatus("error");
	        responseStructure.setMessage("API request limits exceeded");
	        responseStructure.setData(null);
	        return responseStructure;
	    }
	    public ResponseStructure<String> handleEmptyGeneratedContent() {
	        ResponseStructure<String> responseStructure = new ResponseStructure<>();
	        responseStructure.setStatus("error");
	        responseStructure.setMessage("Failed to generate AI content");
	        responseStructure.setData(null);
	        return responseStructure;
	    }

	public byte[] generateImage(String textPrompt) {
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(Collections.singletonList(MediaType.IMAGE_PNG));
		headers.setBearerAuth(apiToken);

		// Define request body
		String requestBody = "{\"text_prompts\": [{\"text\": \"" + textPrompt
				+ "\"}],\"cfg_scale\": 7,\"height\": 320,\"width\": 320,\"samples\": 1,\"steps\": 30}";
		HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

		try {
			// Make the HTTP request
			ResponseEntity<byte[]> responseEntity = restTemplate.exchange(apiUrl, HttpMethod.POST, entity,
					byte[].class);
			// Check response status
			HttpStatusCode statusCode = responseEntity.getStatusCode();
			if (statusCode == HttpStatus.OK) {
				return responseEntity.getBody();
			} else {
				throw new RuntimeException("Failed to generate image. Status code: " + statusCode.value());
			}
		} catch (HttpClientErrorException e) {
			// Handle client-side errors (4xx)
			if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
				throw new RuntimeException(
						"{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Unauthorized access/Token.\"}");
			} else {
				throw new RuntimeException("{\"status\":" + e.getStatusCode().value()
						+ ",\"error\":\"Client Error\",\"message\":\"" + e.getStatusText() + "\"}");
			}
		} catch (HttpServerErrorException e) {
			// Handle server-side errors (5xx)
			throw new RuntimeException("{\"status\":" + e.getStatusCode().value()
					+ ",\"error\":\"Server Error\",\"message\":\"" + e.getStatusText() + "\"}");
		} catch (RestClientException e) {
			// Handle other RestClientExceptions
			throw new RuntimeException(
					"{\"status\":500,\"error\":\"Rest Client Error\",\"message\":\"" + e.getMessage() + "\"}");
		}
	}
}