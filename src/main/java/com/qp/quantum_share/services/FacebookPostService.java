package com.qp.quantum_share.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpServerErrorException.InternalServerError;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.qp.quantum_share.configuration.ConfigurationClass;
import com.qp.quantum_share.dao.FacebookUserDao;
import com.qp.quantum_share.dao.QuantumShareUserDao;
import com.qp.quantum_share.dto.FaceBookUser;
import com.qp.quantum_share.dto.FacebookPageDetails;
import com.qp.quantum_share.dto.MediaPost;
import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.exception.CommonException;
import com.qp.quantum_share.exception.FBException;
import com.qp.quantum_share.response.ErrorResponse;
import com.qp.quantum_share.response.ResponseStructure;
import com.qp.quantum_share.response.SuccessResponse;
import com.restfb.BinaryAttachment;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.exception.FacebookException;
import com.restfb.types.FacebookType;
import com.restfb.types.GraphResponse;
import com.restfb.types.ResumableUploadStartResponse;
import com.restfb.types.ResumableUploadTransferResponse;

@Service
public class FacebookPostService {

	@Autowired
	ConfigurationClass config;

	@Autowired
	FacebookUserDao facebookUserDao;

	@Autowired
	ResponseStructure<String> structure;

	@Autowired
	SuccessResponse successResponse;

	@Autowired
	ErrorResponse errorResponse;

	@Autowired
	QuantumShareUserDao userDao;

	@Autowired
	AnalyticsPostService analyticsPostService;

	@Autowired
	HttpHeaders headers;

	@Autowired
	MultiValueMap<String, Object> body;

	@Autowired
	HttpEntity<MultiValueMap<String, Object>> httpEntity;

	@Autowired
	RestTemplate restTemplate;

	@Autowired
	SocialMediaLogoutService mediaLogoutService;

	private static final long MAX_FILE_SIZE = 60 * 1024 * 1024;

	public boolean postToPage(String pageId, String pageAccessToken, String message) {

		FacebookClient client = config.getFacebookClient(pageAccessToken);
		try {
			FacebookType response = client.publish(pageId + "/feed", FacebookType.class,
					Parameter.with("message", message));
			return true;
		} catch (FacebookException e) {
			return false;
		} catch (Exception e) {
			throw new CommonException(e.getMessage());
		}
	}

