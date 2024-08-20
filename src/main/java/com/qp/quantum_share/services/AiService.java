package com.qp.quantum_share.services;

import java.util.Collections;

import org.json.JSONArray;
import org.json.JSONObject;
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
import org.springframework.web.util.UriComponentsBuilder;

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

	@Autowired
	ResponseStructure<String> responseStructure;

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

//	 public ResponseEntity<ResponseStructure<String>> aiChat(String userMessage, String systemMessage) {
//	        String url = "https://api.openai.com/v1/chat/completions";
//
//	        headers.setContentType(MediaType.APPLICATION_JSON);
//	        headers.setBearerAuth(openAIKey); // Assuming openAIKey is defined somewhere
//
//	        String requestBody = "{\n" +
//	                "    \"model\": \"gpt-3.5-turbo\",\n" +
//	                "    \"messages\": [\n" +
//	                "        {\n" +
//	                "            \"role\": \"system\",\n" +
//	                "            \"content\": \"" + systemMessage + "\"\n" +
//	                "        },\n" +
//	                "        {\n" +
//	                "            \"role\": \"user\",\n" +
//	                "            \"content\": \"" + userMessage + "\"\n" +
//	                "        }\n" +
//	                "    ]\n" +
//	                "}";
//
//	        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);
//
//	        try {
//	            ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
//	            int statusCode = responseEntity.getStatusCode().value();
//	            String responseBody = responseEntity.getBody();
//	            
//	            responseStructure.setStatus("Success");
//	            responseStructure.setMessage("OK");
//	            responseStructure.setCode(statusCode);
//
//	            // Parse the response body to extract the "content" field
//	            JsonNode jsonNode = objectMapper.readTree(responseBody);
//	            String content = jsonNode.path("choices").get(0).path("message").path("content").asText();
//	            responseStructure.setData(content);
//
//	            return ResponseEntity.status(statusCode).body(responseStructure);
//	        } catch (Exception e) {
//	            responseStructure.setStatus("Error");
//	            responseStructure.setMessage("Failed");
//	            responseStructure.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
//	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseStructure);
//	        }
//	    }

	// METHOD TO GENERATE CHAT [GEMINI API]
	public ResponseStructure<String> generateContent(String userQuestion) {

		try {
			// Your existing logic to generate content
			RestTemplate restTemplate = new RestTemplate();

			String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.0-pro:generateContent";

			UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url).queryParam("key", apiKey);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);

			String requestBody = "{\n" + "  \"contents\": [\n" + "    {\n" + "      \"role\": \"user\",\n"
					+ "      \"parts\": [\n" + "        {\n" + "          \"text\": \"what is your name\"\n"
					+ "        }\n" + "      ]\n" + "    },\n" + "     {\n" + "      \"role\": \"model\",\n"
					+ "      \"parts\": [\n" + "        {\n" + "          \"text\": \"" + defaultText + "\"\n"
					+ "        }\n" + "      ]\n" + "    },\n" + "     {\n" + "      \"role\": \"user\",\n"
					+ "      \"parts\": [\n" + "        {\n" + "          \"text\": \"" + userQuestion + "\"\n"
					+ "        }\n" + "      ]\n" + "    }\n" + "  ]\n" + "}";

			HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

			ResponseEntity<String> responseEntity = restTemplate.postForEntity(builder.toUriString(), entity,
					String.class);

			JSONObject jsonResponse = new JSONObject(responseEntity.getBody());
			JSONArray candidates = jsonResponse.getJSONArray("candidates");
			JSONObject content = candidates.getJSONObject(0).getJSONObject("content");
			JSONArray parts = content.getJSONArray("parts");
			String text = parts.getJSONObject(0).getString("text");

			responseStructure.setStatus("Success");
			responseStructure.setMessage("Content generated successfully");
			responseStructure.setCode(HttpStatus.OK.value());
			responseStructure.setData(text);
		} catch (Exception e) {
			responseStructure.setStatus("Error");
			responseStructure.setMessage("Failed to generate content. Internal server error.");
			responseStructure.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
			responseStructure.setData(null);
		}

		return responseStructure;
	}

	public ResponseStructure<String> handleEmptyOrNullRequest() {

		responseStructure.setStatus("Error");
		responseStructure.setMessage("Request parameter 'userMessage' is empty or null");
		responseStructure.setCode(HttpStatus.BAD_REQUEST.value());
		responseStructure.setData(null);
		return responseStructure;
	}

	public ResponseStructure<String> handleExceededLimits() {

		responseStructure.setStatus("Error");
		responseStructure.setMessage("Exceeded limits. Please try again later.");
		responseStructure.setCode(HttpStatus.TOO_MANY_REQUESTS.value());
		responseStructure.setData(null);
		return responseStructure;
	}

	public ResponseStructure<String> handleEmptyGeneratedContent() {

		responseStructure.setStatus("Error");
		responseStructure.setMessage("Generated content is empty");
		responseStructure.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
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