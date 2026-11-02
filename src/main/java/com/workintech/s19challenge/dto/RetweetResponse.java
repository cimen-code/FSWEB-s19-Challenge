package com.workintech.s19challenge.dto;

public record RetweetResponse(
    Long id,
    Long tweetId,
    Long userId
) {
}
