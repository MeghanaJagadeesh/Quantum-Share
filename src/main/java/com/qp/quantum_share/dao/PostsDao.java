package com.qp.quantum_share.dao;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.qp.quantum_share.dto.SocialMediaPosts;
import com.qp.quantum_share.repository.PostsRepository;
import com.qp.quantum_share.repository.SocialMediaPostRepository;

import jakarta.transaction.Transactional;

@Component
public class PostsDao {

	@Autowired
	PostsRepository postsRepository;

	@Autowired
	SocialMediaPostRepository mediaPostRepository;

	@Transactional
	public void deletePages(List<SocialMediaPosts> posts) {
		System.out.println("deleteposts");
		List<SocialMediaPosts> postsToRemove = posts.stream().collect(Collectors.toList());
		postsToRemove.forEach(post -> {
			postsRepository.delete(post);
		});
	}

	public SocialMediaPosts getPost(int pid) {
		return mediaPostRepository.findById(pid).get();
	}

	public List<SocialMediaPosts> getRecentPosts(int userId) {
		return mediaPostRepository.findTop10PostsByUserId(userId);
	}

	public List<SocialMediaPosts> getRecent20Posts(int userId) {
		return mediaPostRepository.findTop20PostsByUserId(userId);
	}

	public SocialMediaPosts getPostByPostId(String postId) {
		return mediaPostRepository.findByPostid(postId);
	}

	public void deletePosts(SocialMediaPosts post) {

		mediaPostRepository.delete(post);
	}
}
