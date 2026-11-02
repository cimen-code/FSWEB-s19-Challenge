package com.workintech.s19challenge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CommentRequest(
    @NotNull
    @Positive
    Long tweetId,

    @NotBlank
    @Size(max = 280)
    String content
) {
}
