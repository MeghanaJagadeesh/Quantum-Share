package com.qp.quantum_share.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.qp.quantum_share.dto.FaceBookUser;
import com.qp.quantum_share.dto.FacebookPageDetails;
import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.repository.FacebookPageRepository;

@Service
public class TestService {

	@Autowired
	RestTemplate restTemplate;

	@Autowired
	FacebookPageRepository facebookPageRepository;

	public void fetchAnalytics(QuantumShareUser user) {
		FaceBookUser facebook = user.getSocialAccounts().getFacebookUser();
		if (facebook == null) {

		}
		List<FacebookPageDetails> pages = facebook.getPageDetails();
		Map<String, JsonNode> responses = new HashMap<String, JsonNode>();

		for (FacebookPageDetails page : pages) {
			String url = "https://graph.facebook.com/v22.0/me/feed?limit=50";
			HttpHeaders headers = new HttpHeaders();
			headers.setBearerAuth(page.getFbPageAceessToken());
			HttpEntity<String> requestEntity = new HttpEntity<String>(headers);
			ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity,
					JsonNode.class);
			if (response.getStatusCode().is2xxSuccessful()) {
				responses.put(page.getFbPageId(), response.getBody());
			}
		}
		for (Object entry : responses.entrySet()) {
			System.out.println(entry);
		}
		getDetailsOfPost(facebook, user, responses);
	}

	private void getDetailsOfPost(FaceBookUser facebook, QuantumShareUser user, Map<String, JsonNode> responses) {
		List<FacebookPageDetails> pages = facebook.getPageDetails();
		for (Entry<String, JsonNode> entry : responses.entrySet()) {
			FacebookPageDetails page = facebookPageRepository.findByFbPageId(entry.getKey());
			System.out.println(page);
			JsonNode posts = entry.getValue();
			 JSONObject jsonObject = new JSONObject(posts);
			JSONArray dataArray = jsonObject.getJSONArray("data");
	        List<String> ids = new ArrayList<>();

	        for (int i = 0; i < dataArray.length(); i++) {
	            JSONObject post = dataArray.getJSONObject(i);
	            if (post.has("id")) {
	                ids.add(post.getString("id"));
	                String apiurl="https://graph.facebook.com/v22.0/"+post.getString("id")+"?fields=id,created_time,full_picture,message,permalink_url";
	                HttpHeaders headers=new HttpHeaders();
	                headers.setBearerAuth(page.getFbPageAceessToken());
	                HttpEntity<String> requestEntity=new HttpEntity<String>(headers);
	                ResponseEntity<JsonNode> response = restTemplate.exchange(apiurl, HttpMethod.GET, requestEntity,JsonNode.class);
	                if(response.getStatusCode().is2xxSuccessful()) {
	                	
	                }
	            }
	        }
			
		}
	}
}