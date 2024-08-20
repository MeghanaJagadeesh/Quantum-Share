package com.qp.quantum_share.services;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qp.quantum_share.configuration.ConfigurationClass;
import com.qp.quantum_share.dao.QuantumShareUserDao;
import com.qp.quantum_share.dao.TelegramUserDao;
import com.qp.quantum_share.dto.MediaPost;
import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.dto.SocialAccounts;
import com.qp.quantum_share.dto.TelegramUser;
import com.qp.quantum_share.response.ErrorResponse;
import com.qp.quantum_share.response.ResponseStructure;
import com.qp.quantum_share.response.ResponseWrapper;
import com.qp.quantum_share.response.SuccessResponse;

@Service
public class TelegramService {

	@Autowired
	ResponseStructure<String> structure;

	@Autowired
	SuccessResponse successResponse;

	@Autowired
	ErrorResponse errorResponse;

	@Autowired
	SecureRandom secureRandom;

	@Autowired
	StringBuilder stringBuilder;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	QuantumShareUserDao userDao;

	@Autowired
	TelegramUserDao telegramUserDao;

	@Autowired
	ConfigurationClass config;

	@Autowired
	HttpHeaders headers;

	@Autowired
	ConfigurationClass.ByteArrayResourceFactory byteArrayResourceFactory;

	@Autowired
	RestTemplate restTemplate;

	@Autowired
	ApplicationContext applicationContext;

	@Autowired
	SocialAccounts socialAccounts;

	@Autowired
	TelegramUser telegramUser;

	@Value("${telegram.bot.token}")
	String telegramBotToken;

	// Telegram Connection
	private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

	public ResponseEntity<ResponseStructure<String>> generateTelegramCode(QuantumShareUser user) {
		StringBuilder sb = applicationContext.getBean(StringBuilder.class);
		sb.append("QS-");
		for (int i = 0; i < 15; i++) {
			int index = secureRandom.nextInt(CHARACTERS.length());
			sb.append(CHARACTERS.charAt(index));
		}
		String telegramCode = sb.toString();
		
		saveTelegramCode(user, telegramCode);

		structure.setCode(HttpStatus.OK.value());
		structure.setMessage("Telegram code generated successfully");
		structure.setStatus("success");
		structure.setPlatform("telegram");
		structure.setData(telegramCode);
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
	}

	private void saveTelegramCode(QuantumShareUser user, String telegramCode) {
		SocialAccounts userSocialAccounts = user.getSocialAccounts();
		if (userSocialAccounts == null) {
			telegramUser.setTelegramCode(telegramCode);
			socialAccounts.setTelegramUser(telegramUser);
			user.setSocialAccounts(socialAccounts);
			userDao.save(user);
		} else if (userSocialAccounts.getTelegramUser() == null) {
			telegramUser.setTelegramCode(telegramCode);
			userSocialAccounts.setTelegramUser(telegramUser);
			user.setSocialAccounts(userSocialAccounts);
			userDao.save(user);
		} else {
			TelegramUser telUser = userSocialAccounts.getTelegramUser();
			telUser.setTelegramCode(telegramCode);
			userSocialAccounts.setTelegramUser(telUser);
			user.setSocialAccounts(userSocialAccounts);
			userDao.save(user);
		}
	}

