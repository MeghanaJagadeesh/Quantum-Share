package com.qp.quantum_share.dto;

import java.util.List;

import org.springframework.stereotype.Component;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Component
@Table(name = "socialaccounts")
public class SocialAccounts {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int socialAccountId;
	
	private boolean LinkedInPagePresent;
	
	@OneToOne(cascade = CascadeType.ALL)
	private FaceBookUser facebookUser;

	@OneToOne(cascade = CascadeType.ALL)
	private InstagramUser instagramUser;
	
	@OneToOne(cascade = CascadeType.ALL)
	private TelegramUser telegramUser;
	
	@OneToOne(cascade = CascadeType.ALL)
	private TwitterUser twitterUser;
	
	@OneToOne(cascade = CascadeType.ALL)
	private LinkedInProfileDto linkedInProfileDto;

//    @OneToOne(cascade = CascadeType.ALL)
//    private List<LinkedInPageDto> linkedInPages;

    @OneToOne(cascade = CascadeType.ALL)
    private LinkedInPageDto linkedInPages;

    
	@OneToOne(cascade = CascadeType.ALL)
	private YoutubeUser youtubeUser;
	
	@OneToOne(cascade = CascadeType.ALL)
	private RedditDto redditDto; 
	
	@OneToOne(cascade = CascadeType.ALL)
	private PinterestUser pinterestUser; 
}
