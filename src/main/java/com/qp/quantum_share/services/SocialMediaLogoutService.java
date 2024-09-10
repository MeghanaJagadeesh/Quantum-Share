package com.qp.quantum_share.services;

import java.util.List;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.qp.quantum_share.dao.FaceBookPageDao;
import com.qp.quantum_share.dao.FacebookUserDao;
import com.qp.quantum_share.dao.InstagramUserDao;
import com.qp.quantum_share.dao.LinkedInPageDao;
import com.qp.quantum_share.dao.LinkedInProfileDao;
import com.qp.quantum_share.dao.PostsDao;
import com.qp.quantum_share.dao.QuantumShareUserDao;
import com.qp.quantum_share.dao.SocialAccountDao;
import com.qp.quantum_share.dao.TelegramUserDao;
import com.qp.quantum_share.dto.FaceBookUser;
import com.qp.quantum_share.dto.FacebookPageDetails;
import com.qp.quantum_share.dto.InstagramUser;
import com.qp.quantum_share.dto.LinkedInPageDto;
import com.qp.quantum_share.dto.LinkedInProfileDto;
import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.dto.SocialAccounts;
import com.qp.quantum_share.dto.TelegramUser;
import com.qp.quantum_share.response.ResponseStructure;

@Service
public class SocialMediaLogoutService {

	@Autowired
	FacebookUserDao facebookUserDao;

	@Autowired
	FaceBookPageDao pageDao;

	@Autowired
	ResponseStructure<String> structure;

	@Autowired
	SocialAccountDao accountDao;

	@Autowired
	QuantumShareUserDao userDao;

	@Autowired
	InstagramUserDao instagramUserDao;

	@Autowired
	TelegramUserDao telegramUserDao;

	@Autowired
	PostsDao postsDao;

	@Autowired
	AnalyticsPostService analyticsPostService;

	@Autowired
	LinkedInProfileDao linkedInProfileDao;

	@Autowired
	LinkedInPageDao linkedInPageDao;

	@Autowired
	SocialAccountDao socialAccountDao;

	public ResponseEntity<ResponseStructure<String>> disconnectFacebook(QuantumShareUser user) {
		SocialAccounts accounts = user.getSocialAccounts();
		if (accounts == null || accounts.getFacebookUser() == null) {
			structure.setCode(404); // Or a custom code for Facebook not linked
			structure.setMessage("Facebook account not linked to this user");
			structure.setStatus("error");
			structure.setData(null);
			structure.setPlatform("facebook");
			return new ResponseEntity<>(structure, HttpStatus.NOT_FOUND);
		}

		FaceBookUser deleteUser = accounts.getFacebookUser();

		Hibernate.initialize(deleteUser.getPageDetails());
		List<FacebookPageDetails> pages = deleteUser.getPageDetails();
		accounts.getFacebookUser().setPageDetails(null);
		accounts.setFacebookUser(null);
		user.setSocialAccounts(accounts);
		userDao.save(user);
		facebookUserDao.deleteFbUser(deleteUser);
		pageDao.deletePage(pages);

		analyticsPostService.deletePosts(user, "facebook");

		structure.setCode(HttpStatus.OK.value());
		structure.setMessage("Facebook Disconnected Successfully");
		structure.setPlatform("facebook");
		structure.setStatus("success");
		structure.setData(null);
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
	}

