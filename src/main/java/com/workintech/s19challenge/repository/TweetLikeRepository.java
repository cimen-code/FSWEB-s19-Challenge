package com.workintech.s19challenge.repository;

import com.workintech.s19challenge.entity.TweetLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TweetLikeRepository
        extends JpaRepository<TweetLike, Long> {

    boolean existsByUser_IdAndTweet_Id(
        Long userId,
        Long tweetId
    );

    Optional<TweetLike> findByUser_IdAndTweet_Id(
        Long userId,
        Long tweetId
    );

    long countByTweet_Id(Long tweetId);
}
