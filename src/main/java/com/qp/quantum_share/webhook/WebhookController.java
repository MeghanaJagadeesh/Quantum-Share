package com.qp.quantum_share.webhook;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    @Value("${app.secret}")
    private String appSecret;

    @Value("${app.verify.token}")
    private String verifyToken;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping
    public ResponseEntity<String> verifyWebhook(@RequestParam("hub.mode") String mode, 
                                                @RequestParam("hub.verify_token") String token, 
                                                @RequestParam("hub.challenge") String challenge) {
        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            return ResponseEntity.ok(challenge);
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification failed");
        }
    }

    @PostMapping("/facebook")
    public ResponseEntity<String> handleFacebookWebhook(@RequestHeader("X-Hub-Signature") String signature,
                                                        @RequestBody String payload) {
        if (!isValidSignature(payload, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(payload);
            processFacebookUpdate(jsonNode);
            return ResponseEntity.ok("EVENT_RECEIVED");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid payload");
        }
    }

    private boolean isValidSignature(String payload, String signature) {
        try {
            String[] parts = signature.split("=");
            String algorithm = parts[0];
            String expectedSignature = parts[1];

            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(appSecret.getBytes(), algorithm));
            byte[] digest = mac.doFinal(payload.getBytes());

            return Hex.encodeHexString(digest).equals(expectedSignature);
        } catch (Exception e) {
            return false;
        }
    }

    private void processFacebookUpdate(JsonNode jsonNode) {
        // Process the Facebook update here
        System.out.println("Facebook request body: " + jsonNode);
    }
}