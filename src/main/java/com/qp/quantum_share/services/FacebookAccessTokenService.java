package com.qp.quantum_share.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qp.quantum_share.configuration.ConfigurationClass;
import com.qp.quantum_share.dao.FaceBookPageDao;
import com.qp.quantum_share.dao.FacebookUserDao;
import com.qp.quantum_share.dao.QuantumShareUserDao;
import com.qp.quantum_share.dao.SocialAccountDao;
import com.qp.quantum_share.dto.FaceBookUser;
import com.qp.quantum_share.dto.FacebookPageDetails;
import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.dto.SocialAccounts;
import com.qp.quantum_share.exception.CommonException;
import com.qp.quantum_share.helper.GenerateId;
import com.qp.quantum_share.response.ResponseStructure;

@Service
public class FacebookAccessTokenService {

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	FacebookUserDao faceBookUserDao;

	@Autowired
	GenerateId generateId;

	@Autowired
	FaceBookUser faceBookUser;

	@Autowired
	FacebookUserDao facebookDao;

	@Autowired
	FaceBookPageDao pageDao;

	@Autowired
	ConfigurationClass configuration;

	@Autowired
	ResponseStructure<String> structure;

	@Autowired
	SocialAccounts socialAccounts;

	@Autowired
	SocialAccountDao accountDao;

	@Autowired
	QuantumShareUserDao userDao;

	public ResponseEntity<ResponseStructure<String>> verifyToken(String access_Token, QuantumShareUser user) {
		String responseUser = fetchUser(access_Token);
		String responsePage = fetchUserPages(access_Token);
		return saveUser(responseUser, responsePage, access_Token, user);
	}

