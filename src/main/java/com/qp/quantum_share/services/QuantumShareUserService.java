package com.qp.quantum_share.services;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.qp.quantum_share.configuration.ConfigurationClass;
import com.qp.quantum_share.dao.QuantumShareUserDao;
import com.qp.quantum_share.dto.CreditSystem;
import com.qp.quantum_share.dto.FaceBookUser;
import com.qp.quantum_share.dto.FacebookPageDetails;
import com.qp.quantum_share.dto.InstagramUser;
import com.qp.quantum_share.dto.LinkedInPageDto;
import com.qp.quantum_share.dto.LinkedInProfileDto;
import com.qp.quantum_share.dto.PinterestUser;
import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.dto.RedditDto;
import com.qp.quantum_share.dto.SocialAccounts;
import com.qp.quantum_share.dto.SubscriptionDetails;
import com.qp.quantum_share.dto.TelegramUser;
import com.qp.quantum_share.dto.TwitterUser;
import com.qp.quantum_share.dto.YoutubeUser;
import com.qp.quantum_share.helper.CommonMethod;
import com.qp.quantum_share.helper.GenerateId;
import com.qp.quantum_share.helper.JwtToken;
import com.qp.quantum_share.helper.PostOnServer;
import com.qp.quantum_share.helper.SecurePassword;
import com.qp.quantum_share.helper.SendMail;
import com.qp.quantum_share.response.ResponseStructure;

@Service
public class QuantumShareUserService {

	@Value("${quantumshare.freetrail}")
	private int freetrail;

	@Value("${quantumshare.freeCredit}")
	private int freeCredit;

	@Value("${default.profile.picture}")
	private String defaultProfile;

	@Autowired
	QuantumShareUserDao userDao;

	@Autowired
	GenerateId generateId;

	@Autowired
	SendMail sendMail;

	@Autowired
	JwtToken token;

	@Autowired
	ConfigurationClass configure;

	@Autowired
	SubscriptionDetails subscriptionDetails;

	@Autowired
	QuantumShareUserTracking userTracking;

	@Autowired
	RedditService redditService;

	@Autowired
	PostOnServer postOnServer;

	@Autowired
	CommonMethod commonMethod;

	@Autowired
	YoutubeService youtubeService;

	@Autowired
	TwitterService twitterService;

	@Autowired
	AdminService adminService;

