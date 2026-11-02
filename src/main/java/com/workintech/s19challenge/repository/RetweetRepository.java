package com.workintech.s19challenge.repository;

import com.workintech.s19challenge.entity.Retweet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RetweetRepository
        extends JpaRepository<Retweet, Long> {

    boolean existsByUser_IdAndTweet_Id(
        Long userId,
        Long tweetId
    );

    long countByTweet_Id(Long tweetId);
}