	public ResponseEntity<ResponseStructure<String>> saveUser(String fbUser, String userPage, String acceToken,
			QuantumShareUser user) {
		Map<String, Object> pageProfile = configuration.getMap();
		pageProfile.clear();
		try {
			if (fbUser != null) {
				JsonNode fbuser = objectMapper.readTree(fbUser);
				System.out.println("fb "+fbUser);
				FaceBookUser savedUser = null;
				if (user.getSocialAccounts() == null) {
					savedUser = faceBookUser;
					savedUser.setFbuserId(fbuser.has("id") ? fbuser.get("id").asText() : null);
					savedUser.setFbuserUsername(fbuser.has("name") ? fbuser.get("name").asText() : null);
					savedUser.setUserAccessToken(acceToken);
					savedUser.setEmail(fbuser.has("email") ? fbuser.get("email").asText() : null);
					savedUser.setBirthday(fbuser.has("birthday") ? fbuser.get("birthday").asText() : null);
					savedUser.setFirstName(fbuser.has("first_name") ? fbuser.get("first_name").asText() : null);
					savedUser.setLastName((fbuser.has("last_name") ? fbuser.get("last_name").asText() : null));
					String pictureUrl = fbuser.has("picture") ? fbuser.get("picture").get("data").get("url").asText()
							: null;
					savedUser.setPictureUrl(pictureUrl);
					socialAccounts.setFacebookUser(savedUser);
					user.setSocialAccounts(socialAccounts);
					userDao.save(user);
				} else if (user.getSocialAccounts().getFacebookUser() == null) {
					savedUser = faceBookUser;
					savedUser.setFbuserId(fbuser.has("id") ? fbuser.get("id").asText() : null);
					savedUser.setFbuserUsername(fbuser.has("name") ? fbuser.get("name").asText() : null);
					savedUser.setUserAccessToken(acceToken);
					savedUser.setEmail(fbuser.has("email") ? fbuser.get("email").asText() : null);
					savedUser.setBirthday(fbuser.has("birthday") ? fbuser.get("birthday").asText() : null);
					savedUser.setFirstName(fbuser.has("first_name") ? fbuser.get("first_name").asText() : null);
					savedUser.setLastName((fbuser.has("last_name") ? fbuser.get("last_name").asText() : null));
					String pictureUrl = fbuser.has("picture") ? fbuser.get("picture").get("data").get("url").asText()
							: null;
					savedUser.setPictureUrl(pictureUrl);

					SocialAccounts accounts = user.getSocialAccounts();
					accounts.setFacebookUser(savedUser);
					user.setSocialAccounts(accounts);
					userDao.save(user);
				} else {
					SocialAccounts socialAccounts = user.getSocialAccounts();
					FaceBookUser exfbUser = socialAccounts.getFacebookUser();
					savedUser = exfbUser;
					savedUser.setFbuserId(fbuser.has("id") ? fbuser.get("id").asText() : null);
					savedUser.setFbuserUsername(fbuser.has("name") ? fbuser.get("name").asText() : null);
					savedUser.setUserAccessToken(acceToken);
					savedUser.setEmail(fbuser.has("email") ? fbuser.get("email").asText() : null);
					savedUser.setBirthday(fbuser.has("birthday") ? fbuser.get("birthday").asText() : null);
					savedUser.setFirstName(fbuser.has("first_name") ? fbuser.get("first_name").asText() : null);
					savedUser.setLastName((fbuser.has("last_name") ? fbuser.get("last_name").asText() : null));
					String pictureUrl = fbuser.has("picture") ? fbuser.get("picture").get("data").get("url").asText()
							: null;
					savedUser.setPictureUrl(pictureUrl);
					socialAccounts.setFacebookUser(savedUser);
					user.setSocialAccounts(socialAccounts);
					userDao.save(user);

				}

				List<FacebookPageDetails> pageList = new ArrayList<>();
				if (userPage != null) {
					JsonNode fbuserPage = objectMapper.readTree(userPage);
					JsonNode data = fbuserPage.get("data");

					if (data != null && data.isArray()) {
						int numberOfPages = data.size();

						for (JsonNode page : data) {
							FacebookPageDetails pages = configuration.pageDetails();
							pages.setFbPageId(page.has("id") ? page.get("id").asText() : null);
							pages.setPageName(page.get("name") != null ? page.get("name").asText() : null);
							pages.setFbPageAceessToken(
									page.get("access_token") != null ? page.get("access_token").asText() : null);
							pages.setInstagramId(page.has("instagram_business_account")
									&& page.get("instagram_business_account").has("id")
											? page.get("instagram_business_account").get("id").asText()
											: null);
							pages.setPictureUrl(page.get("picture").get("data").get("url").asText());
//							pageDao.savePage(pages);
							pageProfile.put(page.get("name") != null ? page.get("name").asText() : null,
									page.get("picture").get("data").get("url"));
							pageList.add(pages);

						}
						SocialAccounts acc = user.getSocialAccounts();
						FaceBookUser fb = acc.getFacebookUser();
						fb.setNoOfFbPages(numberOfPages);
						fb.setPageDetails(pageList);
						acc.setFacebookUser(fb);
						user.setSocialAccounts(acc);
						userDao.save(user);

					}
				}
//				facebookDao.saveUser(faceBookUser);
//				accountDao.save(socialAccounts);
				userDao.save(user);
				structure.setCode(HttpStatus.CREATED.value());
				structure.setMessage("Facebook Connected Successfully");
				structure.setStatus("success");
				structure.setPlatform("facebook");
				Map<String, Object> data = configuration.getMap();
				data.clear();
				FaceBookUser datauser = user.getSocialAccounts().getFacebookUser();
				data.put("facebookUrl", datauser.getPictureUrl());
				data.put("facebookUsername", datauser.getFbuserUsername());
				data.put("facebookNumberofpages", datauser.getNoOfFbPages());
				data.put("pages_url", pageProfile);
				structure.setData(data);
				return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.CREATED);
			} else {
				structure.setCode(HttpStatus.NOT_FOUND.value());
				structure.setMessage("unable to find the user");
				structure.setStatus("error");
				structure.setData(null);
				return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
			}
		} catch (JsonProcessingException e) {
			throw new CommonException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new CommonException(e.getMessage());
		}
	}

	private String fetchUser(String userAccessToken) {
		String apiUrl = "https://graph.facebook.com/v19.0/me?fields=id,name,birthday,email,gender,first_name,last_name,picture&access_token="
				+ userAccessToken;
		HttpEntity<String> requestEntity = configuration.getHttpEntity(configuration.httpHeaders());
		ResponseEntity<String> response = configuration.getRestTemplate().exchange(apiUrl, HttpMethod.GET,
				requestEntity, String.class);
		if (response.getStatusCode() == HttpStatus.OK)
			return response.getBody();
		else
			return null;
	}

	public String fetchUserPages(String userAccessToken) {
		String apiUrl = "https://graph.facebook.com/v19.0/me/accounts?fields=id,name,access_token,instagram_business_account,picture&access_token="
				+ userAccessToken;
		HttpEntity<String> requestEntity = configuration.getHttpEntity(configuration.httpHeaders());
		ResponseEntity<String> response = configuration.getRestTemplate().exchange(apiUrl, HttpMethod.GET,
				requestEntity, String.class);
		if (response.getStatusCode() == HttpStatus.OK && response != null)
			return response.getBody();
		else
			return null;
	}

}