	public ResponseEntity<ResponseStructure<String>> disconnectInstagram(QuantumShareUser user) {
		SocialAccounts accounts = user.getSocialAccounts();
		if (accounts == null || accounts.getInstagramUser() == null) {
			structure.setCode(404);
			structure.setMessage("Instagram account not linked to this user");
			structure.setStatus("error");
			structure.setData(null);
			structure.setPlatform("instagram");
			return new ResponseEntity<>(structure, HttpStatus.NOT_FOUND);
		}
		InstagramUser deleteUser = accounts.getInstagramUser();
		accounts.setInstagramUser(null);
		user.setSocialAccounts(accounts);
		userDao.save(user);

		instagramUserDao.deleteUser(deleteUser);
		analyticsPostService.deletePosts(user, "instagram");
		structure.setCode(HttpStatus.OK.value());
		structure.setMessage("Instagram Disconnected Successfully");
		structure.setPlatform("instagram");
		structure.setStatus("success");
		structure.setData(null);
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);

	}

	// Telegram
	public ResponseEntity<ResponseStructure<String>> disconnectTelegram(QuantumShareUser user) {
		SocialAccounts accounts = user.getSocialAccounts();
		if (accounts == null || accounts.getTelegramUser() == null) {
			structure.setCode(404);
			structure.setMessage("Telegram account not linked to this user");
			structure.setStatus("error");
			structure.setData(null);
			structure.setPlatform("telegram");
			return new ResponseEntity<>(structure, HttpStatus.NOT_FOUND);
		}
		TelegramUser deleteUser = accounts.getTelegramUser();
		accounts.setTelegramUser(null);
		user.setSocialAccounts(accounts);
		userDao.save(user);

		telegramUserDao.deleteUser(deleteUser);

		structure.setCode(HttpStatus.OK.value());
		structure.setMessage("Telegram Disconnected Successfully");
		structure.setPlatform("telegram");
		structure.setStatus("success");
		structure.setData(null);
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
	}


	public ResponseEntity<ResponseStructure<String>> disconnectLinkedIn(QuantumShareUser user) {
		SocialAccounts accounts = user.getSocialAccounts();
		if (accounts == null || accounts.getLinkedInProfileDto() == null) {
			structure.setCode(404);
			structure.setMessage("LinkedIn account not linked to this user");
			structure.setStatus("error");
			structure.setData(null);
			structure.setPlatform("LinkedIn");
			return new ResponseEntity<>(structure, HttpStatus.NOT_FOUND);
		}
		LinkedInProfileDto deleteUser = accounts.getLinkedInProfileDto();
		if (deleteUser.getLinkedinProfileURN() != null) {
			accounts.setLinkedInProfileDto(null);
			user.setSocialAccounts(accounts);
			userDao.save(user);
			linkedInProfileDao.deleteUser(deleteUser);
			structure.setCode(HttpStatus.OK.value());
			structure.setMessage("LinkedIn Profile Disconnected Successfully");
			structure.setPlatform("LinkedIn");
			structure.setStatus("success");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
		} else if (deleteUser.getPages().get(0).getLinkedinPageURN() != null) {
			List<LinkedInPageDto> pages = deleteUser.getPages();
			if (pages.isEmpty()) {
				structure.setCode(HttpStatus.BAD_REQUEST.value());
				structure.setMessage("No LinkedIn pages found for user");
				structure.setStatus("error");
				structure.setPlatform("LinkedIn");
				structure.setData(null);
				return ResponseEntity.badRequest().body(structure);
			}
			LinkedInPageDto linkedInPageDto = pages.get(0);
			deleteUser.getPages().remove(linkedInPageDto);
			linkedInPageDao.deletePage(linkedInPageDto);
			if (deleteUser.getPages().isEmpty()) {
				accounts.setLinkedInProfileDto(null);
				linkedInProfileDao.deleteUser(deleteUser);
				user.setSocialAccounts(null);
				accountDao.deleteSocialAccount(accounts);
			}
			userDao.save(user);
			ResponseStructure<String> response = new ResponseStructure<>();
			response.setCode(HttpStatus.OK.value());
			response.setMessage("LinkedIn Page Disconnected Successfully");
			response.setStatus("success");
			response.setPlatform("LinkedIn");
			response.setData("Page '" + linkedInPageDto.getLinkedinPageName() + "' disconnected successfully");
			return ResponseEntity.ok(response);
		}
		return null;

	}
}
