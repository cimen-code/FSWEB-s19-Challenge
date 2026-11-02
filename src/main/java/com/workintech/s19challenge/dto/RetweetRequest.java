package com.workintech.s19challenge.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RetweetRequest(
    @NotNull
    @Positive
    Long tweetId
) {
}
