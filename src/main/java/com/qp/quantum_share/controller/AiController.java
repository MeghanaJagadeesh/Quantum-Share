package com.qp.quantum_share.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.qp.quantum_share.response.ResponseStructure;
import com.qp.quantum_share.services.AiService;
@RestController
@RequestMapping("/quantum-share")
public class AiController {

	@Autowired
	AiService aiService;
	
	 @Autowired
	 ResponseStructure<String> responseStructure;
	 
	 @Autowired
	 ResponseStructure<byte[]> responsedStructure;

	
//	@Value("${openAI.system.message}")
//	private String systemMessage;
//	
//	@PostMapping("/aichat")
//	public ResponseEntity<ResponseStructure<String>> aiChat(@RequestParam(name = "userMessage") String userMessage)
//	{
//		  if (userMessage == null || userMessage.isEmpty()) {
//	            // Handle the case where userMessage is empty or null
//	            responseStructure.setStatus("Error");
//	            responseStructure.setMessage("Missing or Inorrect Parameter");
//	            responseStructure.setCode(HttpStatus.BAD_REQUEST.value());
//	            responseStructure.setData(null);
//	            return ResponseEntity.badRequest().body(responseStructure);
//	        }
//	        
//		return aiService.aiChat(userMessage,systemMessage);
//	}
	

	 //GEMINI AI TEXT GENERATION
	 @PostMapping("/aitext")
	    public ResponseEntity<ResponseStructure<String>> generateContent(@RequestParam(value = "userMessage", required = false) String userMessage) {
	        if (userMessage == null || userMessage.isEmpty()) {
	            return ResponseEntity.badRequest().body(aiService.handleEmptyOrNullRequest());
	        }

	        try {
	            ResponseStructure<String> responseStructure = aiService.generateContent(userMessage);

	            if (responseStructure == null) {
	                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(aiService.handleExceededLimits());
	            }

	            return ResponseEntity.ok(responseStructure);
	        } catch (Exception e) {
	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(aiService.handleEmptyGeneratedContent());
	        }
	    }

	@PostMapping("/generate-image")
	public ResponseEntity<Object> generateImage(@RequestParam(name="textPromt") String textPrompt) {
		try {
	        if (textPrompt == null || textPrompt.isEmpty()) {
	            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	                                 .body(Map.of("status", HttpStatus.BAD_REQUEST.value(),
	                                              "error", "Bad Request",
	                                              "message", "Text prompt/field cannot be empty"));
	        }
	        byte[] imageData = aiService.generateImage(textPrompt);
	        HttpHeaders headers = new HttpHeaders();
	        headers.setContentType(MediaType.IMAGE_PNG);
	        return new ResponseEntity<>(imageData, headers, HttpStatus.OK);
	    } catch (RuntimeException e) {
	        // Simplified error response for other runtime exceptions
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                             .body(Map.of("status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
	                                          "error", "Internal Server Error",
	                                          "message", e.getMessage()));
	    }
	}
}