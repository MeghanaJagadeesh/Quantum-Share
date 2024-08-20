package com.qp.quantum_share.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.qp.quantum_share.dto.SocialMediaPosts;

public interface PostsRepository extends JpaRepository<SocialMediaPosts, Integer>{
	

}
