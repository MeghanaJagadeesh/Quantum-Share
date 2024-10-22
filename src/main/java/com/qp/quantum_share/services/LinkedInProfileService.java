package com.qp.quantum_share.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
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
	HttpHeaders headers;

	@Autowired
	QuantumShareUserDao userDao;

	@Autowired
	ConfigurationClass config;

	@Autowired
	MultiValueMap<String, Object> body;

//	@Autowired
//	ResponseStructure<String> responseStructure;

	@Autowired
	SocialAccounts accounts;

	@Autowired
	ObjectMapper objectMapper;

	public String generateAuthorizationUrl() {
		return "https://www.linkedin.com/oauth/v2/authorization" + "?response_type=code" + "&client_id=" + clientId
				+ "&redirect_uri=" + redirectUri + "&scope=" + scope;
	}

	public String exchangeAuthorizationCodeForAccessToken(String code) throws IOException {
		System.out.println("2");
		String url = "https://www.linkedin.com/oauth/v2/accessToken";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("grant_type", "authorization_code");
		body.add("code", code);
		body.add("client_id", clientId);
		body.add("client_secret", clientSecret);
		body.add("redirect_uri", redirectUri);

		HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);
		ResponseEntity<Map> responseEntity = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Map.class);
		System.out.println("3");
		Map<String, Object> responseBody = responseEntity.getBody();
		return responseBody != null ? (String) responseBody.get("access_token") : null;

	}

