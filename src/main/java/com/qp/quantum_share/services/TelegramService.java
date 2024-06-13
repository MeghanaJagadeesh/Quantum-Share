package com.qp.quantum_share.services;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
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
import com.qp.quantum_share.response.ResponseStructure;
import com.qp.quantum_share.response.ResponseWrapper;

@Service
public class TelegramService {

	@Autowired
	ResponseStructure<String> structure;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	SocialAccounts socialAccounts;

	@Autowired
	QuantumShareUserDao userDao;

	@Autowired
	TelegramUserDao telegramUserDao;

	@Autowired
	ConfigurationClass config;

	@Autowired
	HttpHeaders headers;

	@Value("${telegram.bot.token}")
	private String telegramBotToken;

	private final RestTemplate restTemplate;

	public TelegramService(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

//	Telegram Connecting
	private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

	public ResponseEntity<ResponseStructure<String>> generateTelegramCode(QuantumShareUser user) {
		SecureRandom random = new SecureRandom();
		StringBuilder sb = new StringBuilder("QS-");
		for (int i = 0; i < 15; i++) { // Changed to < 15 to generate exactly 15 characters after "QS-"
			int index = random.nextInt(CHARACTERS.length());
			sb.append(CHARACTERS.charAt(index));
		}
		String telegramCode = sb.toString();
		System.out.println("TelegramCode : " + telegramCode);

		saveTelegramCode(user, telegramCode);

		ResponseStructure<String> structure = new ResponseStructure<>();
		structure.setCode(HttpStatus.OK.value());
		structure.setMessage("Telegram code generated successfully");
		structure.setStatus("success");
		structure.setPlatform("telegram");
		structure.setData(telegramCode);
		return new ResponseEntity<>(structure, HttpStatus.OK);
	}

	private void saveTelegramCode(QuantumShareUser user, String telegramCode) {
		SocialAccounts socialAccounts = user.getSocialAccounts();
		if (socialAccounts == null) {
			socialAccounts = new SocialAccounts();
			user.setSocialAccounts(socialAccounts);
		}

		TelegramUser telegramUser = socialAccounts.getTelegramUser();
		if (telegramUser == null) {
			telegramUser = new TelegramUser();
			socialAccounts.setTelegramUser(telegramUser);
		}
		telegramUser.setTelegramCode(telegramCode);
		userDao.save(user);
	}

//  Fetching Group Details
	public ResponseEntity<ResponseStructure<String>> pollTelegramUpdates(QuantumShareUser user) {
		System.out.println("Coming to pollTelegramUpdates");
		String telegramApiUrl = "https://api.telegram.org/bot%s/getUpdates";
		String url = String.format(telegramApiUrl, telegramBotToken);
		String telegramCode = user.getSocialAccounts().getTelegramUser().getTelegramCode();

		ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

		if (response.getStatusCode() == HttpStatus.OK) {
			String responseBody = response.getBody();
			try {
				String telegramProfileUrl = "https://quantumshare.quantumparadigm.in/vedio/ProfilePicture.jpg";
				JsonNode rootNode = objectMapper.readTree(responseBody);
				JsonNode resultArray = rootNode.get("result");
				for (JsonNode updateNode : resultArray) {
					if (updateNode.has("message")) {
						JsonNode messageNode = updateNode.get("message");
						JsonNode chatNode = messageNode.get("chat");
						long telegramChatId = chatNode.get("id").asLong();
						String telegramGroupName = chatNode.has("title") ? chatNode.get("title").asText() : "";
						int telegramGroupMembersCount = getGroupMembersCount(telegramChatId);
						if (messageNode.has("text")) {
							String text = messageNode.get("text").asText();
							if (text.contains(telegramCode)) {
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
						saveGroupInfo(user, telegramChatId, telegramGroupName, telegramGroupMembersCount,
								telegramProfileUrl);
					}
				}
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

		Map<String, Object> body = new HashMap<>();
		body.put("chat_id", telgramChatId);
		body.put("text", message);

		HttpEntity<Map<String, Object>> responseEntity = new HttpEntity<>(body, headers);

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
		SocialAccounts socialAccounts = user.getSocialAccounts();
		if (socialAccounts == null) {
			socialAccounts = new SocialAccounts();
			user.setSocialAccounts(socialAccounts);
		}
		TelegramUser telegramUser = socialAccounts.getTelegramUser();
		if (telegramUser == null) {
			telegramUser = new TelegramUser();
			socialAccounts.setTelegramUser(telegramUser);
		}
		telegramUser.setTelegramChatId(telegramChatId);
		telegramUser.setTelegramGroupName(telegramGroupName);
		telegramUser.setTelegramGroupMembersCount(telegramGroupMembersCount);
		telegramUser.setTelegramProfileUrl(telegramProfileUrl);
		userDao.save(user);
	}

//	Media Posting
	public ResponseEntity<ResponseWrapper> postMediaToGroup(MediaPost mediaPost, MultipartFile mediaFile,
			TelegramUser user) {
		System.out.println("Coming to TelegramService");
		if (user == null) {
			structure.setMessage("Telegram user not found");
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setPlatform("telegram");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(structure), HttpStatus.NOT_FOUND);
		}

		long telgramChatId = user.getTelegramChatId();
		String contentType = mediaFile.getContentType();
		System.out.println(contentType);

		try {
			if (contentType != null && contentType.startsWith("image/")) {
				System.out.println("Send photo to group");
				sendPhotoToGroup(telgramChatId, mediaFile, mediaPost.getCaption());
			} else if (contentType != null && contentType.startsWith("video/")) {
				System.out.println("Send video to group");
				sendVideoToGroup(telgramChatId, mediaFile, mediaPost.getCaption());
			} else {
				structure.setMessage("Unsupported media type");
				structure.setCode(HttpStatus.BAD_REQUEST.value());
				structure.setPlatform("telegram");
				structure.setStatus("error");
				structure.setData(null);
				return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(structure),
						HttpStatus.BAD_REQUEST);
			}
			structure.setMessage("Posted On Telegram");
			structure.setCode(HttpStatus.OK.value());
			structure.setPlatform("telegram");
			structure.setStatus("success");
			structure.setData(null);
			return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(structure), HttpStatus.OK);
		} catch (Exception e) {
			structure.setMessage("Failed to send media: " + e.getMessage());
			structure.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
			structure.setPlatform("telegram");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(structure),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	public void sendPhotoToGroup(long telgramChatId, MultipartFile mediaFile, String caption) throws IOException {
		System.out.println("Entering to sendPhotoToGroup method");
		String telegramApiPhotoUrl = "https://api.telegram.org/bot%s/sendPhoto";
		String url = String.format(telegramApiPhotoUrl, telegramBotToken);

		headers.setContentType(MediaType.MULTIPART_FORM_DATA);

		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("chat_id", telgramChatId);
		body.add("caption", caption);
		body.add("photo", new ByteArrayResource(mediaFile.getBytes()) {
			@Override
			public String getFilename() {
				return mediaFile.getOriginalFilename();
			}
		});

		HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

		if (response.getStatusCode() == HttpStatus.OK) {
			System.out.println("Photo sent successfully!");
		} else {
			System.err.println("Failed to send photo. Status code: " + response.getStatusCode());
		}
	}

	public void sendVideoToGroup(long telgramChatId, MultipartFile mediaFile, String caption) throws IOException {
		System.out.println("Entering to sendVideoToGroup method");
		String telegramApiVideoUrl = "https://api.telegram.org/bot%s/sendVideo";
		String url = String.format(telegramApiVideoUrl, telegramBotToken);

		headers.setContentType(MediaType.MULTIPART_FORM_DATA);

		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("chat_id", telgramChatId);
		body.add("caption", caption);
		body.add("video", new ByteArrayResource(mediaFile.getBytes()) {
			@Override
			public String getFilename() {
				return mediaFile.getOriginalFilename();
			}
		});

		HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

		if (response.getStatusCode() == HttpStatus.OK) {
			System.out.println("Video sent successfully!");
		} else {
			System.err.println("Failed to send photo. Status code: " + response.getStatusCode());
		}
	}

}