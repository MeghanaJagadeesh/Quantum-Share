package com.qp.quantum_share.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qp.quantum_share.configuration.ConfigurationClass;
import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.exception.CommonException;
import com.qp.quantum_share.response.ResponseStructure;

@Service
public class TwitterService {

	@Value("${twitter.client_id}")
	private String client_id;

	@Value("${twitter.redirect_uri}")
	private String redirect_uri;

	@Value("${twitter.code_challenge_method}")
	private String code_challenge_method;

	@Value("${twitter.scope}")
	private String scope;

	@Value("${twitter.state}")
	private String state;

	@Value("${twitter.code_challenge}")
	private String code_challenge;

	@Autowired
	ResponseStructure<String> structure;

	@Autowired
	HttpHeaders headers;

	@Autowired
	ConfigurationClass configurationClass;

	@Autowired
	MultiValueMap<String, Object> multiValueMap;

	@Autowired
	RestTemplate restTemplate;

	@Autowired
	ObjectMapper objectMapper;

	public ResponseEntity<ResponseStructure<String>> getAuthorizationUrl(QuantumShareUser user) {
		String apiUrl = "https://twitter.com/i/oauth2/authorize";
		String ouath = apiUrl + "?response_type=code&client_id=" + client_id + "&redirect_uri=" + redirect_uri
				+ "&scope=" + scope + "&state="+user.getUserId()+"&code_challenge=" + code_challenge + "&code_challenge_method="
				+ code_challenge_method;
		System.out.println(ouath);
		structure.setCode(HttpStatus.OK.value());
		structure.setStatus("success");
		structure.setMessage("oauth_url generated successfully");
		structure.setPlatform(null);
		structure.setData(ouath);
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
	}

	public ResponseEntity<ResponseStructure<String>> verifyToken(String code, QuantumShareUser user) {
		try {
			String url = "https://api.twitter.com/oauth2/token";

			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

			multiValueMap.add("code", code);
			multiValueMap.add("grant_type", "authorization_code");
			multiValueMap.add("redirect_uri", "https://quantumparadigm.in/");
			multiValueMap.add("code_verifier", "challenge");
			multiValueMap.add("client_id", "aWRKTjQ5ZVFqZzRSUnRLeEdVRU46MTpjaQ");

			HttpEntity<MultiValueMap<String, Object>> httpRequest = new HttpEntity<>(multiValueMap, headers);
			System.out.println("request : " + httpRequest + "   -   " + httpRequest.toString());
			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, httpRequest, String.class);
			System.out.println("response  :  "+response);
			if (response.getStatusCode().is2xxSuccessful()) {
				JsonNode responseBody = objectMapper.readTree(response.getBody());
				System.out.println("success");
				String access_token = responseBody.get("access_token").toString();
				System.out.println(access_token);
				return fetchUser(access_token);
			} else {
				structure.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
				structure.setData(null);
				structure.setMessage("Something went wrong!!");
				structure.setPlatform(null);
				structure.setStatus("error");
				return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.INTERNAL_SERVER_ERROR);
			}

		} catch (JsonProcessingException e) {
			throw new CommonException(e.getMessage());
		}// catch (Exception e) {
//			e.printStackTrace();
//			throw new CommonException(e.getMessage());
//		}

	}

	private ResponseEntity<ResponseStructure<String>> fetchUser(String access_token) {
		try {
			String apiUrl = "https://api.twitter.com/2/users/me";
			headers.setBearerAuth(access_token);
			String requestBody = "user.fields=id,name,profile_image_url,username,public_metrics";
			HttpEntity<String> httpRequest = configurationClass.getHttpEntity(requestBody, headers);
			ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.GET, httpRequest, String.class);
			System.out.println(response);
			return null;
		} catch (NullPointerException exception) {
			throw new CommonException(exception.getMessage());
		}
	}
}
