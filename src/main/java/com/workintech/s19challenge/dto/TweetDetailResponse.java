package com.workintech.s19challenge.dto;

import java.util.List;

public record TweetDetailResponse(
    Long id,
    Long userId,
    String username,
    String content,
    List<CommentResponse> comments,
    long likeCount,
    long retweetCount
) {
}
