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

//	@Autowired
//	ResponseStructure<String> structure;

	@Autowired
	SocialAccounts socialAccounts;

	@Autowired
	SocialAccountDao accountDao;

	@Autowired
	QuantumShareUserDao userDao;

	public ResponseEntity<ResponseStructure<String>> verifyToken(String access_Token, QuantumShareUser user,
			int userId) {
		JsonNode responseUser = fetchUser(access_Token);
		JsonNode responsePage = fetchUserPages(access_Token);
		return getFBUserDetails(responseUser, responsePage, access_Token);
//		return saveUser(responseUser, responsePage, access_Token, user,userId);
	}

	private ResponseEntity<ResponseStructure<String>> getFBUserDetails(JsonNode responseUser, JsonNode fbuserPage,
			String access_Token) {
		// fbuser
		FaceBookUser fbuser = new FaceBookUser();
		fbuser.setFbuserId(responseUser.has("id") ? responseUser.get("id").asText() : null);
		fbuser.setFbuserUsername(responseUser.has("name") ? responseUser.get("name").asText() : null);
		fbuser.setUserAccessToken(access_Token);
		fbuser.setEmail(responseUser.has("email") ? responseUser.get("email").asText() : null);
		fbuser.setBirthday(responseUser.has("birthday") ? responseUser.get("birthday").asText() : null);
		fbuser.setFirstName(responseUser.has("first_name") ? responseUser.get("first_name").asText() : null);
		fbuser.setLastName((responseUser.has("last_name") ? responseUser.get("last_name").asText() : null));
		String pictureUrl = responseUser.has("picture") ? responseUser.get("picture").get("data").get("url").asText()
				: null;
		fbuser.setPictureUrl(pictureUrl);

		// pages
		List<FacebookPageDetails> pageList = new ArrayList<>();
		if (fbuserPage != null) {
			JsonNode pages = fbuserPage.get("data");
			if (pages != null && pages.isArray()) {
				int numberOfPages = pages.size();
				fbuser.setNoOfFbPages(numberOfPages);
				for (JsonNode page : pages) {
					FacebookPageDetails fbpage = new FacebookPageDetails();
					fbpage.setFbPageId(page.has("id") ? page.get("id").asText() : null);
					fbpage.setPageName(page.get("name") != null ? page.get("name").asText() : null);
					fbpage.setFbPageAceessToken(
							page.get("access_token") != null ? page.get("access_token").asText() : null);
					fbpage.setInstagramId(
							page.has("instagram_business_account") && page.get("instagram_business_account").has("id")
									? page.get("instagram_business_account").get("id").asText()
									: null);
					fbpage.setPictureUrl(page.get("picture").get("data").get("url").asText());
					pageList.add(fbpage);
				}
			}
		}

		Map<String, Object> map = configuration.getMap();
		map.put("fbuser", fbuser);
		map.put("fbpages", pageList);
		ResponseStructure<String> structure = new ResponseStructure<String>();
		structure.setCode(HttpStatus.OK.value());
		structure.setData(map);
		structure.setMessage(null);
		structure.setPlatform(null);
		structure.setStatus("success");
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
	}

	public ResponseEntity<ResponseStructure<String>> saveUser(FaceBookUser fbuser, List<FacebookPageDetails> pageList,
			QuantumShareUser user) {
		SocialAccounts socialAccounts = user.getSocialAccounts();
		fbuser.setPageDetails(pageList);
		fbuser.setNoOfFbPages(pageList.size());
		if (socialAccounts == null) {
			socialAccounts = new SocialAccounts();
		}
		socialAccounts.setFacebookUser(fbuser);
		user.setSocialAccounts(socialAccounts);
		userDao.save(user);

		ResponseStructure<String> structure = new ResponseStructure<String>();
		structure.setCode(HttpStatus.CREATED.value());
		structure.setMessage("Facebook Connected Successfully");
		structure.setStatus("success");
		structure.setPlatform("facebook");
		Map<String, Object> data = configuration.getMap();
		FaceBookUser datauser = user.getSocialAccounts().getFacebookUser();
		data.put("facebookUrl", datauser.getPictureUrl());
		
		data.put("facebookUsername", datauser.getFbuserUsername());
		data.put("facebookNumberofpages", datauser.getNoOfFbPages());
		Map<String, Object> pageProfile = configuration.getMap();
		List<FacebookPageDetails> list = datauser.getPageDetails();
		for (FacebookPageDetails page : list) {
			pageProfile.put(page.getPageName(), page.getPictureUrl());
		}

		data.put("pages_url", pageProfile);
		structure.setData(data);
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.CREATED);

	}