	public ResponseEntity<List<Object>> postMediaToPage(MediaPost mediaPost, MultipartFile mediaFile, FaceBookUser user,
			QuantumShareUser qsuser) {
		List<Object> mainresponse = config.getList();
		mainresponse.clear();

		try {
			List<FacebookPageDetails> pages = new ArrayList<>(user.getPageDetails());
			if (pages.isEmpty()) {
				structure.setCode(HttpStatus.NOT_FOUND.value());
				structure.setMessage("No pages are available for this Facebook account.");
				structure.setPlatform("facebook");
				structure.setStatus("error");
				structure.setData(null);
				mainresponse.add(structure);
				return new ResponseEntity<List<Object>>(mainresponse, HttpStatus.NOT_FOUND);
			}
			for (FacebookPageDetails page : pages) {
				String facebookPageId = page.getFbPageId();
				String pageAccessToken = page.getFbPageAceessToken();
				FacebookClient client = config.getFacebookClient(pageAccessToken);
				FacebookType response;
				if (isVideo(mediaFile)) {
					if (mediaFile.getSize() <= MAX_FILE_SIZE) {
//					if (false) {
						ResponseEntity<JsonNode> res = postVideo(facebookPageId, pageAccessToken, mediaFile,
								mediaPost.getCaption());
						if (res.getStatusCode().is2xxSuccessful()) {
							analyticsPostService.savePost(res.getBody().get("id").asText(), facebookPageId, qsuser,
									mediaFile.getContentType(), "facebook", page.getPageName());
							SuccessResponse succesresponse = config.getSuccessResponse();
							succesresponse.setCode(HttpStatus.OK.value());
							succesresponse.setMessage("Posted On " + page.getPageName() + " FaceBook Page");
							succesresponse.setStatus("success");
							succesresponse.setPlatform("facebook");
							succesresponse.setData(res.getBody());
							succesresponse.setRemainingCredits(qsuser.getCredit());
							mainresponse.add(succesresponse);
						} else {
							ErrorResponse errResponse = config.getErrorResponse();
							errResponse.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
							errResponse.setMessage("Request Failed to post on " + page.getPageName());
							errResponse.setStatus("error");
							errResponse.setPlatform("facebook");
							errResponse.setData(res.getBody());
							mainresponse.add(errResponse);
						}
					} else {
						byte[] videoByte = mediaFile.getBytes();
						int videosize = videoByte.length;
						String uploadSessionId = createVideoUploadSession(client, facebookPageId, videosize);
						uploadSessionId = uploadSessionId.replaceAll("\"", "");
						long startOffset = 0;

						while (startOffset < videosize) {
							startOffset = uploadVideoChunk(client, facebookPageId, uploadSessionId, startOffset,
									videoByte);
						}
						GraphResponse finalResponse = finishVideoUploadSession(facebookPageId, client, uploadSessionId,
								mediaPost.getCaption());
						String pageName = page.getPageName();
						if (finalResponse.isSuccess()) {
							qsuser.setCredit(qsuser.getCredit() - 1);
							userDao.save(qsuser);
							System.out.println();
							analyticsPostService.savePost(finalResponse.getId(), facebookPageId, qsuser,
									mediaFile.getContentType(), "facebook", pageName);
							SuccessResponse succesresponse = config.getSuccessResponse();
							succesresponse.setCode(HttpStatus.OK.value());
							succesresponse.setMessage("Posted On " + pageName + " FaceBook Page");
							succesresponse.setStatus("success");
							succesresponse.setPlatform("facebook");
							succesresponse.setData(finalResponse);
							succesresponse.setRemainingCredits(qsuser.getCredit());
							mainresponse.add(succesresponse);
						} else {
							ErrorResponse errResponse = config.getErrorResponse();
							errResponse.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
							errResponse.setMessage("Request Failed to post on " + page.getPageName());
							errResponse.setStatus("error");
							errResponse.setPlatform("facebook");
							errResponse.setData(finalResponse);
							mainresponse.add(errResponse);
						}
					}

				} else {
					String pagename = page.getPageName();
					response = client.publish(facebookPageId + "/photos", FacebookType.class,
							BinaryAttachment.with("source", mediaFile.getBytes()),
							Parameter.with("message", mediaPost.getCaption()));
					if (response.getId() != null) {
						System.out.println("picture : " + response.getId());
						analyticsPostService.savePost(response.getId(), facebookPageId, qsuser,
								mediaFile.getContentType(), "facebook", pagename);
						SuccessResponse succesresponse = config.getSuccessResponse();
						qsuser.setCredit(qsuser.getCredit() - 1);
						userDao.save(qsuser);
						succesresponse.setCode(HttpStatus.OK.value());
						succesresponse.setMessage("Posted On " + page.getPageName() + " FaceBook Page");
						succesresponse.setStatus("success");
						succesresponse.setData(response);
						succesresponse.setPlatform("facebook");
						succesresponse.setRemainingCredits(qsuser.getCredit());
						mainresponse.add(succesresponse);
					} else {
						ErrorResponse errResponse = config.getErrorResponse();
						errResponse.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
						errResponse.setMessage("Request Failed to post on " + page.getPageName());
						errResponse.setStatus("error");
						errResponse.setData(response);
						errResponse.setPlatform("facebook");
						mainresponse.add(errResponse);
					}
				}
			}
			return new ResponseEntity<List<Object>>(mainresponse, HttpStatus.OK);

		} catch (FacebookException e) {
			if (e.getMessage().contains("Error validating access token: Session has expired")) {
				mediaLogoutService.disconnectFacebook(qsuser);
				structure.setCode(118);
				structure.setMessage("Access Expiry!! Please Connect your Instagram profile");
				structure.setPlatform("instagram");
				structure.setStatus("error");
				structure.setData(e.getMessage());
				mainresponse.add(structure);
				return new ResponseEntity<List<Object>>(mainresponse, HttpStatus.OK);
			}
			throw new FBException(e.getMessage(), "facebook");
		} catch (IllegalArgumentException e) {
			throw new CommonException(e.getMessage());
		} catch (IOException e) {
			throw new CommonException(e.getMessage());
		} catch (NullPointerException e) {
			throw new NullPointerException(e.getMessage());
		} catch (InternalServerError error) {
			throw new CommonException(error.getMessage());
		} catch (Exception e) {
			throw new CommonException(e.getMessage());
		}
	}

