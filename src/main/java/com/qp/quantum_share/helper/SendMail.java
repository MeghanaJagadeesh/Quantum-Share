package com.qp.quantum_share.helper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.qp.quantum_share.dao.QuantumShareUserDao;
import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.exception.CommonException;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class SendMail {
	@Autowired
	JavaMailSender mailSender;

	@Autowired
	QuantumShareUserDao userDao;
	
	@Value("${quatumshare.sendmail}")
	private String redirect;

	public void sendVerificationEmail(QuantumShareUser userDto, String type) {
		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);
		try {
			helper.setFrom("quantumshare12@gmail.com", "QuantumShare");
			helper.setTo(userDto.getEmail());
			String htmlBody = null;
			if (type.equals("signup")) {
				String verificationLink = redirect+"verify?token="
						+ userDto.getVerificationToken();
				helper.setSubject("Welcome to Quantumshare, Please verify your email address");
				htmlBody = readHtmlTemplate("verification_email_template.html");
				htmlBody = htmlBody.replace("{{VERIFICATION_LINK}}", verificationLink);

			} else if (type.equals("email_updation")) {
				String verificationLink = redirect+"verify/update?token="
						+ userDto.getVerificationToken();
				helper.setSubject("Secure Your Account: Confirm Your Email Change");
				htmlBody = readHtmlTemplate("email_updation.html");
				htmlBody = htmlBody.replace("{{VERIFICATION_LINK}}", verificationLink);
			} else if (type.equals("password_reset_request")) {
				String verificationLink = redirect+"user/rest_password/request?token="
						+ userDto.getVerificationToken();
				helper.setSubject("Quantum Share: Password Reset Request");
				htmlBody = readHtmlTemplate("password_reset_request.html");
				htmlBody = htmlBody.replace("{{RESET_PASSWORD_LINK}}", verificationLink);
			}

			else if (type.equals("password_updation")) {
				helper.setSubject("Your Quantum Share Password Has Been Updated\"");
				htmlBody = readHtmlTemplate("password_successful_updation.html");
			}
			String firstName=userDto.getFirstName();
			String lastName=userDto.getLastName();
			if(firstName==null) {
				firstName="";
			}
			if(lastName==null) {
				lastName="";
			}
			htmlBody = htmlBody.replace("{{USERNAME}}", userDto.getFirstName() + " " + userDto.getLastName());
			helper.setText(htmlBody, true);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (MessagingException e) {
			e.printStackTrace();
		}
		mailSender.send(message);
	}

	private String readHtmlTemplate(String templateName) {
		try {
			ClassPathResource resource = new ClassPathResource("templates/" + templateName);
			InputStream inputStream = resource.getInputStream();
			byte[] bytes = new byte[inputStream.available()];
			inputStream.read(bytes);
			return new String(bytes, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new CommonException(e.getMessage());
		}
	}
}
