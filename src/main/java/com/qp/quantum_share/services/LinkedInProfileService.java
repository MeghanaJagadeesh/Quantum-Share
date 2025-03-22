package com.qp.quantum_share.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qp.quantum_share.configuration.ConfigurationClass;
import com.qp.quantum_share.dao.LinkedInPageDao;
import com.qp.quantum_share.dao.LinkedInProfileDao;
import com.qp.quantum_share.dao.QuantumShareUserDao;
import com.qp.quantum_share.dto.LinkedInPageDto;
import com.qp.quantum_share.dto.LinkedInProfileDto;
import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.dto.SocialAccounts;
import com.qp.quantum_share.exception.CommonException;
import com.qp.quantum_share.response.ResponseStructure;

@Service
public class LinkedInProfileService {

	@Value("${default.profile.picture}")
	private String defaultImageUrl;

	@Value("${linkedin.clientId}")
	private String clientId;

	@Value("${linkedin.clientSecret}")
	private String clientSecret;

	@Value("${linkedin.redirectUri}")
	private String redirectUri;

	@Value("${linkedin.scope}")
	private String scope;

	@Autowired
	LinkedInProfileDto linkedInProfileDto;

	@Autowired
	LinkedInProfileDao linkedInProfileDao;

	@Autowired
	LinkedInPageDto linkedInPageDto;

	@Autowired
	LinkedInPageDao linkedInPageDao;

//	@Autowired
//	ResponseStructure<String> structure;

	@Autowired
	RestTemplate restTemplate;

	@Autowired
	QuantumShareUserDao userDao;

	@Autowired
	ConfigurationClass config;

	@Autowired
	MultiValueMap<String, Object> body;

	@Autowired
	SocialAccounts accounts;

	@Autowired
	ObjectMapper objectMapper;
	
	public ResponseEntity<Map<String, String>> getLinkedInAuth() {
		Map<String, String> authUrlParams = new HashMap<>();
		authUrlParams.put("clientId", clientId);
		authUrlParams.put("redirectUri", redirectUri);
		authUrlParams.put("scope", scope);
		return ResponseEntity.ok(authUrlParams);
	}