	public ResponseEntity<ResponseStructure<String>> login(String emph, String password) {
		ResponseStructure<String> structure = new ResponseStructure<String>();
		long mobile = 1;
		String email = null;
		try {
			mobile = Long.parseLong(emph);
		} catch (NumberFormatException e) {
			email = emph;
		}
		if (adminService.isStaff(email)) {
			return adminService.staffLogin(emph, password);
		}

		QuantumShareUser users = userDao.findByEmail(email);
		if (users == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("Invalid email or mobile");
			structure.setStatus("success");
			structure.setData(null);
			structure.setPlatform(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		} else {
			QuantumShareUser user = users;
			if (user.getPassword() == null || user.getPassword().isEmpty() || user.getPassword().isBlank()) {
				structure.setCode(120);
				structure.setMessage("Password null");
				structure.setStatus("error");
				structure.setData(user.getEmail());
				structure.setPlatform(null);
				return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
			}
			if (SecurePassword.decrypt(user.getPassword(), "123").equals(password)) {
				if (user.isVerified()) {
					String tokenValue = token.generateJWT(user);
					structure.setCode(HttpStatus.OK.value());
					structure.setMessage("Login Successful");
					structure.setStatus("success");
					structure.setData(tokenValue);
					structure.setPlatform(null);

					return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);

				} else {
					String verificationToken = UUID.randomUUID().toString();
					user.setVerificationToken(verificationToken);
					userDao.save(user);
					sendMail.sendVerificationEmail(user, "signup");
					structure.setCode(HttpStatus.NOT_ACCEPTABLE.value());
					structure.setMessage("please verify your email, email has been sent.");
					structure.setStatus("error");
					structure.setData(user);
					structure.setPlatform(null);

					return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_ACCEPTABLE);
				}
			} else {
				structure.setCode(HttpStatus.NOT_ACCEPTABLE.value());
				structure.setMessage("Invalid Password");
				structure.setStatus("error");
				structure.setData(null);
				structure.setPlatform(null);

				return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_ACCEPTABLE);
			}
		}
	}

	public ResponseEntity<ResponseStructure<String>> regeneratePassword(String password, String email) {
		QuantumShareUser user = userDao.findByEmail(email);
		ResponseStructure<String> structure = new ResponseStructure<String>();
		if (user == null) {
			structure.setMessage("User not found");
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setStatus("error");
			structure.setData(null);
			structure.setPlatform(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_ACCEPTABLE);
		}
		user.setPassword(SecurePassword.encrypt(password, "123"));
		userDao.save(user);
		String tokenValue = token.generateJWT(user);
		structure.setCode(HttpStatus.CREATED.value());
		structure.setStatus("success");
		structure.setMessage("successfully signedup");
		structure.setData(tokenValue);
		structure.setPlatform(null);

		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.CREATED);

	}

	public ResponseEntity<ResponseStructure<String>> AppForgetPassword(String email) {
		ResponseStructure<String> structure = new ResponseStructure<String>();
		QuantumShareUser user = userDao.findByEmail(email);
		if (user == null) {
			structure.setData(null);
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("No account found with this email address.");
			structure.setStatus("error");
			structure.setPlatform(null);
			structure.setPlatform(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		String verificationToken = UUID.randomUUID().toString();
		user.setVerificationToken(verificationToken);
		userDao.save(user);
//		sendMail.sendVerificationEmail(user, "password_reset_request");

		structure.setData(verificationToken);
		structure.setMessage("Change your password");
		structure.setPlatform(null);
		structure.setStatus("success");
		structure.setCode(HttpStatus.OK.value());
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
	}

	public ResponseEntity<ResponseStructure<String>> userSignUp(QuantumShareUser user) {
		ResponseStructure<String> structure = new ResponseStructure<String>();
		List<QuantumShareUser> exUser = userDao.findByEmailOrPhoneNo(user.getEmail(), user.getPhoneNo());
		if (!exUser.isEmpty()) {
			structure.setMessage("Account Already exist");
			structure.setCode(HttpStatus.NOT_ACCEPTABLE.value());
			structure.setStatus("error");
			structure.setData(null);
			structure.setPlatform(null);

			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_ACCEPTABLE);
		} else {
			user.setPassword(SecurePassword.encrypt(user.getPassword(), "123"));
//			userDao.saveUser(user);
			String verificationToken = UUID.randomUUID().toString();
			user.setVerificationToken(verificationToken);
			userDao.save(user);
			sendMail.sendVerificationEmail(user, "signup");
			structure.setCode(HttpStatus.CREATED.value());
			structure.setStatus("success");
			structure.setMessage("successfully signedup, please verify your mail.");
			structure.setData(user);
			structure.setPlatform(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.CREATED);
		}
	}

	public ResponseEntity<ResponseStructure<String>> signInWithGoogle(QuantumShareUser user) {
		ResponseStructure<String> structure = new ResponseStructure<String>();
		QuantumShareUser exUser = userDao.findByEmail(user.getEmail());
		if (exUser != null) {
			structure.setMessage("Account Already exist with this email");
			structure.setCode(HttpStatus.NOT_ACCEPTABLE.value());
			structure.setStatus("error");
			structure.setData(null);
			structure.setPlatform(null);

			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_ACCEPTABLE);
		} else {
			user.setSignUpDate(Instant.now());
			user.setTrial(true);
			user.setVerified(true);
//			user.setPassword(user.getPassword());
			user.setPassword(SecurePassword.encrypt(user.getPassword(), "123"));
//			
			CreditSystem creditSystem = new CreditSystem();
			creditSystem.setCreditedDate(LocalDate.now());
			creditSystem.setCreditedTime(LocalTime.now());
			creditSystem.setTotalAppliedCredit(freeCredit);
			creditSystem.setRemainingCredit(freeCredit);
			user.setCreditSystem(creditSystem);
			userDao.save(user);
			String tokenValue = token.generateJWT(user);
			structure.setCode(HttpStatus.CREATED.value());
			structure.setStatus("success");
			structure.setMessage("successfully signedup");
			structure.setData(tokenValue);
			structure.setPlatform(null);

			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.CREATED);
		}
	}

	public ResponseEntity<ResponseStructure<String>> verifyEmail(String token) {
		ResponseStructure<String> structure = new ResponseStructure<String>();
		QuantumShareUser user = userDao.findByVerificationToken(token);
		if (user != null) {
			user.setVerified(true);
			user.setSignUpDate(Instant.now());
			user.setTrial(true);
			user.setVerificationToken(null);
			CreditSystem creditSystem = new CreditSystem();
			creditSystem.setCreditedDate(LocalDate.now());
			creditSystem.setCreditedTime(LocalTime.now());
			creditSystem.setTotalAppliedCredit(freeCredit);
			creditSystem.setRemainingCredit(freeCredit);
			user.setCreditSystem(creditSystem);
			Map<String, Object> map = configure.getMap();
			map.put("remainingdays", freetrail);
			map.put("user", user);
			userDao.saveUser(user);

			structure.setCode(HttpStatus.CREATED.value());
			structure.setStatus("success");
			structure.setMessage("successfully signedup");
			structure.setData(map);
			structure.setPlatform(null);

			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.CREATED);
		} else {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("Email verification failed... ");
			structure.setStatus("error");
			structure.setData(null);
			structure.setPlatform(null);

			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.BAD_REQUEST);
		}
	}

	public ResponseEntity<ResponseStructure<String>> verifyUpdatedEmail(String token) {
		ResponseStructure<String> structure = new ResponseStructure<String>();
		QuantumShareUser user = userDao.findByVerificationToken(token);
		if (user != null) {
			user.setVerified(true);
			user.setVerificationToken(null);
			userDao.saveUser(user);

			structure.setCode(HttpStatus.CREATED.value());
			structure.setStatus("success");
			structure.setMessage("email updated successfully, Please login");
			structure.setData(user);
			structure.setPlatform(null);

			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.CREATED);
		} else {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("Email verification failed... ");
			structure.setStatus("error");
			structure.setData(null);
			structure.setPlatform(null);

			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.BAD_REQUEST);
		}
	}

	public ResponseEntity<ResponseStructure<String>> accountOverView(int userId) {
		ResponseStructure<String> structure = new ResponseStructure<String>();
		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("user doesn't exists, please login");
			structure.setStatus("error");
			structure.setData(null);
			structure.setPlatform(null);

			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		Map<String, Object> map = configure.getMap();
		map.clear();
		map.put("name", user.getFirstName() + " " + user.getLastName());
		map.put("company_name", user.getCompany());
		map.put("email", user.getEmail());
		map.put("mobile", user.getPhoneNo());
		map.put("profile_pic", user.getProfilePic());

		structure.setCode(HttpStatus.OK.value());
		structure.setData(map);
		structure.setStatus("success");
		structure.setMessage(null);
		structure.setPlatform(null);
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
	}

	public ResponseEntity<ResponseStructure<String>> accountOverView(int userId, MultipartFile[] mediaFile,
			String firstname, String lastname, String email, Long phoneNo, String company) {
		ResponseStructure<String> structure = new ResponseStructure<String>();
		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("user doesn't exists, please login");
			structure.setStatus("error");
			structure.setData(null);
			structure.setPlatform(null);

			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		String profilepic = null;
		MultipartFile file = mediaFile[0];
		if (file != null) {
			if (!file.getContentType().startsWith("image")) {
				structure.setCode(HttpStatus.BAD_REQUEST.value());
				structure.setMessage("Missing or invalid file type");
				structure.setStatus("error");
				structure.setPlatform(null);
				structure.setData(null);
				structure.setPlatform(null);

				return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.UNAUTHORIZED);
			}
			profilepic = postOnServer.uploadFile(mediaFile, "profile/").get(0);
			user.setProfilePic(profilepic);
			userDao.save(user);
		}
		if (firstname != null) {
			user.setFirstName(firstname);
			userDao.save(user);
		}
		if (lastname != null) {
			user.setLastName(lastname);
			userDao.save(user);
		}
		if (phoneNo != null) {
			user.setPhoneNo(phoneNo);
			userDao.save(user);
		}
		if (company != null) {
			user.setCompany(company);
			userDao.save(user);
		}
		if (email != null) {
			QuantumShareUser exuser = userDao.findByEmail(email);
			if (exuser != null) {
				structure.setData(null);
				structure.setMessage("Account already exists with this email address");
				structure.setPlatform(null);
				structure.setStatus("success");
				structure.setCode(HttpStatus.NOT_ACCEPTABLE.value());
				return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_ACCEPTABLE);

			}
			String verificationToken = UUID.randomUUID().toString();
			user.setVerificationToken(verificationToken);
			user.setVerified(false);
			user.setEmail(email);
			userDao.save(user);
			sendMail.sendVerificationEmail(user, "email_updation");

			structure.setData(null);
			structure.setMessage("Email Updated successfully, please verify your mail");
			structure.setPlatform(null);
			structure.setStatus("success");
			structure.setCode(HttpStatus.OK.value());
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
		}

		structure.setCode(HttpStatus.OK.value());
		Map<String, Object> map = configure.getMap();
		map.clear();
		map.put("name", user.getFirstName() + " " + user.getLastName());
		map.put("company_name", user.getCompany());
		map.put("email", user.getEmail());
		map.put("mobile", user.getPhoneNo());
		map.put("profile_pic", profilepic);
		structure.setData(map);
		structure.setMessage("Updated successfully");
		structure.setPlatform(null);
		structure.setStatus("success");
		structure.setPlatform(null);

		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
	}

	public int calculateRemainingPackageDays(QuantumShareUser user) {
		Instant localDate = Instant.now();
		int remainingDays = 0;
		if (user.isTrial()) {
			Instant trailDate = user.getSignUpDate();
			if ((freetrail - ChronoUnit.DAYS.between(trailDate, localDate)) > 0) {
				remainingDays = (int) (freetrail - ChronoUnit.DAYS.between(trailDate, localDate));
				return remainingDays;
			} else {
				remainingDays = 0;
				user.setTrial(false);
				userDao.saveUser(user);
				return remainingDays;
			}
		} else if (user.getSubscriptionDetails() != null && user.getSubscriptionDetails().isSubscribed()) {
			Instant subscriptionDate = user.getSubscriptionDetails().getSubscriptionDate();
			int subscriptiondays = user.getSubscriptionDetails().getSubscriptiondays();
			if ((subscriptiondays - ChronoUnit.DAYS.between(subscriptionDate, localDate)) > 0) {
				remainingDays = (int) (subscriptiondays - ChronoUnit.DAYS.between(subscriptionDate, localDate));
				return remainingDays;
			} else {
				remainingDays = 0;
				SubscriptionDetails subcribedUser = user.getSubscriptionDetails();
				subcribedUser.setSubscribed(false);
				subcribedUser.setSubscriptiondays(0);
				user.setSubscriptionDetails(subcribedUser);
				userDao.save(user);
				return remainingDays;
			}
		} else {
			return 0;
		}
	}

	public ResponseEntity<ResponseStructure<String>> forgetPassword(String email) {
		ResponseStructure<String> structure = new ResponseStructure<String>();
		QuantumShareUser user = userDao.findByEmail(email);
		if (user == null) {
			structure.setData(null);
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("No account found with this email address.");
			structure.setStatus("error");
			structure.setPlatform(null);
			structure.setPlatform(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		String verificationToken = UUID.randomUUID().toString();
		user.setVerificationToken(verificationToken);
		userDao.save(user);
		sendMail.sendVerificationEmail(user, "password_reset_request");

		structure.setData(null);
		structure.setMessage("Email has been sent to this mail to chnage your password");
		structure.setPlatform(null);
		structure.setStatus("success");
		structure.setCode(HttpStatus.OK.value());
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);

	}

	public ResponseEntity<ResponseStructure<String>> fetchUserInfo(int userId) {
		ResponseStructure<String> structure = new ResponseStructure<String>();
		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("user doesn't exists, please login");
			structure.setStatus("error");
			structure.setData(null);
			structure.setPlatform(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		boolean creditApplied = userTracking.applyCredit(user);
//		if (!creditApplied) {
//			System.out.println("false");
//			structure.setCode(HttpStatus.NOT_ACCEPTABLE.value());
//			structure.setMessage("Your package has expired. Please Upgrade your package");
//			structure.setStatus("error");
//			structure.setData(null);
//			structure.setPlatform(null);
//			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_ACCEPTABLE);
//		}

		CompletableFuture<Void> redditFuture = CompletableFuture
				.runAsync(() -> redditService.checkAndRefreshAccessToken(user));
		CompletableFuture<Void> youtubeFuture = CompletableFuture
				.runAsync(() -> youtubeService.ytCheckAndRefreshAccessToken(user));
		CompletableFuture<Void> twitterFuture = CompletableFuture
				.runAsync(() -> twitterService.checkAndRefreshAccessTokenTwitter(user));
		CompletableFuture.allOf(redditFuture, youtubeFuture, twitterFuture).join();

		Map<String, Object> map = configure.getMap();
		map.put("credit", user.getCreditSystem().getRemainingCredit());
		map.put("trail", user.isTrial());
		SubscriptionDetails subscription = user.getSubscriptionDetails();
		if (subscription == null)
			map.put("subscription", false);
		else if (!subscription.isSubscribed())
			map.put("subscription", false);
		else
			map.put("subscription", true);
		map.put("remainingdays", calculateRemainingPackageDays(user));
		structure.setData(map);
		structure.setCode(HttpStatus.OK.value());
		structure.setMessage(null);
		structure.setStatus("success");
		structure.setPlatform(null);
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
	}

	public ResponseEntity<ResponseStructure<String>> updatePassword(String password, String token) {
		ResponseStructure<String> structure = new ResponseStructure<String>();
		QuantumShareUser user = userDao.findByVerificationToken(token);
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("user doesn't exists, please login");
			structure.setStatus("error");
			structure.setData(null);
			structure.setPlatform(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		user.setPassword(SecurePassword.encrypt(password, "123"));
		user.setVerificationToken(null);
		userDao.save(user);
		sendMail.sendVerificationEmail(user, "password_updation");

		structure.setData(null);
		structure.setCode(HttpStatus.OK.value());
		structure.setMessage("Password has been updated successfully, please login");
		structure.setStatus("success");
		structure.setPlatform(null);
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
	}

	public ResponseEntity<ResponseStructure<String>> fetchConnectedFb(int userId) {
		ResponseStructure<String> structure = new ResponseStructure<String>();
		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("user doesn't exists, please login");
			structure.setStatus("error");
			structure.setData(null);
			structure.setPlatform(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		SocialAccounts accounts = user.getSocialAccounts();
		if (accounts == null || accounts.getFacebookUser() == null) {
			structure.setCode(119);
			structure.setMessage("user has not connected facebook platforms");
			structure.setPlatform("facebook");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		FaceBookUser fbuser = accounts.getFacebookUser();
		Map<String, Object> data = configure.getMap();
		data.clear();
		if (fbuser != null) {
			List<FacebookPageDetails> pages = fbuser.getPageDetails();
			Map<String, Object> pagedata = configure.getMap();
			pagedata.clear();
			for (FacebookPageDetails page : pages) {
				pagedata.put(page.getPageName(), page.getPictureUrl());
			}
			Map<String, Object> fb = configure.getMap();
			fb.clear();
			fb.put("facebookUrl", fbuser.getPictureUrl());
			fb.put("facebookUsername", fbuser.getFbuserUsername());
			fb.put("facebookNumberofpages", fbuser.getNoOfFbPages());
			fb.put("pages_url", pagedata);
			fb.put("user_id", userId);
			data.put("facebook", fb);
		}
		structure.setData(data);
		structure.setCode(HttpStatus.OK.value());
		structure.setMessage(null);
		structure.setStatus("success");
		structure.setPlatform(null);
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
	}

	public ResponseEntity<ResponseStructure<String>> fetchConnectedInsta1(int userId) {
		ResponseStructure<String> structure = new ResponseStructure<String>();
		QuantumShareUser user = userDao.fetchUser(userId);

		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("user doesn't exists, please login");
			structure.setStatus("error");
			structure.setData(null);
			structure.setPlatform(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		SocialAccounts accounts = user.getSocialAccounts();
		if (accounts == null || accounts.getInstagramUser() == null) {
			structure.setCode(119);
			structure.setMessage("user has not connected Instagram platforms");
			structure.setPlatform("instagram");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		InstagramUser instaUser = accounts.getInstagramUser();
		Map<String, Object> data = configure.getMap();
		data.clear();
		if (instaUser != null) {
			Map<String, Object> insta = configure.getMap();
			String instagramUrl;
			if (instaUser.getPictureUrl() == null) {
				instagramUrl = defaultProfile;
			} else {
				instagramUrl = instaUser.getPictureUrl();
			}
			insta.clear();
			insta.put("instagramUrl", instagramUrl);
			insta.put("InstagramUsername", instaUser.getInstaUsername());
			insta.put("Instagram_follwers_count", instaUser.getFollwersCount());
			insta.put("user_id", userId);
			data.put("instagram", insta);
		}
		structure.setData(data);
		structure.setCode(HttpStatus.OK.value());
		structure.setMessage(null);
		structure.setStatus("success");
		structure.setPlatform(null);
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
	}

	public ResponseEntity<ResponseStructure<String>> fetchConnectedTelegram(int userId) {
		ResponseStructure<String> structure = new ResponseStructure<String>();
		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("user doesn't exists, please login");
			structure.setStatus("error");
			structure.setData(null);
			structure.setPlatform(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		SocialAccounts accounts = user.getSocialAccounts();
		if (accounts == null || accounts.getTelegramUser() == null) {
			structure.setCode(119);
			structure.setMessage("user has not connected telegram platforms");
			structure.setPlatform("telegram");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		TelegramUser telegramUser = accounts.getTelegramUser();
		Map<String, Object> data = configure.getMap();
		data.clear();
		if (telegramUser != null) {
			Map<String, Object> telegram = configure.getMap();
			telegram.clear();
			String imageUrl;
			if (telegramUser.getTelegramProfileUrl() == null) {
				imageUrl = defaultProfile;
			} else {
				imageUrl = telegramUser.getTelegramProfileUrl();
			}
			telegram.put("telegramChatId", telegramUser.getTelegramChatId());
			telegram.put("telegramGroupName", telegramUser.getTelegramGroupName());
			telegram.put("telegramProfileUrl", imageUrl);
			telegram.put("telegramGroupMembersCount", telegramUser.getTelegramGroupMembersCount());
			telegram.put("user_id", userId);
			data.put("telegram", telegram);
		}
		structure.setData(data);
		structure.setCode(HttpStatus.OK.value());
		structure.setMessage(null);
		structure.setStatus("success");
		structure.setPlatform(null);
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
	}

	public ResponseEntity<ResponseStructure<String>> fetchLinkedIn(int userId) {
		ResponseStructure<String> structure = new ResponseStructure<String>();
		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("User doesn't exist, please log in");
			structure.setStatus("error");
			structure.setData(null);
			structure.setPlatform(null);
			return new ResponseEntity<>(structure, HttpStatus.NOT_FOUND);
		}

		SocialAccounts accounts = user.getSocialAccounts();
		if (accounts == null || (!accounts.isLinkedInPagePresent() && accounts.getLinkedInProfileDto() == null)) {
			structure.setCode(119);
			structure.setMessage("User has not connected LinkedIn platforms");
			structure.setPlatform("LinkedIn");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<>(structure, HttpStatus.NOT_FOUND);
		}

		Map<String, Object> data = new HashMap<>();
		Map<String, Object> linkedIn = new HashMap<>();

		// Map pages if they exist
		if (accounts.isLinkedInPagePresent()) {
			LinkedInPageDto page = accounts.getLinkedInPages(); // Assuming single page example
			linkedIn.clear();
			linkedIn.put("linkedInUserName", page.getLinkedinPageName());
			linkedIn.put("linkedInProfilePic", page.getLinkedinPageImage());
			linkedIn.put("linkedInFollowersCount", page.getLinkedinPageFollowers());
		} else {
			linkedIn.clear();
			LinkedInProfileDto linkedInUser = accounts.getLinkedInProfileDto();
			String imageUrl = (linkedInUser.getLinkedinProfileImage() == null) ? defaultProfile
					: linkedInUser.getLinkedinProfileImage();
			linkedIn.put("linkedInProfilePic", imageUrl);
			linkedIn.put("linkedInUserName", linkedInUser.getLinkedinProfileUserName());
		}
		data.put("linkedIn", linkedIn);
		structure.setData(data);
		structure.setCode(HttpStatus.OK.value());
		structure.setMessage(null);
		structure.setStatus("success");
		structure.setPlatform("LinkedIn");
		return new ResponseEntity<>(structure, HttpStatus.OK);
	}

	public ResponseEntity<ResponseStructure<String>> fetchConnectedYoutube(int userId) {
		ResponseStructure<String> structure = new ResponseStructure<String>();
		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setPlatform(null);

			structure.setMessage("user doesn't exists, please login");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		SocialAccounts accounts = user.getSocialAccounts();
		if (accounts == null || accounts.getYoutubeUser() == null) {

			structure.setCode(119);
			structure.setMessage("user has not connected youtube platforms");
			structure.setPlatform("youtube");
			structure.setStatus("error");
			structure.setData(null);

			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		YoutubeUser youTubeUser = accounts.getYoutubeUser();
		Map<String, Object> data = configure.getMap();
		data.clear();
		if (youTubeUser != null) {
			Map<String, Object> youtube = configure.getMap();
			String youTubeUrl;
			if (youTubeUser.getChannelImageUrl() == null) {
				youTubeUrl = defaultProfile;
			} else {
				youTubeUrl = youTubeUser.getChannelImageUrl();
			}
			youtube.clear();
			youtube.put("youtubeUrl", youTubeUrl);
			youtube.put("youtubeChannelName", youTubeUser.getChannelName());
			youtube.put("youtubeSubscriberCount", youTubeUser.getSubscriberCount());
			youtube.put("user_id", userId);
			data.put("youtube", youtube);
		}

		structure.setData(data);
		structure.setCode(HttpStatus.OK.value());
		structure.setMessage(null);
		structure.setStatus("success");
		structure.setPlatform("youtube");
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
	}

	public ResponseEntity<ResponseStructure<String>> emailVerify(String email) {
		ResponseStructure<String> structure = new ResponseStructure<String>();
		QuantumShareUser user = userDao.findByEmail(email);

		if (user == null) {
			structure.setCode(HttpStatus.OK.value());
			structure.setMessage("new user");
			structure.setData(null);
			structure.setPlatform(null);
			structure.setStatus("success");
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
		} else {
			String tokenValue = token.generateJWT(user);
			if (user.getPassword() == null || user.getPassword() == "" || user.getPassword().isBlank()
					|| user.getPassword().isEmpty()) {
				structure.setCode(120);
				structure.setMessage("Password null");
				structure.setData(user.getEmail());
				structure.setPlatform(null);
				structure.setStatus("error");
				return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_ACCEPTABLE);
			}
			structure.setCode(HttpStatus.OK.value());
			structure.setMessage("Account already exists with this email");
			structure.setData(tokenValue);
			structure.setPlatform(null);
			structure.setStatus("success");
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
		}
	}

	public ResponseEntity<ResponseStructure<String>> fetchConnectedReddit(int userId) {
		ResponseStructure<String> structure = new ResponseStructure<String>();
		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setPlatform(null);

			structure.setMessage("user doesn't exists, please login");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		SocialAccounts accounts = user.getSocialAccounts();
		if (accounts == null || accounts.getRedditDto() == null) {
			structure.setCode(119);
			structure.setMessage("user has not connected reddit platforms");
			structure.setPlatform("reddit");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		RedditDto redditDto = accounts.getRedditDto();
		Map<String, Object> data = configure.getMap();
		data.clear();
		if (redditDto != null) {
			Map<String, Object> reddit = configure.getMap();
			String reddit_image_url;
			if (redditDto.getRedditUserImage() == null) {
				reddit_image_url = defaultProfile;
			} else {
				reddit_image_url = redditDto.getRedditUserImage();
			}
			reddit.clear();
			reddit.put("redditProfileImage", reddit_image_url);
			reddit.put("redditUsername", redditDto.getRedditUsername());
			reddit.put("subscribersCount", redditDto.getRedditSubscribers());
			data.put("reddit", reddit);
		}
		structure.setData(data);
		structure.setCode(HttpStatus.OK.value());
		structure.setMessage(null);
		structure.setStatus("success");
		structure.setPlatform("reddit");
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
	}

	public ResponseEntity<ResponseStructure<String>> fetchConnectedTwitter(int userId) {
		ResponseStructure<String> structure = new ResponseStructure<String>();
		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setPlatform(null);
			structure.setMessage("user doesn't exists, please login");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		SocialAccounts accounts = user.getSocialAccounts();
		if (accounts == null || accounts.getTwitterUser() == null) {
			structure.setCode(119);
			structure.setMessage("user has not connected twitter platforms");
			structure.setPlatform("twitter");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		TwitterUser twitterUser = accounts.getTwitterUser();
		Map<String, Object> data = configure.getMap();
		data.clear();
		if (twitterUser != null) {
			Map<String, Object> twitter = configure.getMap();
			String twitter_profile_url;
			if (twitterUser.getPicture_url() == null) {
				twitter_profile_url = defaultProfile;
			} else {
				twitter_profile_url = twitterUser.getPicture_url();
			}
			twitter.clear();
			twitter.put("twitterProfileImage", twitter_profile_url);
			twitter.put("twitterUsername", twitterUser.getUserName());
			twitter.put("twitterFollowerCount", twitterUser.getFollower_count());
			data.put("twitter", twitter);
		}
		structure.setData(data);
		structure.setCode(HttpStatus.OK.value());
		structure.setMessage(null);
		structure.setStatus("success");
		structure.setPlatform("reddit");
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
	}

	public ResponseEntity<ResponseStructure<String>> fetchConnectedPlatform(int userId) {
		ResponseStructure<String> structure = new ResponseStructure<String>();
		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setPlatform(null);
			structure.setMessage("user doesn't exists, please login");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		SocialAccounts accounts = user.getSocialAccounts();
		if (accounts == null) {
			structure.setCode(119);
			structure.setMessage("user has not connected any socialmedia platforms");
			structure.setPlatform(null);
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		FaceBookUser fbuser = accounts.getFacebookUser();
		InstagramUser instaUser = accounts.getInstagramUser();
		RedditDto redditDto = accounts.getRedditDto();
		TelegramUser telegramUser = accounts.getTelegramUser();
		TwitterUser twitterUser = accounts.getTwitterUser();
		YoutubeUser youTubeUser = accounts.getYoutubeUser();

		Map<String, Object> data = configure.getMap();
		if (fbuser != null) {
			Map<String, Object> fb = configure.getMap();
			Map<String, Object> pagedata = configure.getMap();
			List<FacebookPageDetails> pages = fbuser.getPageDetails();
			pagedata.clear();
			for (FacebookPageDetails page : pages) {
				pagedata.put(page.getPageName(), page.getPictureUrl());
			}
			fb.put("facebookUrl", fbuser.getPictureUrl());
			fb.put("facebookUsername", fbuser.getFbuserUsername());
			fb.put("facebookNumberofpages", fbuser.getNoOfFbPages());
			fb.put("pages_url", pagedata);

			data.put("facebook", fb);
		}

		if (instaUser != null) {
			Map<String, Object> insta = configure.getMap();
			String instagramUrl;
			if (instaUser.getPictureUrl() == null) {
				instagramUrl = defaultProfile;
			} else {
				instagramUrl = instaUser.getPictureUrl();
			}
			insta.clear();
			insta.put("instagramUrl", instagramUrl);
			insta.put("InstagramUsername", instaUser.getInstaUsername());
			insta.put("Instagram_follwers_count", instaUser.getFollwersCount());
			data.put("instagram", insta);
		}
		Map<String, Object> linkedIn = configure.getMap();
		if (accounts.isLinkedInPagePresent()) {
			LinkedInPageDto page = accounts.getLinkedInPages(); // Assuming single page example
			linkedIn.clear();
			linkedIn.put("linkedInUserName", page.getLinkedinPageName());
			linkedIn.put("linkedInProfilePic", page.getLinkedinPageImage());
			linkedIn.put("linkedInFollowersCount", page.getLinkedinPageFollowers());
			data.put("linkedinPage", linkedIn);
		} else if (accounts.getLinkedInProfileDto() != null) {
			LinkedInProfileDto linkedInUser = accounts.getLinkedInProfileDto();
			String imageUrl = (linkedInUser.getLinkedinProfileImage() == null) ? defaultProfile
					: linkedInUser.getLinkedinProfileImage();
			linkedIn.put("linkedInProfilePic", imageUrl);
			linkedIn.put("linkedInUserName", linkedInUser.getLinkedinProfileUserName());
			data.put("linkedINprofile", linkedIn);
			data.put("linkedinProfile", linkedIn);
		}

		if (youTubeUser != null) {
			Map<String, Object> youtube = configure.getMap();
			String youTubeUrl;
			if (youTubeUser.getChannelImageUrl() == null) {
				youTubeUrl = defaultProfile;
			} else {
				youTubeUrl = youTubeUser.getChannelImageUrl();
			}
			youtube.clear();
			youtube.put("youtubeUrl", youTubeUrl);
			youtube.put("youtubeChannelName", youTubeUser.getChannelName());
			youtube.put("youtubeSubscriberCount", youTubeUser.getSubscriberCount());
			youtube.put("user_id", userId);
			data.put("youtube", youtube);
		}

		if (redditDto != null) {
			Map<String, Object> reddit = configure.getMap();
			String reddit_image_url;
			if (redditDto.getRedditUserImage() == null) {
				reddit_image_url = defaultProfile;
			} else {
				reddit_image_url = redditDto.getRedditUserImage();
			}
			reddit.put("redditProfileImage", reddit_image_url);
			reddit.put("redditUsername", redditDto.getRedditUsername());
			reddit.put("subscribersCount", redditDto.getRedditSubscribers());
			data.put("reddit", reddit);
		}

		if (twitterUser != null) {
			Map<String, Object> twitter = configure.getMap();
			String twitter_profile_url;
			if (twitterUser.getPicture_url() == null) {
				twitter_profile_url = defaultProfile;
			} else {
				twitter_profile_url = twitterUser.getPicture_url();
			}
			twitter.clear();
			twitter.put("twitterProfileImage", twitter_profile_url);
			twitter.put("twitterUsername", twitterUser.getUserName());
			twitter.put("twitterFollowerCount", twitterUser.getFollower_count());
			data.put("twitter", twitter);
		}

		if (telegramUser != null) {
			Map<String, Object> telegram = configure.getMap();
			String imageUrl;
			if (telegramUser.getTelegramProfileUrl() == null) {
				imageUrl = defaultProfile;
			} else {
				imageUrl = telegramUser.getTelegramProfileUrl();
			}
			telegram.put("telegramChatId", telegramUser.getTelegramChatId());
			telegram.put("telegramGroupName", telegramUser.getTelegramGroupName());
			telegram.put("telegramProfileUrl", imageUrl);
			telegram.put("telegramGroupMembersCount", telegramUser.getTelegramGroupMembersCount());
			data.put("telegram", telegram);
		}

		structure.setCode(HttpStatus.OK.value());
		structure.setPlatform(null);
		structure.setMessage(null);
		structure.setStatus("success");
		structure.setData(data);
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);

	}

	public ResponseEntity<ResponseStructure<String>> fetchConnectedPinterest(int userId) {
		ResponseStructure<String> structure = new ResponseStructure<String>();
		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setPlatform(null);
			structure.setMessage("user doesn't exists, please login");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		SocialAccounts accounts = user.getSocialAccounts();
		if (accounts == null || accounts.getPinterestUser() == null) {
			structure.setCode(119);
			structure.setMessage("user has not connected pinterest");
			structure.setPlatform("pinterest");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		PinterestUser pinterestUser = accounts.getPinterestUser();
		Map<String, Object> data = configure.getMap();
		data.clear();
		if (pinterestUser != null) {
			Map<String, Object> pinterest = configure.getMap();
			String pinterestUrl;
			if (pinterestUser.getPinterestProfileImage() == null) {
				pinterestUrl = defaultProfile;
			} else {
				pinterestUrl = pinterestUser.getPinterestProfileImage();
			}
			pinterest.clear();
			pinterest.put("pinterestProfileImage", pinterestUrl);
			pinterest.put("pinterestProfileName", pinterestUser.getPinterestProfileName());
			pinterest.put("pinterestBoardDetails", pinterestUser.getPinterestBoardDetails());
			pinterest.put("pinterestFollowersCount", pinterestUser.getPinterestFollowersCount());
			pinterest.put("user_id", userId);
			data.put("pinterest", pinterest);
		}

		structure.setData(data);
		structure.setCode(HttpStatus.OK.value());
		structure.setMessage(null);
		structure.setStatus("success");
		structure.setPlatform("pinterest");
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
	}

}
