package com.workintech.s19challenge.dto;

public record CommentResponse(
    Long id,
    Long tweetId,
    Long userId,
    String username,
    String content
) {
}