	public ResponseEntity<?> getPagesAndProfile(String code, QuantumShareUser user, int userId) {
		ResponseStructure<String> structure = new ResponseStructure<String>();
		String accessToken = getAccessToken(code);
		if (accessToken == null) {
			structure.setCode(500);
			structure.setMessage("Failed to retrieve access token");
			structure.setStatus("error");
			structure.setPlatform("LinkedIn");
			structure.setData(null);
			return new ResponseEntity<>(structure, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		Map<String, Object> profile = getProfileInfo(accessToken, user, userId);
		List<Map<String, Object>> pages = getOrganizationsDetails(accessToken, user, userId);
		Map<String, Object> data = config.getMap();
		data.clear();
		if (!profile.isEmpty()) {
			data.put("linkedInProfile", profile);
			if (pages == null) {
				data.put("linkedInPages", null);
			} else if (pages.isEmpty()) {
				data.put("linkedInPages", null);
			} else {
				data.put("linkedInPages", pages);
			}
			structure.setCode(200);
			structure.setMessage("LinkedIn Profile Connected Successfully");
			structure.setStatus("success");
			structure.setPlatform("LinkedIn");
			structure.setData(data);
			return new ResponseEntity<>(structure, HttpStatus.OK);
		} else {
			structure.setCode(500);
			structure.setMessage("Failed to retrieve organization info");
			structure.setStatus("error");
			structure.setPlatform("LinkedIn");
			structure.setData(null);
			return new ResponseEntity<>(structure, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	public ResponseEntity<Object> saveLinkedInProfile(LinkedInProfileDto profile, QuantumShareUser user, int userId) {
		SocialAccounts socialAccount = user.getSocialAccounts();
		if (socialAccount == null) {
			SocialAccounts accounts = new SocialAccounts();
			accounts.setLinkedInProfileDto(profile);
			accounts.setLinkedInPagePresent(false);
			user.setSocialAccounts(accounts);
		} else if (socialAccount.getLinkedInProfileDto() == null) {
			socialAccount.setLinkedInProfileDto(profile);
			socialAccount.setLinkedInPagePresent(false);
			user.setSocialAccounts(socialAccount);
		} else {
			LinkedInProfileDto exuser = socialAccount.getLinkedInProfileDto();
			exuser.setLinkedinProfileURN(profile.getLinkedinProfileURN());
			exuser.setLinkedinProfileUserName(profile.getLinkedinProfileUserName());
			exuser.setLinkedinProfileAccessToken(profile.getLinkedinProfileAccessToken());
			exuser.setLinkedinProfileImage(profile.getLinkedinProfileImage()); // Set profile image URL
			socialAccount.setLinkedInProfileDto(exuser);
			socialAccount.setLinkedInPagePresent(false);
			user.setSocialAccounts(socialAccount);
		}
		userDao.saveUser(user);
		ResponseStructure<String> structure = new ResponseStructure<String>();
		structure.setCode(HttpStatus.OK.value());
		structure.setData(user.getSocialAccounts().getLinkedInProfileDto());
		structure.setMessage("linkedin connected successfully");
		structure.setPlatform("linkedin");
		structure.setStatus("success");
		return new ResponseEntity<Object>(structure, HttpStatus.OK);
	}

	public String getAccessToken(String code) {
		String url = "https://www.linkedin.com/oauth/v2/accessToken";
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, Object> body = config.getMultiValueMap();
		body.add("grant_type", "authorization_code");
		body.add("code", code);
		body.add("client_id", clientId);
		body.add("client_secret", clientSecret);
		body.add("redirect_uri", redirectUri);
		try {
			HttpEntity<MultiValueMap<String, Object>> requestEntity = config.getHttpEntityWithMap(body, headers);
			ResponseEntity<JsonNode> responseEntity = restTemplate.exchange(url, HttpMethod.POST, requestEntity,
					JsonNode.class);
			JsonNode response = responseEntity.getBody();
			return response.get("access_token").asText();
		} catch (Exception e) {
			throw new CommonException(e.getMessage());
		}
	}
	
	//profile
	public Map<String, Object> getProfileInfo(String accessToken, QuantumShareUser user, int userId) {
		String userInfoUrl = "https://api.linkedin.com/v2/me";
		HttpHeaders headers = new HttpHeaders();
		System.out.println("access token linkedin "+accessToken);
		headers.setBearerAuth(accessToken);
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = config.getHttpEntity(headers);
		ResponseEntity<JsonNode> response = restTemplate.exchange(userInfoUrl, HttpMethod.GET, entity, JsonNode.class);
		if (response.getStatusCode() == HttpStatus.OK) {
			JsonNode responseBody = response.getBody();
			Map<String, Object> map = config.getMap();
			map.clear();
			String localizedFirstName = responseBody.path("localizedFirstName").asText();
			String localizedLastName = responseBody.path("localizedLastName").asText();
			String id = responseBody.path("id").asText();
			String imageUrl = getLinkedInProfile(accessToken);
			if (imageUrl == null)
				imageUrl = defaultImageUrl;
			map.put("urn", id);
			map.put("name", localizedFirstName + " " + localizedLastName);
			map.put("accessToken", accessToken);
			map.put("profile_image", imageUrl);
			return map;
		} else {
			return null;
		}
	}

	public String getLinkedInProfile(String accessToken) {
		ResponseStructure<String> responseStructure = new ResponseStructure<String>();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);
		headers.set("Authorization", "Bearer " + accessToken);
		HttpEntity<String> entity = config.getHttpEntity(headers);
		try {
			ResponseEntity<JsonNode> response = restTemplate.exchange(
					"https://api.linkedin.com/v2/me?projection=(id,profilePicture(displayImage~:playableStreams))",
					HttpMethod.GET, entity, JsonNode.class);

			String imageUrl = "https://quantumshare.quantumparadigm.in/vedio/ProfilePicture.jpg"; // default image URL
			if (response.getStatusCode() == HttpStatus.OK) {
				JsonNode rootNode = response.getBody();
				JsonNode elements = rootNode.path("profilePicture").path("displayImage~").path("elements");
				if (elements.isArray() && elements.size() > 0) {
					for (JsonNode element : elements) {
						JsonNode displaySize = element.path("data")
								.path("com.linkedin.digitalmedia.mediaartifact.StillImage").path("displaySize");
						if (displaySize.path("width").asInt() == 200 && displaySize.path("height").asInt() == 200) {
							JsonNode identifiers = element.path("identifiers");
							if (identifiers.isArray() && identifiers.size() > 0) {
								String fetchedImageUrl = identifiers.get(0).path("identifier").asText();
								if (fetchedImageUrl != null && !fetchedImageUrl.isEmpty()) {
									imageUrl = fetchedImageUrl;
									break;
								}
							}
						}
					}
				}
				return imageUrl;
			} else {
				return null;
			}
		} catch (RestClientException e) {
			responseStructure.setStatus("Failure");
			responseStructure.setMessage("Exception occurred: " + e.getMessage());
			responseStructure.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
			responseStructure.setData("https://quantumshare.quantumparadigm.in/vedio/ProfilePicture.jpg"); // return
			responseStructure.setPlatform("linkedin"); // exception
		}
		return null;
	}

	
	//Pages
	public List<Map<String, Object>> getOrganizationsDetails(String accessToken, QuantumShareUser user, int userId) {
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.set("X-Restli-Protocol-Version", "2.0.0");
			headers.set("Authorization", "Bearer " + accessToken);
			HttpEntity<String> requestEntity = config.getHttpEntity(headers);
			ResponseEntity<JsonNode> responseEntity = restTemplate.exchange(
					"https://api.linkedin.com/v2/organizationAcls?q=roleAssignee", HttpMethod.GET, requestEntity,
					JsonNode.class);

			if (responseEntity.getStatusCode() == HttpStatus.OK) {
				JsonNode responseBody = responseEntity.getBody();
				JsonNode elementsNode = responseBody.path("elements");
				if (elementsNode.isEmpty()) {
					return null;
				} else {
					List<String> organizationUrns = new ArrayList<>();
					for (JsonNode pageNode : elementsNode) {
						String organizationURN = pageNode.path("organization").asText();
						System.out.println("organizationURN : "+organizationURN);
						organizationUrns.add(organizationURN);
					}
					List<String> organizationNames = getOrganizationName(accessToken, organizationUrns);
					List<Map<String, Object>> data = new ArrayList<>();
					for (int i = 0; i < organizationUrns.size(); i++) {
						Map<String, Object> map = config.getMap();
						String organizationId = organizationUrns.get(i)
								.substring(organizationUrns.get(i).lastIndexOf(':') + 1);

						map.put("urn", organizationId);
						map.put("accessToken", accessToken);
						map.put("name", organizationNames.get(i));
						String imageUrl = getOrganizationUrl(accessToken, organizationId);
						map.put("profile_image", imageUrl);
						if (imageUrl == null) {
							imageUrl = defaultImageUrl;
						}
						data.add(map);
					}
					System.out.println("list : " + organizationUrns);
					return data;
				}
			}
		} catch (Exception e) {
			throw new CommonException(e.getMessage());
		}
		return null;
	}

	private List<String> getOrganizationName(String accessToken, List<String> organizationUrns) {
		List<String> pageNames = new ArrayList<>();
		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", "Bearer " + accessToken);
		HttpEntity<String> entity = config.getHttpEntity(headers);

		for (String organizationUrn : organizationUrns) {
			String organizationId = organizationUrn.substring(organizationUrn.lastIndexOf(':') + 1);

			try {
				ResponseEntity<String> responseEntity = restTemplate.exchange(
						"https://api.linkedin.com/v2/organizations/" + organizationId, HttpMethod.GET, entity,
						String.class);
				if (responseEntity.getStatusCode() == HttpStatus.OK) {
					JsonNode rootNode = objectMapper.readTree(responseEntity.getBody());
					String pageName = rootNode.path("localizedName").asText();
					System.out.println(organizationId + " " + pageName);
					pageNames.add(pageName);
				} else {
					pageNames.add(null);
				}
			} catch (Exception e) {
				throw new CommonException(e.getMessage());
			}
		}
		return pageNames;
	}
	
	public ResponseEntity<Object> saveSelectedPage(LinkedInPageDto pages, QuantumShareUser user, int userId) {
		ResponseStructure<Map<String, Object>> response = new ResponseStructure<>();
		if (user == null) {
			response.setCode(HttpStatus.UNAUTHORIZED.value());
			response.setMessage("User not authenticated");
			response.setStatus("error");
			response.setData(Collections.emptyMap()); // Empty data for error case
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
		}
		if (pages == null || pages.getLinkedinPageURN() == null || pages.getLinkedinPageName() == null
				|| pages.getLinkedinPageAccessToken() == null) {
			response.setCode(HttpStatus.BAD_REQUEST.value());
			response.setMessage("Invalid selected page data");
			response.setStatus("error");
			response.setData(Collections.emptyMap());
			return ResponseEntity.badRequest().body(response);
		}

		int networkSizeResponse = getNetworkSize(pages.getLinkedinPageAccessToken(), pages.getLinkedinPageURN());
		SocialAccounts socialAccounts = user.getSocialAccounts();
		pages.setLinkedinPageFollowers(networkSizeResponse);
		if (socialAccounts == null) {
			socialAccounts = new SocialAccounts();
			socialAccounts.setLinkedInPages(pages);
			socialAccounts.setLinkedInPagePresent(true);
			user.setSocialAccounts(accounts);
		} else if (socialAccounts.getLinkedInPages() == null) {
			socialAccounts.setLinkedInPages(pages);
			socialAccounts.setLinkedInPagePresent(true);
			user.setSocialAccounts(socialAccounts);
		}else {
			LinkedInPageDto exPage = socialAccounts.getLinkedInPages();	
			exPage.setLinkedinPageAccessToken(pages.getLinkedinPageAccessToken());
			exPage.setLinkedinPageFollowers(pages.getLinkedinPageFollowers());
			exPage.setLinkedinPageImage(pages.getLinkedinPageImage());
			exPage.setLinkedinPageName(pages.getLinkedinPageName());
			exPage.setLinkedinPageURN(pages.getLinkedinPageURN());
			socialAccounts.setLinkedInPages(pages);
			user.setSocialAccounts(socialAccounts);
			userDao.save(user);
			
		}
		userDao.saveUser(user);
		response.setCode(HttpStatus.OK.value());
		response.setMessage(pages.getLinkedinPageName());
		response.setStatus("success");
		response.setPlatform("LinkedIn");

		Map<String, Object> responseData = config.getMap();
		responseData.clear();
		responseData.put("linkedInProfilePic", pages.getLinkedinPageImage());
		responseData.put("linkedInFollowersCount", pages.getLinkedinPageFollowers());
		responseData.put("linkedInUserName", pages.getLinkedinPageName());
		response.setData(responseData);

		return ResponseEntity.ok(response);
	}

	public int getNetworkSize(String accessToken, String organizationId) {

		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

		HttpEntity<String> entity = config.getHttpEntity(headers);

		String url = "https://api.linkedin.com/v2/networkSizes/urn:li:organization:" + organizationId
				+ "?edgeType=CompanyFollowedByMember";
		ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);

		if (response.getStatusCode().is2xxSuccessful()) {
			Integer firstDegreeSize = response.getBody().path("firstDegreeSize").asInt();
			return firstDegreeSize;
		} else {
			return 0;
		}
	}

	public String getOrganizationUrl(String accessToken, String organizationId) {
		ResponseStructure<String> responseStructure = new ResponseStructure<String>();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Authorization", "Bearer " + accessToken);
		headers.set("LinkedIn-Version", "202405");
		headers.set("X-Restli-Protocol-Version", "2.0.0");
		HttpEntity<String> entity = config.getHttpEntity(headers);
		try {
			ResponseEntity<JsonNode> response = restTemplate.exchange(
					"https://api.linkedin.com/v2/organizations/" + organizationId
							+ "?projection=(logoV2(original~:playableStreams,cropped~:playableStreams,cropInfo))",
					HttpMethod.GET, entity, JsonNode.class);
			String logoUrl = "https://quantumshare.quantumparadigm.in/vedio/ProfilePicture.jpg"; // default image URL
			System.out.println(response.getBody());
			if (response.getStatusCode() == HttpStatus.OK) {
				JsonNode elementsNode = response.getBody().path("logoV2").path("original~").path("elements");
				if (elementsNode.isArray() && elementsNode.size() > 0) {
					JsonNode firstElement = elementsNode.get(0);
					JsonNode identifiersNode = firstElement.path("identifiers");
					if (identifiersNode.isArray() && identifiersNode.size() > 0) {
						String fetchedLogoUrl = identifiersNode.get(0).path("identifier").asText();

						if (fetchedLogoUrl != null && !fetchedLogoUrl.isEmpty()) {
							logoUrl = fetchedLogoUrl; // use fetched logo URL if available
						}
					}
				}
				System.out.println(logoUrl);
				return logoUrl;
				
			} else {
				return defaultImageUrl;
			}
		} catch (Exception e) {
			responseStructure.setStatus("Failure");
			responseStructure.setMessage("Exception occurred: " + e.getMessage());
			responseStructure.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
			responseStructure.setData("https://quantumshare.quantumparadigm.in/vedio/ProfilePicture.jpg"); // return
			responseStructure.setPlatform("linkedin");
		}
		return null;
	}

	public String generateAuthorizationUrl() {
        return "https://www.linkedin.com/oauth/v2/authorization" +
                "?response_type=code" +
                "&client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&scope=" + scope;
    }

	}




//	public ResponseEntity<ResponseStructure<String>> getUserInfoWithToken(String code, QuantumShareUser user,
//			int userId) throws IOException {
//		String accessToken = getAccessToken(code);
//		if (accessToken == null) {
//			ResponseStructure<String> structure = new ResponseStructure<String>();
//			structure.setCode(500);
//			structure.setMessage("Failed to retrieve access token");
//			structure.setStatus("error");
//			structure.setPlatform("LinkedIn");
//			structure.setData(null);
//			return new ResponseEntity<>(structure, HttpStatus.INTERNAL_SERVER_ERROR);
//		}
//
//		Map<String, Object> organizationAclsResponse = getProfileInfo(accessToken, user, userId);
//		if (!organizationAclsResponse.isEmpty()) {
//			ResponseStructure<String> structure = new ResponseStructure<String>();
//			structure.setCode(200);
//			structure.setMessage("LinkedIn Profile Connected Successfully");
//			structure.setStatus("success");
//			structure.setPlatform("LinkedIn");
//			structure.setData(organizationAclsResponse);
//			return new ResponseEntity<>(structure, HttpStatus.OK);
//		} else {
//			ResponseStructure<String> structure = new ResponseStructure<String>();
//			structure.setCode(500);
//			structure.setMessage("Failed to retrieve organization info");
//			structure.setStatus("error");
//			structure.setPlatform("LinkedIn");
//			structure.setData(null);
//			return new ResponseEntity<>(structure, HttpStatus.INTERNAL_SERVER_ERROR);
//		}
//	}

	
//page		
		