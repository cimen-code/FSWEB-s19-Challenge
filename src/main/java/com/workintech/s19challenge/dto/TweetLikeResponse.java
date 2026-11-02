package com.workintech.s19challenge.dto;

public record TweetLikeResponse(
    Long id,
    Long tweetId,
    Long userId
) {
}