//		System.out.println("*****************3********************");
//		Map<String, Object> pageProfile = configuration.getMap();
//		System.out.println("Pageprofile" + pageProfile);
//		pageProfile.clear();
//		System.out.println("Pageprofile" + pageProfile);
//		try {
//			if (fbUser != null) {
//				JsonNode fbuser = objectMapper.readTree(fbUser);
//				FaceBookUser savedUser = new FaceBookUser();
//				if (user.getSocialAccounts() == null) {
//					savedUser.setFbuserId(fbuser.has("id") ? fbuser.get("id").asText() : null);
//					savedUser.setFbuserUsername(fbuser.has("name") ? fbuser.get("name").asText() : null);
//					savedUser.setUserAccessToken(acceToken);
//					savedUser.setEmail(fbuser.has("email") ? fbuser.get("email").asText() : null);
//					savedUser.setBirthday(fbuser.has("birthday") ? fbuser.get("birthday").asText() : null);
//					savedUser.setFirstName(fbuser.has("first_name") ? fbuser.get("first_name").asText() : null);
//					savedUser.setLastName((fbuser.has("last_name") ? fbuser.get("last_name").asText() : null));
//					String pictureUrl = fbuser.has("picture") ? fbuser.get("picture").get("data").get("url").asText()
//							: null;
//					savedUser.setPictureUrl(pictureUrl);
//					SocialAccounts socialAccounts = new SocialAccounts();
//					socialAccounts.setFacebookUser(savedUser);
//					user.setSocialAccounts(socialAccounts);
//					userDao.save(user);
//				} else if (user.getSocialAccounts().getFacebookUser() == null) {
//					savedUser.setFbuserId(fbuser.has("id") ? fbuser.get("id").asText() : null);
//					savedUser.setFbuserUsername(fbuser.has("name") ? fbuser.get("name").asText() : null);
//					savedUser.setUserAccessToken(acceToken);
//					savedUser.setEmail(fbuser.has("email") ? fbuser.get("email").asText() : null);
//					savedUser.setBirthday(fbuser.has("birthday") ? fbuser.get("birthday").asText() : null);
//					savedUser.setFirstName(fbuser.has("first_name") ? fbuser.get("first_name").asText() : null);
//					savedUser.setLastName((fbuser.has("last_name") ? fbuser.get("last_name").asText() : null));
//					String pictureUrl = fbuser.has("picture") ? fbuser.get("picture").get("data").get("url").asText()
//							: null;
//					savedUser.setPictureUrl(pictureUrl);
//
//					SocialAccounts accounts = user.getSocialAccounts();
//					accounts.setFacebookUser(savedUser);
//					user.setSocialAccounts(accounts);
//					userDao.save(user);
//				} else {
//					SocialAccounts socialAccounts = user.getSocialAccounts();
//					FaceBookUser exfbUser = socialAccounts.getFacebookUser();
//					savedUser = exfbUser;
//					savedUser.setFbuserId(fbuser.has("id") ? fbuser.get("id").asText() : null);
//					savedUser.setFbuserUsername(fbuser.has("name") ? fbuser.get("name").asText() : null);
//					savedUser.setUserAccessToken(acceToken);
//					savedUser.setEmail(fbuser.has("email") ? fbuser.get("email").asText() : null);
//					savedUser.setBirthday(fbuser.has("birthday") ? fbuser.get("birthday").asText() : null);
//					savedUser.setFirstName(fbuser.has("first_name") ? fbuser.get("first_name").asText() : null);
//					savedUser.setLastName((fbuser.has("last_name") ? fbuser.get("last_name").asText() : null));
//					String pictureUrl = fbuser.has("picture") ? fbuser.get("picture").get("data").get("url").asText()
//							: null;
//					savedUser.setPictureUrl(pictureUrl);
//					socialAccounts.setFacebookUser(savedUser);
//					user.setSocialAccounts(socialAccounts);
//					userDao.save(user);
//
//				}
//
//				List<FacebookPageDetails> pageList = new ArrayList<>();
//				if (userPage != null) {
//					JsonNode fbuserPage = objectMapper.readTree(userPage);
//					JsonNode data = fbuserPage.get("data");
//
//					if (data != null && data.isArray()) {
//						int numberOfPages = data.size();
//
//						for (JsonNode page : data) {
//							FacebookPageDetails pages = new FacebookPageDetails();
//							pages.setFbPageId(page.has("id") ? page.get("id").asText() : null);
//							pages.setPageName(page.get("name") != null ? page.get("name").asText() : null);
//							pages.setFbPageAceessToken(
//									page.get("access_token") != null ? page.get("access_token").asText() : null);
//							pages.setInstagramId(page.has("instagram_business_account")
//									&& page.get("instagram_business_account").has("id")
//											? page.get("instagram_business_account").get("id").asText()
//											: null);
//							pages.setPictureUrl(page.get("picture").get("data").get("url").asText());
////							pageDao.savePage(pages);
//							pageProfile.put(page.get("name") != null ? page.get("name").asText() : null,
//									page.get("picture").get("data").get("url"));
//							pageList.add(pages);
//
//						}
//						SocialAccounts acc = user.getSocialAccounts();
//						FaceBookUser fb = acc.getFacebookUser();
//						fb.setNoOfFbPages(numberOfPages);
//						fb.setPageDetails(pageList);
//						acc.setFacebookUser(fb);
//						user.setSocialAccounts(acc);
//						userDao.save(user);
//
//					}
//				}
////				facebookDao.saveUser(faceBookUser);
////				accountDao.save(socialAccounts);
////				userDao.save(user);
//				ResponseStructure<String> structure = new ResponseStructure<String>();
//				structure.setCode(HttpStatus.CREATED.value());
//				structure.setMessage("Facebook Connected Successfully");
//				structure.setStatus("success");
//				structure.setPlatform("facebook");
//				Map<String, Object> data = configuration.getMap();
//				System.out.println(data);
//				data.clear();
//				System.out.println(data);
//				FaceBookUser datauser = user.getSocialAccounts().getFacebookUser();
//				data.put("facebookUrl", datauser.getPictureUrl());
//				data.put("facebookUsername", datauser.getFbuserUsername());
//				data.put("facebookNumberofpages", datauser.getNoOfFbPages());
//				data.put("pages_url", pageProfile);
//				data.put("user_id", userId);
//				structure.setData(data);
//				return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.CREATED);
//			} else {
//				ResponseStructure<String> structure = new ResponseStructure<String>();
//				structure.setCode(HttpStatus.NOT_FOUND.value());
//				structure.setMessage("unable to find the user");
//				structure.setStatus("error");
//				structure.setData(null);
//				structure.setPlatform(null);
//				return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
//			}
//		} catch (JsonProcessingException e) {
//			throw new CommonException(e.getMessage());
//		} catch (Exception e) {
//			e.printStackTrace();
//			throw new CommonException(e.getMessage());
//		}

	private JsonNode fetchUser(String userAccessToken) {
		String apiUrl = "https://graph.facebook.com/v19.0/me?fields=id,name,birthday,email,gender,first_name,last_name,picture&access_token="
				+ userAccessToken;
		HttpEntity<String> requestEntity = configuration.getHttpEntity(configuration.httpHeaders());
		ResponseEntity<JsonNode> response = configuration.getRestTemplate().exchange(apiUrl, HttpMethod.GET,
				requestEntity, JsonNode.class);
		if (response.getStatusCode() == HttpStatus.OK)
			return response.getBody();
		else
			return null;
	}

	public JsonNode fetchUserPages(String userAccessToken) {
		String apiUrl = "https://graph.facebook.com/v19.0/me/accounts?fields=id,name,access_token,instagram_business_account,picture&access_token="
				+ userAccessToken;
		HttpEntity<String> requestEntity = configuration.getHttpEntity(configuration.httpHeaders());
		ResponseEntity<JsonNode> response = configuration.getRestTemplate().exchange(apiUrl, HttpMethod.GET,
				requestEntity, JsonNode.class);
		if (response.getStatusCode() == HttpStatus.OK && response != null)
			return response.getBody();
		else
			return null;
	}

}
