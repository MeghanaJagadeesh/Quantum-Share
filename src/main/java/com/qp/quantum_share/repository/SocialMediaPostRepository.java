package com.qp.quantum_share.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.qp.quantum_share.dto.SocialMediaPosts;

public interface SocialMediaPostRepository extends JpaRepository<SocialMediaPosts, Integer> {
	
	 @Query("SELECT sp FROM SocialMediaPosts sp WHERE sp.id IN " +
	           "(SELECT p.id FROM QuantumShareUser q JOIN q.posts p WHERE q.userId = :userId) " +
	           "ORDER BY sp.postDate DESC, sp.postTime DESC LIMIT 10")
	    List<SocialMediaPosts> findTop10PostsByUserId(@Param("userId") int userId);

	 
	 @Query("SELECT sp FROM SocialMediaPosts sp WHERE sp.id IN " +
	           "(SELECT p.id FROM QuantumShareUser q JOIN q.posts p WHERE q.userId = :userId) " +
	           "ORDER BY sp.postDate DESC, sp.postTime DESC LIMIT 20")
	    List<SocialMediaPosts> findTop20PostsByUserId(@Param("userId") int userId);


	SocialMediaPosts findByPostid(String pid);

	
}
