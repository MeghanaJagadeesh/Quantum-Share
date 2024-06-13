package com.qp.quantum_share.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.qp.quantum_share.dao.FaceBookPageDao;
import com.qp.quantum_share.dao.FacebookUserDao;
import com.qp.quantum_share.dao.InstagramUserDao;
import com.qp.quantum_share.dao.QuantumShareUserDao;
import com.qp.quantum_share.dao.SocialAccountDao;
import com.qp.quantum_share.dao.TelegramUserDao;
import com.qp.quantum_share.dto.FaceBookUser;
import com.qp.quantum_share.dto.FacebookPageDetails;
import com.qp.quantum_share.dto.InstagramUser;
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
		List<FacebookPageDetails> pages = accounts.getFacebookUser().getPageDetails();
		System.out.println("1 " + pages);
		accounts.getFacebookUser().setPageDetails(null);
		accounts.setFacebookUser(null);
		user.setSocialAccounts(accounts);
		userDao.save(user);
		System.out.println("2 " + pages);
		facebookUserDao.deleteFbUser(deleteUser);
		pageDao.deletePage(pages);

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

}
