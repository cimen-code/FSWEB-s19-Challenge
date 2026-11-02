package com.workintech.s19challenge.dto;

public record TweetResponse(
    Long id,
    Long userId,
    String username,
    String content
) {
}