//profile
	public ResponseEntity<ResponseStructure<String>> getUserInfoWithToken(String code, QuantumShareUser user,
			int userId) throws IOException {
		String accessToken = exchangeAuthorizationCodeForAccessToken(code);
		if (accessToken == null) {
			ResponseStructure<String> structure = new ResponseStructure<String>();
			structure.setCode(500);
			structure.setMessage("Failed to retrieve access token");
			structure.setStatus("error");
			structure.setPlatform("LinkedIn");
			structure.setData(null);
			return new ResponseEntity<>(structure, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		Map<String, Object> organizationAclsResponse = getProfileInfo(accessToken, user, userId);
//		List<LinkedInPageDto> pages = getOrganizationsDetailsByProfile(code, user,
//				userId);
		if (!organizationAclsResponse.isEmpty()) {
			ResponseStructure<String> structure = new ResponseStructure<String>();
			structure.setCode(200);
			structure.setMessage("LinkedIn Profile Connected Successfully");
			structure.setStatus("success");
			structure.setPlatform("LinkedIn");
			structure.setData(organizationAclsResponse);
			return new ResponseEntity<>(structure, HttpStatus.OK);
		} else {
			ResponseStructure<String> structure = new ResponseStructure<String>();
			structure.setCode(500);
			structure.setMessage("Failed to retrieve organization info");
			structure.setStatus("error");
			structure.setPlatform("LinkedIn");
			structure.setData(null);
			return new ResponseEntity<>(structure, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	public Map<String, Object> getProfileInfo(String accessToken, QuantumShareUser user, int userId) {
		System.out.println("6");
		String userInfoUrl = "https://api.linkedin.com/v2/me";
		headers.setBearerAuth(accessToken);
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = config.getHttpEntity(headers);
		ResponseEntity<JsonNode> response = restTemplate.exchange(userInfoUrl, HttpMethod.GET, entity, JsonNode.class);

		if (response.getStatusCode() == HttpStatus.OK) {
			JsonNode responseBody = response.getBody();
			Map<String, Object> map = config.getMap();
			map.clear();
			try {
				String localizedFirstName = responseBody.path("localizedFirstName").asText();
				String localizedLastName = responseBody.path("localizedLastName").asText();
				String id = responseBody.path("id").asText();

				// Fetch profile image URL
				ResponseStructure<String> profileImageResponse = getLinkedInProfile(accessToken);
				System.out.println("8");
				String imageUrl = null;

				if (profileImageResponse.getStatus().equals("Success")) {
					Object data = profileImageResponse.getData();
					if (data instanceof String) {
						imageUrl = (String) data;
					} else {
						imageUrl = defaultImageUrl;
					}
				} else {
					imageUrl = defaultImageUrl;
				}
				map.put("urn", id);
				map.put("name", localizedFirstName + " " + localizedLastName);
				map.put("accessToken", accessToken);
				map.put("profile_image", imageUrl);

				return map;
			} catch (Exception e) {
				throw new CommonException(e.getMessage());
			}
		} else {
			return null;
		}
	}

//page	
	public List<Map<String, Object>> getOrganizationsDetailsByProfile(String accessToken, QuantumShareUser user,
			int userId) {
		System.out.println("9");
		return getOrganizationInfo(accessToken, user, userId);
	}

	public List<Map<String, Object>> getOrganizationInfo(String accessToken, QuantumShareUser user, int userId) {
		System.out.println("10");
		headers.set("X-Restli-Protocol-Version", "2.0.0");
		headers.set("Authorization", "Bearer " + accessToken);
		HttpEntity<String> requestEntity = config.getHttpEntity(headers);
		ResponseEntity<String> responseEntity;
		try {
			responseEntity = restTemplate.exchange("https://api.linkedin.com/v2/organizationAcls?q=roleAssignee",
					HttpMethod.GET, requestEntity, String.class);
		} catch (Exception e) {
			throw new CommonException(e.getMessage());
		}

		if (responseEntity.getStatusCode() == HttpStatus.OK) {
			String responseBody = responseEntity.getBody();
			System.out.println("response body : " + responseBody);
			try {
				JsonNode rootNode = objectMapper.readTree(responseBody);
				JsonNode elementsNode = rootNode.path("elements");
				if (elementsNode.isEmpty()) {
					return null;
				} else {
					List<String> organizationUrns = new ArrayList<>();
					System.out.println("elementsNode " + elementsNode);
					for (JsonNode pageNode : elementsNode) {
						String organizationURN = pageNode.path("organization").asText();
						System.out.println(organizationURN);
						organizationUrns.add(organizationURN);
					}
					List<String> organizationNames = getPageNamesFromLinkedInAPI(accessToken, organizationUrns);
					System.out.println(organizationUrns + " \n" + organizationNames);
					List<Map<String, Object>> data = new ArrayList<>();
					for (int i = 0; i < organizationUrns.size(); i++) {

						System.out.println(i);
						Map<String, Object> map = config.getMap();
						String organizationId = organizationUrns.get(i)
								.substring(organizationUrns.get(i).lastIndexOf(':') + 1);

						map.put("urn", organizationId);
						map.put("accessToken", accessToken);
						map.put("name", organizationNames.get(i));
						System.out.println("11");
						ResponseStructure<String> logoResponse = getOrganizationLogo(accessToken, organizationId);
						System.out.println("14");
						System.out.println("logo " + logoResponse);
						if (logoResponse.getCode() == HttpStatus.OK.value()) {
							String logoUrl = logoResponse.getData().toString();

							map.put("profile_image", logoUrl);
						}

						data.add(map);
					}
					return data;
				}
			} catch (JsonProcessingException e) {
				throw new org.springframework.boot.json.JsonParseException();
			}
		} else {
			return null;
		}
	}

	public ResponseStructure<String> getLinkedInProfile(String accessToken) {
		System.out.println("7");
		ResponseStructure<String> responseStructure = new ResponseStructure<String>();
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
									imageUrl = fetchedImageUrl; // use fetched image URL if available
									break;
								}
							}
						}
					}
				}

				responseStructure.setStatus("Success");
				responseStructure.setMessage("Profile fetched successfully");
				responseStructure.setCode(HttpStatus.OK.value());
				responseStructure.setData(imageUrl);
				responseStructure.setPlatform("linkedin");
			} else {
				responseStructure.setStatus("Failure");
				responseStructure.setMessage("Failed to fetch profile: " + response.getStatusCode());
				responseStructure.setCode(response.getStatusCode().value());
				responseStructure.setData(imageUrl); // return default image URL even if response status is not OK
				responseStructure.setPlatform("linkedin");
			}
		} catch (RestClientException e) {
			responseStructure.setStatus("Failure");
			responseStructure.setMessage("Exception occurred: " + e.getMessage());
			responseStructure.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
			responseStructure.setData("https://quantumshare.quantumparadigm.in/vedio/ProfilePicture.jpg"); // return
			responseStructure.setPlatform("linkedin"); // exception
		}

		return responseStructure;
	}

	private List<String> getPageNamesFromLinkedInAPI(String accessToken, List<String> organizationUrns) {
		List<String> pageNames = new ArrayList<>();

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
					pageNames.add(null); // Add null for failed requests
				}
			} catch (Exception e) {
				throw new CommonException(e.getMessage());
			}
		}
		return pageNames;
	}

	public ResponseEntity<Object> saveSelectedPage(LinkedInPageDto selectedLinkedInPageDto, QuantumShareUser user,
			int userId) {
		System.out.println("selectedLinkedInPageDto  " + selectedLinkedInPageDto);
		ResponseStructure<Map<String, Object>> response = new ResponseStructure<>();
		if (user == null) {
			response.setCode(HttpStatus.UNAUTHORIZED.value());
			response.setMessage("User not authenticated");
			response.setStatus("error");
			response.setData(Collections.emptyMap()); // Empty data for error case
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
		}
		if (selectedLinkedInPageDto == null || selectedLinkedInPageDto.getLinkedinPageURN() == null
				|| selectedLinkedInPageDto.getLinkedinPageName() == null
				|| selectedLinkedInPageDto.getLinkedinPageAccessToken() == null) {
			response.setCode(HttpStatus.BAD_REQUEST.value());
			response.setMessage("Invalid selected page data");
			response.setStatus("error");
			response.setData(Collections.emptyMap()); // Empty data for error case
			return ResponseEntity.badRequest().body(response);
		}

		String organizationId = selectedLinkedInPageDto.getLinkedinPageURN()
				.substring(selectedLinkedInPageDto.getLinkedinPageURN().lastIndexOf(':') + 1);

		System.out.println("accesstoken  " + selectedLinkedInPageDto.getLinkedinPageAccessToken());
		System.out.println("id  " + organizationId);
		ResponseStructure<String> logoResponse = getOrganizationLogo(
				selectedLinkedInPageDto.getLinkedinPageAccessToken(), organizationId);
		System.out.println("logo " + logoResponse);
		ResponseStructure<Integer> networkSizeResponse = getNetworkSize(
				selectedLinkedInPageDto.getLinkedinPageAccessToken(), organizationId);

		if (logoResponse.getCode() == HttpStatus.OK.value() && networkSizeResponse.getCode() == HttpStatus.OK.value()) {
			String logoUrl = (String) logoResponse.getData();
			Integer firstDegreeSize = (Integer) networkSizeResponse.getData();
			selectedLinkedInPageDto.setLinkedinPageImage(logoUrl);
			selectedLinkedInPageDto.setLinkedinPageFollowers(firstDegreeSize);
			selectedLinkedInPageDto.setLinkedinPageURN(organizationId);

			SocialAccounts socialAccounts = user.getSocialAccounts();
			if (socialAccounts == null) {
				System.out.println("account null");
				socialAccounts = new SocialAccounts();
				socialAccounts.setLinkedInPages(selectedLinkedInPageDto);
				socialAccounts.setLinkedInPagePresent(true);
				user.setSocialAccounts(accounts);
			} else if (socialAccounts.getLinkedInPages() == null) {
				socialAccounts.setLinkedInPages(selectedLinkedInPageDto);
				socialAccounts.setLinkedInPagePresent(true);
				user.setSocialAccounts(socialAccounts);
			}
			userDao.saveUser(user);
			response.setCode(HttpStatus.OK.value());
			response.setMessage(selectedLinkedInPageDto.getLinkedinPageName());
			response.setStatus("success");
			response.setPlatform("LinkedIn");

			Map<String, Object> responseData = config.getMap();
			responseData.clear();
			responseData.put("linkedInProfilePic", selectedLinkedInPageDto.getLinkedinPageImage());
			responseData.put("linkedInFollowersCount", selectedLinkedInPageDto.getLinkedinPageFollowers());
			responseData.put("linkedInUserName", selectedLinkedInPageDto.getLinkedinPageName());
			response.setData(responseData);

			return ResponseEntity.ok(response);
		} else {
			int errorCode = (logoResponse.getCode() != HttpStatus.OK.value()) ? logoResponse.getCode()
					: networkSizeResponse.getCode();
			String errorMessage = (logoResponse.getCode() != HttpStatus.OK.value()) ? logoResponse.getMessage()
					: networkSizeResponse.getMessage();

			response.setCode(errorCode);
			response.setMessage("Failed to fetch organization details: " + errorMessage);
			response.setStatus("error");
			response.setData(Collections.emptyMap()); // Empty data for error case

			return ResponseEntity.status(errorCode).body(response);
		}
	}

	public ResponseStructure<Integer> getNetworkSize(String accessToken, String organizationId) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

		HttpEntity<String> entity = config.getHttpEntity(headers);

		String url = "https://api.linkedin.com/v2/networkSizes/urn:li:organization:" + organizationId
				+ "?edgeType=CompanyFollowedByMember";
		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

		ResponseStructure<Integer> responseStructure = new ResponseStructure<>();
		if (response.getStatusCode().is2xxSuccessful()) {
			try {
				JsonNode rootNode = objectMapper.readTree(response.getBody());
				Integer firstDegreeSize = rootNode.path("firstDegreeSize").asInt();
				responseStructure.setMessage("Network size retrieved successfully");
				responseStructure.setStatus("OK");
				responseStructure.setCode(response.getStatusCode().value());
				responseStructure.setPlatform("LinkedIn");
				responseStructure.setData(firstDegreeSize);
				return responseStructure;
			} catch (JsonProcessingException e) {
				throw new CommonException(e.getMessage());
			}
		} else {
			responseStructure.setMessage("Failed to retrieve network size");
			responseStructure.setStatus("Error");
			responseStructure.setCode(response.getStatusCode().value());
			responseStructure.setPlatform("LinkedIn");
			responseStructure.setData(null);
		}

		return responseStructure;
	}

	public ResponseStructure<String> getOrganizationLogo(String accessToken, String organizationId) {
		ResponseStructure<String> responseStructure = new ResponseStructure<String>();
		System.out.println("12");
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Authorization", "Bearer " + accessToken);
		headers.set("LinkedIn-Version", "202405");
		headers.set("X-Restli-Protocol-Version", "2.0.0");

		HttpEntity<String> entity = config.getHttpEntity(headers);

		try {
			ResponseEntity<String> response = restTemplate.exchange(
					"https://api.linkedin.com/v2/organizations/" + organizationId
							+ "?projection=(logoV2(original~:playableStreams,cropped~:playableStreams,cropInfo))",
					HttpMethod.GET, entity, String.class);
			System.out.println("13");
			String logoUrl = "https://quantumshare.quantumparadigm.in/vedio/ProfilePicture.jpg"; // default image URL

			if (response.getStatusCode() == HttpStatus.OK) {
				JsonNode rootNode = objectMapper.readTree(response.getBody());
				JsonNode elementsNode = rootNode.path("logoV2").path("original~").path("elements");
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
				responseStructure.setStatus("Success");
				responseStructure.setMessage("Organization logo fetched successfully");
				responseStructure.setCode(HttpStatus.OK.value());
				responseStructure.setData(logoUrl);
				responseStructure.setPlatform("linkedin");
			} else {
				responseStructure.setStatus("Failure");
				responseStructure.setMessage("Failed to fetch organization logo: " + response.getStatusCode());
				responseStructure.setCode(response.getStatusCode().value());
				responseStructure.setData(logoUrl); // return default image URL even if response status is not OK
				responseStructure.setPlatform("linkedin");
			}
		} catch (Exception e) {
			responseStructure.setStatus("Failure");
			responseStructure.setMessage("Exception occurred: " + e.getMessage());
			responseStructure.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
			responseStructure.setData("https://quantumshare.quantumparadigm.in/vedio/ProfilePicture.jpg"); // return
			responseStructure.setPlatform("linkedin");
		}
		return responseStructure;
	}

	public ResponseEntity<?> getPagesAndProfile(String code, QuantumShareUser user, int userId) {
		String accessToken;
		try {
			System.out.println("1");
			accessToken = exchangeAuthorizationCodeForAccessToken(code);
			System.out.println("4");
		} catch (IOException e) {
			throw new CommonException(e.getMessage());
		}
		if (accessToken == null) {
			ResponseStructure<String> structure = new ResponseStructure<String>();
			structure.setCode(500);
			structure.setMessage("Failed to retrieve access token");
			structure.setStatus("error");
			structure.setPlatform("LinkedIn");
			structure.setData(null);
			return new ResponseEntity<>(structure, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		System.out.println("5");
		Map<String, Object> profile = getProfileInfo(accessToken, user, userId);
		System.out.println("8");
		List<Map<String, Object>> pages = getOrganizationsDetailsByProfile(accessToken, user, userId);
		System.out.println("15");
		Map<String, Object> data = config.getMap();
		data.clear();
		if (!profile.isEmpty()) {
			data.put("linkedInProfile", profile);
			if(pages==null) {
				data.put("linkedInPages", null);
			}else if(pages.isEmpty()) {
				data.put("linkedInPages", null);
			}else {
				data.put("linkedInPages", pages);
			}
			ResponseStructure<String> structure = new ResponseStructure<String>();
			structure.setCode(200);
			structure.setMessage("LinkedIn Profile Connected Successfully");
			structure.setStatus("success");
			structure.setPlatform("LinkedIn");
			structure.setData(data);
			return new ResponseEntity<>(structure, HttpStatus.OK);
		} else {
			ResponseStructure<String> structure = new ResponseStructure<String>();
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
}
