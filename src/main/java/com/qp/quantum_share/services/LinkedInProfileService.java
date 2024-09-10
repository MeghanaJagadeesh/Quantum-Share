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

	@Autowired
	ResponseStructure<String> structure;

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

	@Autowired
	ResponseStructure<String> responseStructure;

	@Autowired
	SocialAccounts accounts;

	@Autowired
	ObjectMapper objectMapper;

	public String generateAuthorizationUrl() {
		return "https://www.linkedin.com/oauth/v2/authorization" + "?response_type=code" + "&client_id=" + clientId
				+ "&redirect_uri=" + redirectUri + "&scope=" + scope;
	}

	public String exchangeAuthorizationCodeForAccessToken(String code) throws IOException {
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
		System.out.println("exchangeAuthorizationCodeForAccessToken");
		ResponseEntity<Map> responseEntity = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Map.class);
		Map<String, Object> responseBody = responseEntity.getBody();
		return responseBody != null ? (String) responseBody.get("access_token") : null;

	}

	public ResponseEntity<ResponseStructure<String>> getUserInfoWithToken(String code, QuantumShareUser user)
			throws IOException {
		String accessToken = exchangeAuthorizationCodeForAccessToken(code);
		if (accessToken == null) {
			structure.setCode(500);
			structure.setMessage("Failed to retrieve access token");
			structure.setStatus("error");
			structure.setPlatform("LinkedIn");
			structure.setData(null);
			return new ResponseEntity<>(structure, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		Map<String, Object> organizationAclsResponse = getProfileInfo(accessToken, user);

		if (!organizationAclsResponse.isEmpty()) {
			structure.setCode(200);
			structure.setMessage("LinkedIn Profile Connected Successfully");
			structure.setStatus("success");
			structure.setPlatform("LinkedIn");
			structure.setData(organizationAclsResponse);
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

	public Map<String, Object> getProfileInfo(String accessToken, QuantumShareUser user) {
		String userInfoUrl = "https://api.linkedin.com/v2/me";
		headers.setBearerAuth(accessToken);
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = config.getHttpEntity(headers);
		ResponseEntity<JsonNode> response = restTemplate.exchange(userInfoUrl, HttpMethod.GET, entity, JsonNode.class);

		if (response.getStatusCode() == HttpStatus.OK) {
			JsonNode responseBody = response.getBody();
			System.out.println("responseBody " + responseBody);
			Map<String, Object> map = config.getMap();
			map.clear();
			try {
				String localizedFirstName = responseBody.path("localizedFirstName").asText();
				String localizedLastName = responseBody.path("localizedLastName").asText();
				String id = responseBody.path("id").asText();

				// Fetch profile image URL
				ResponseStructure<String> profileImageResponse = getLinkedInProfile(accessToken);
				String imageUrl = null;

				if (profileImageResponse.getStatus().equals("Success")) {
					Object data = profileImageResponse.getData();
					if (data instanceof String) {
						imageUrl = (String) data;
					} else {
						imageUrl = "default_image_url";
					}
				} else {
					imageUrl = "default_image_url";
				}
//				map.put("profile_sub", id);
				map.put("linkedInUserName", localizedFirstName + " " + localizedLastName);
//				map.put("access_token", accessToken);
				map.put("linkedInProfilePic", imageUrl);

				linkedInProfileDto.setLinkedinProfileURN(id);
				linkedInProfileDto.setLinkedinProfileUserName(localizedFirstName + " " + localizedLastName);
				linkedInProfileDto.setLinkedinProfileAccessToken(accessToken);
				linkedInProfileDto.setLinkedinProfileImage(imageUrl); // Set profile image URL

				SocialAccounts socialAccounts = user.getSocialAccounts();
				if (socialAccounts == null) {

					accounts.setLinkedInProfileDto(linkedInProfileDto);
					user.setSocialAccounts(accounts);
				} else {
					if (socialAccounts.getLinkedInProfileDto() == null) {
						socialAccounts.setLinkedInProfileDto(linkedInProfileDto);
					}
				}

				userDao.saveUser(user);

				return map;
			} catch (Exception e) {
				throw new CommonException(e.getMessage());
			}
		} else {

			return null;
		}
	}

	public ResponseStructure<String> getLinkedInProfile(String accessToken) {
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
			} else {
				responseStructure.setStatus("Failure");
				responseStructure.setMessage("Failed to fetch profile: " + response.getStatusCode());
				responseStructure.setCode(response.getStatusCode().value());
				responseStructure.setData(imageUrl); // return default image URL even if response status is not OK
			}
		} catch (RestClientException e) {
			responseStructure.setStatus("Failure");
			responseStructure.setMessage("Exception occurred: " + e.getMessage());
			responseStructure.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
			responseStructure.setData("https://quantumshare.quantumparadigm.in/vedio/ProfilePicture.jpg"); // return
																											// exception
		}

		return responseStructure;
	}

	public ResponseEntity<ResponseStructure<List<LinkedInPageDto>>> getOrganizationsDetailsByProfile(String code,
			QuantumShareUser user) throws IOException {
		String accessToken = exchangeAuthorizationCodeForAccessToken(code);
		return getOrganizationInfo(accessToken);
	}

	public ResponseEntity<ResponseStructure<List<LinkedInPageDto>>> getOrganizationInfo(String accessToken) {
		headers.set("X-Restli-Protocol-Version", "2.0.0");
		headers.set("Authorization", "Bearer " + accessToken);
		HttpEntity<String> requestEntity = config.getHttpEntity(headers);
		ResponseEntity<String> responseEntity;
		System.out.println("getOrganizationInfo ");
		try {
			responseEntity = restTemplate.exchange("https://api.linkedin.com/v2/organizationAcls?q=roleAssignee",
					HttpMethod.GET, requestEntity, String.class);
		} catch (Exception e) {
			ResponseStructure<List<LinkedInPageDto>> structure = new ResponseStructure<>();
			structure.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
			structure.setMessage("Failed to make request");
			structure.setStatus("error");
			structure.setPlatform("LinkedIn");
			structure.setData(null);
			return new ResponseEntity<>(structure, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		ResponseStructure<List<LinkedInPageDto>> structure = new ResponseStructure<>();
		if (responseEntity.getStatusCode() == HttpStatus.OK) {
			String responseBody = responseEntity.getBody();

			try {
				JsonNode rootNode = objectMapper.readTree(responseBody);
				JsonNode elementsNode = rootNode.path("elements");

				if (elementsNode.isEmpty()) {
					structure.setCode(HttpStatus.OK.value());
					structure.setMessage("User does not have associated pages");
					structure.setStatus("success");
					structure.setPlatform("LinkedIn");
					structure.setData(null);
					return ResponseEntity.ok(structure);
				} else {
					List<String> organizationUrns = new ArrayList<>();
					for (JsonNode pageNode : elementsNode) {
						String organizationURN = pageNode.path("organization").asText();
						organizationUrns.add(organizationURN);
					}
					List<String> organizationNames = getPageNamesFromLinkedInAPI(accessToken, organizationUrns);
					List<LinkedInPageDto> data = new ArrayList<>();
					for (int i = 0; i < organizationUrns.size(); i++) {
						linkedInPageDto.setLinkedinPageURN(organizationUrns.get(i));
						linkedInPageDto.setLinkedinPageAccessToken(accessToken);
						linkedInPageDto.setLinkedinPageName(organizationNames.get(i));
						data.add(linkedInPageDto);
					}
					structure.setCode(HttpStatus.OK.value());
					structure.setMessage("LinkedIn associated pages");
					structure.setStatus("success");
					structure.setPlatform("LinkedIn");
					structure.setData(data);
					return ResponseEntity.ok(structure);
				}
			} catch (JsonProcessingException e) {
				ResponseStructure<List<LinkedInPageDto>> errorResponse = new ResponseStructure<>();
				errorResponse.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
				errorResponse.setMessage("Failed to process JSON response");
				errorResponse.setStatus("error");
				errorResponse.setPlatform("LinkedIn");
				errorResponse.setData(null);
				return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
			}
		} else {
			structure.setCode(responseEntity.getStatusCode().value());
			structure.setMessage("Failed to retrieve organization info");
			structure.setStatus("error");
			structure.setPlatform("LinkedIn");
			structure.setData(null);
			return new ResponseEntity<>(structure, responseEntity.getStatusCode());
		}
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

	public ResponseEntity<ResponseStructure<Map<String, Object>>> saveSelectedPage(
			LinkedInPageDto selectedLinkedInPageDto, QuantumShareUser user) {
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

		ResponseStructure<String> logoResponse = getOrganizationLogo(
				selectedLinkedInPageDto.getLinkedinPageAccessToken(), organizationId);

		ResponseStructure<Integer> networkSizeResponse = getNetworkSize(
				selectedLinkedInPageDto.getLinkedinPageAccessToken(), organizationId);

		if (logoResponse.getCode() == HttpStatus.OK.value() && networkSizeResponse.getCode() == HttpStatus.OK.value()) {
			String logoUrl = (String) logoResponse.getData();
			Integer firstDegreeSize = (Integer) networkSizeResponse.getData();
			selectedLinkedInPageDto.setLinkedinPageImage(logoUrl);
			selectedLinkedInPageDto.setLinkedinPageFollowers(firstDegreeSize);

			SocialAccounts socialAccounts = user.getSocialAccounts();
			if (socialAccounts == null) {
				socialAccounts = accounts;
			}
			LinkedInProfileDto linkedInProfileDto = socialAccounts.getLinkedInProfileDto();
			if (linkedInProfileDto == null) {
				linkedInProfileDto = new LinkedInProfileDto();
				socialAccounts.setLinkedInProfileDto(linkedInProfileDto);
			}
			selectedLinkedInPageDto.setLinkedinPageURN(organizationId);
			selectedLinkedInPageDto.setProfile(linkedInProfileDto);
			List<LinkedInPageDto> pages = linkedInProfileDto.getPages();
			if (pages == null) {
				pages = new ArrayList<>();
			}
			pages.add(selectedLinkedInPageDto);
			linkedInProfileDto.setPages(pages);

			user.setSocialAccounts(socialAccounts);
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
		System.out.println(accessToken);
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

		HttpEntity<String> entity = config.getHttpEntity(headers);

		String url = "https://api.linkedin.com/v2/networkSizes/urn:li:organization:" + organizationId
				+ "?edgeType=CompanyFollowedByMember";
		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
		System.out.println("after");

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
			} else {
				responseStructure.setStatus("Failure");
				responseStructure.setMessage("Failed to fetch organization logo: " + response.getStatusCode());
				responseStructure.setCode(response.getStatusCode().value());
				responseStructure.setData(logoUrl); // return default image URL even if response status is not OK
			}
		} catch (Exception e) {
			responseStructure.setStatus("Failure");
			responseStructure.setMessage("Exception occurred: " + e.getMessage());
			responseStructure.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
			responseStructure.setData("https://quantumshare.quantumparadigm.in/vedio/ProfilePicture.jpg"); // return
		}
		return responseStructure;
	}

}