	// Fetching Group Details
	public ResponseEntity<ResponseStructure<String>> pollTelegramUpdates(QuantumShareUser user) {
		String telegramCode = user.getSocialAccounts().getTelegramUser().getTelegramCode();
		String telegramApiUrl = "https://api.telegram.org/bot%s/getUpdates";
		String url = String.format(telegramApiUrl, telegramBotToken);

		ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

		if (response.getStatusCode() == HttpStatus.OK) {
			String responseBody = response.getBody();
			try {
				String telegramProfileUrl = "https://quantumshare.quantumparadigm.in/vedio/ProfilePicture.jpg";
				JsonNode rootNode = objectMapper.readTree(responseBody);
				JsonNode resultArray = rootNode.get("result");
				boolean codeFound = false;
				long telegramChatId = 0;
				String telegramGroupName = "";
				int telegramGroupMembersCount = 0;

				for (JsonNode updateNode : resultArray) {
					if (updateNode.has("message")) {
						JsonNode messageNode = updateNode.get("message");
						JsonNode chatNode = messageNode.get("chat");
						telegramChatId = chatNode.get("id").asLong();
						telegramGroupName = chatNode.has("title") ? chatNode.get("title").asText() : "";
						telegramGroupMembersCount = getGroupMembersCount(telegramChatId);
						if (messageNode.has("text")) {
							String text = messageNode.get("text").asText();
							if (text.contains(telegramCode)) {
								codeFound = true;
								try {
									String successMessage = "Success!";
									sendMessageToGroup(telegramChatId, successMessage);
									telegramGroupMembersCount = getGroupMembersCount(telegramChatId);
								} catch (HttpClientErrorException.BadRequest e) {
									System.err.println("Error sending message: " + e.getMessage());
								}
							}
						}
						if (messageNode.has("new_chat_photo")) {
							JsonNode newChatPhotoArray = messageNode.get("new_chat_photo");
							JsonNode largestPhoto = newChatPhotoArray.get(newChatPhotoArray.size() - 1);
							String fileId = largestPhoto.get("file_id").asText();
							telegramProfileUrl = getPhotoUrl(fileId);
						}
					}
				}
				if (codeFound) {
					saveGroupInfo(user, telegramChatId, telegramGroupName, telegramGroupMembersCount,
							telegramProfileUrl);
					structure.setCode(HttpStatus.CREATED.value());
					structure.setMessage("Telegram Connected Successfully");
					structure.setStatus("success");
					structure.setPlatform("telegram");
					Map<String, Object> data = config.getMap();
					TelegramUser dataUser = user.getSocialAccounts().getTelegramUser();
					data.put("telegramChatId", dataUser.getTelegramChatId());
					data.put("telegramGroupName", dataUser.getTelegramGroupName());
					data.put("telegramProfileUrl", dataUser.getTelegramProfileUrl());
					data.put("telegramGroupMembersCount", dataUser.getTelegramGroupMembersCount());
					structure.setData(data);
					return new ResponseEntity<>(structure, HttpStatus.CREATED);
				} else {
					structure.setCode(HttpStatus.BAD_REQUEST.value());
					structure.setMessage("Please paste the Generated Code in your Group or Channel");
					structure.setStatus("error");
					structure.setPlatform("telegram");
					structure.setData(null);
					return new ResponseEntity<>(structure, HttpStatus.BAD_REQUEST);
				}
			} catch (IOException e) {
				e.printStackTrace();
				structure.setData(null);
				structure.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
				structure.setMessage("Error processing Telegram updates");
				structure.setPlatform(null);
				structure.setStatus("fail");
				return new ResponseEntity<>(structure, HttpStatus.INTERNAL_SERVER_ERROR);
			}
		} else {
			System.err.println("Failed to fetch updates from Telegram. Status code: " + response.getStatusCode());
			structure.setData(null);
			structure.setCode(HttpStatus.BAD_REQUEST.value());
			structure.setMessage("Failed to fetch updates from Telegram");
			structure.setPlatform(null);
			structure.setStatus("fail");
			return new ResponseEntity<>(structure, HttpStatus.BAD_REQUEST);
		}
	}

	public void sendMessageToGroup(long telgramChatId, String message) {
		String telegramApiUrl = "https://api.telegram.org/bot%s/sendMessage";
		String url = String.format(telegramApiUrl, telegramBotToken);

		headers.setContentType(MediaType.APPLICATION_JSON);

		Map<String, Object> body = config.getMap();
		body.put("chat_id", telgramChatId);
		body.put("text", message);

		HttpEntity<Map<String, Object>> responseEntity = config.getMapHttpEntity(body, headers);
		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, responseEntity, String.class);

