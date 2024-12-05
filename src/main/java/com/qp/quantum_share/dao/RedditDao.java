package com.qp.quantum_share.dao;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qp.quantum_share.dto.RedditDto;
import com.qp.quantum_share.repository.RedditRepository;


@Service
public class RedditDao {

	@Autowired
	RedditRepository redditRepository;
	
	public void saveReddit(RedditDto redditDto)
	{
		redditRepository.save(redditDto);
	}
	
	public Optional<RedditDto> findById(int id)
	{
		return redditRepository.findById(id);
	}

	public void deleteUser(RedditDto deleteUser) {
		redditRepository.delete(deleteUser);
		
	}
}

