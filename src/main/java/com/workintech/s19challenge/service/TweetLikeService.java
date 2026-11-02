package com.workintech.s19challenge.service;

import com.workintech.s19challenge.dto.TweetLikeResponse;

public interface TweetLikeService {

    TweetLikeResponse like(
        String username,
        Long tweetId
    );

    void dislike(
        String username,
        Long tweetId
    );
}