		if (response.getStatusCode() == HttpStatus.OK) {
			System.out.println("Message sent successfully!");
		} else {
			System.err.println("Failed to send photo. Status code: " + response.getStatusCode());
		}
	}

	public int getGroupMembersCount(long telgramChatId) {
		String telegramApiUrl = "https://api.telegram.org/bot%s/getChatMembersCount";
		String url = String.format(telegramApiUrl, telegramBotToken) + "?chat_id=" + telgramChatId;

		ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
		if (response.getStatusCode() == HttpStatus.OK) {
			String responseBody = response.getBody();
			try {
				JsonNode rootNode = objectMapper.readTree(responseBody);
				JsonNode result = rootNode.get("result");
				if (result != null) {
					return result.asInt();
				} else {
					System.err.println("Chat members count not found in the JSON response.");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			System.err.println("Error fetching chat members count. Status code: " + response.getStatusCode());
		}
		return -1;
	}

	public String getPhotoUrl(String fileId) {
		String telegramApiUrl = "https://api.telegram.org/bot%s/getFile";
		String url = String.format(telegramApiUrl, telegramBotToken) + "?file_id=" + fileId;

		ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
		if (response.getStatusCode() == HttpStatus.OK) {
			String responseBody = response.getBody();
			try {
				JsonNode rootNode = objectMapper.readTree(responseBody);
				JsonNode result = rootNode.get("result");
				if (result != null && result.has("file_path")) {
					String filePath = result.get("file_path").asText();
					return String.format("https://api.telegram.org/file/bot%s/%s", telegramBotToken, filePath);
				} else {
					System.err.println("File path not found in the JSON response.");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			System.err.println("Error fetching file information. Status code: " + response.getStatusCode());
		}
		return null;
	}

	public void saveGroupInfo(QuantumShareUser user, long telegramChatId, String telegramGroupName,
			int telegramGroupMembersCount, String telegramProfileUrl) {
		SocialAccounts userSocialAccounts = user.getSocialAccounts();
		if (userSocialAccounts == null) {
			user.setSocialAccounts(socialAccounts);
		}
		TelegramUser userTelegramUser = userSocialAccounts.getTelegramUser();
		if (userTelegramUser == null) {
			userTelegramUser = telegramUser;
			userSocialAccounts.setTelegramUser(userTelegramUser);
		}
		userTelegramUser.setTelegramChatId(telegramChatId);
		userTelegramUser.setTelegramGroupName(telegramGroupName);
		userTelegramUser.setTelegramGroupMembersCount(telegramGroupMembersCount);
		userTelegramUser.setTelegramProfileUrl(telegramProfileUrl);
		userDao.save(user);
	}

	// Media Posting
	public ResponseEntity<ResponseWrapper> postMediaToGroup(MediaPost mediaPost, MultipartFile mediaFile,
			TelegramUser telegramUser, QuantumShareUser user) {
		if (telegramUser == null) {
			structure.setMessage("Telegram user not found");
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setPlatform("telegram");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(structure), HttpStatus.NOT_FOUND);
		}
		int telegramId = telegramUser.getTelegramId();
		long telgramChatId = telegramUser.getTelegramChatId();
		String contentType = mediaFile.getContentType();
		try {
			if (contentType != null && contentType.startsWith("image/")) {
				return sendPhotoToGroup(telegramId, telgramChatId, mediaFile, mediaPost.getCaption(), user);
			} else if (contentType != null && contentType.startsWith("video/")) {
				return sendVideoToGroup(telegramId, telgramChatId, mediaFile, mediaPost.getCaption(), user);
			} else {
				structure.setMessage("Unsupported media type");
				structure.setCode(HttpStatus.BAD_REQUEST.value());
				structure.setPlatform("telegram");
				structure.setStatus("error");
				structure.setData(null);
				return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(structure),
						HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			errorResponse.setMessage("Failed to send media: " + e.getMessage());
			errorResponse.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
			errorResponse.setPlatform("telegram");
			errorResponse.setStatus("error");
			errorResponse.setData(null);
			return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(errorResponse),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	public ResponseEntity<ResponseWrapper> sendPhotoToGroup(int telegramId, long telgramChatId, MultipartFile mediaFile,
			String caption, QuantumShareUser user) throws IOException {
		String telegramApiPhotoUrl = "https://api.telegram.org/bot%s/sendPhoto";
		String url = String.format(telegramApiPhotoUrl, telegramBotToken);

		headers.setContentType(MediaType.MULTIPART_FORM_DATA);

		MultiValueMap<String, Object> body = new LinkedMultiValueMap<String, Object>();
		body.add("chat_id", telgramChatId);
		body.add("caption", caption);
		body.add("photo", byteArrayResourceFactory.createByteArrayResource(mediaFile.getBytes(),
				mediaFile.getOriginalFilename()));

		HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

		if (response.getStatusCode().is2xxSuccessful()) {
			user.setCredit(user.getCredit() - 1);
			userDao.save(user);
			successResponse.setMessage("Posted On Telegram");
			successResponse.setCode(HttpStatus.OK.value());
			successResponse.setPlatform("telegram");
			successResponse.setStatus("success");
			successResponse.setData(null);
			successResponse.setRemainingCredits(user.getCredit());
			return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(successResponse), HttpStatus.OK);

		} else {
			errorResponse.setMessage("Request Failed to process");
			errorResponse.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
			errorResponse.setPlatform("telegram");
			errorResponse.setStatus("error");
			errorResponse.setData(null);
			return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(errorResponse),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	public ResponseEntity<ResponseWrapper> sendVideoToGroup(int telegramId, long telgramChatId, MultipartFile mediaFile,
			String caption, QuantumShareUser user) throws IOException {
		String telegramApiVideoUrl = "https://api.telegram.org/bot%s/sendVideo";
		String url = String.format(telegramApiVideoUrl, telegramBotToken);

		headers.setContentType(MediaType.MULTIPART_FORM_DATA);

		MultiValueMap<String, Object> body = new LinkedMultiValueMap<String, Object>();
		body.add("chat_id", telgramChatId);
		body.add("caption", caption);
		body.add("video", byteArrayResourceFactory.createByteArrayResource(mediaFile.getBytes(),
				mediaFile.getOriginalFilename()));

		HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
		ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, JsonNode.class);

		if (response.getStatusCode().is2xxSuccessful()) {
			user.setCredit(user.getCredit() - 1);
			userDao.save(user);
			successResponse.setMessage("Posted On Telegram");
			successResponse.setCode(HttpStatus.OK.value());
			successResponse.setPlatform("telegram");
			successResponse.setStatus("success");
			successResponse.setData(null);
			successResponse.setRemainingCredits(user.getCredit());
			return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(successResponse), HttpStatus.OK);

		} else {
			errorResponse.setMessage("Request Failed to process");
			errorResponse.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
			errorResponse.setPlatform("telegram");
			errorResponse.setStatus("error");
			errorResponse.setData(null);
			return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(errorResponse),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

}