	private ResponseEntity<JsonNode> postVideo(String facebookPageId, String pageAccessToken, MultipartFile mediaFile,
			String message) {
		try {
			System.out.println("videp methid 1");
			String url = "https://graph.facebook.com/v20.0/" + facebookPageId + "/videos";
			headers.setContentType(MediaType.MULTIPART_FORM_DATA);
			headers.setBearerAuth(pageAccessToken);
			body.add("file", mediaFile.getResource());
			System.out.println(mediaFile.getResource());
			if (mediaFile.isEmpty()) {
				throw new IllegalArgumentException("File is empty.");
			}
			System.out.println(mediaFile.getContentType());
			System.out.println(mediaFile.getInputStream().toString());
			body.add("description", message);
			HttpEntity<MultiValueMap<String, Object>> requestEntity = config.getHttpEntityWithMap(body, headers);
			System.out.println(requestEntity);
			ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity,
					JsonNode.class);
			System.out.println("response " + response.getBody());
			return response;
		} catch (Exception e) {
			e.printStackTrace();
			throw new CommonException(e.getMessage());
		}

	}

	public String createVideoUploadSession(FacebookClient client, String pageId, long fileSize) {
		ResumableUploadStartResponse response = client.publish(pageId + "/videos", ResumableUploadStartResponse.class,
				Parameter.with("upload_phase", "start"), Parameter.with("file_size", fileSize));
		System.out.println(" createVideoUploadSession  :  " + response);
		return response.getUploadSessionId();
	}

	public Long uploadVideoChunk(FacebookClient client, String facebookPageId, String uploadSessionId, long startOffset,
			byte[] vidFile) {
		ResumableUploadTransferResponse response = client.publish(facebookPageId + "/videos",
				ResumableUploadTransferResponse.class, BinaryAttachment.with("video_file_chunk", vidFile),
				Parameter.with("upload_phase", "transfer"), Parameter.with("start_offset", startOffset),
				Parameter.with("upload_session_id", uploadSessionId));
		System.out.println(" video chunnk : " + response);
		return response.getStartOffset();
	}

	public GraphResponse finishVideoUploadSession(String facebookPageId, FacebookClient client, String uploadSessionId,
			String message) {
		GraphResponse response = client.publish(facebookPageId + "/videos", GraphResponse.class,
				Parameter.with("upload_phase", "finish"), Parameter.with("upload_session_id", uploadSessionId),
				Parameter.with("description", message));
		System.out.println("response : " + response.toString() + "    ---   " + response);
		System.out.println("finishVideoUploadSession : " + response.getPostId());
		return response;
	}

	public boolean isVideo(MultipartFile file) {
		if (file.getContentType().startsWith("video")) {
			return true;
		} else if (file.getContentType().startsWith("image")) {
			return false;
		} else {
			throw new IllegalArgumentException("Unsupported file type: " + file.getContentType());
		}
	}
}
