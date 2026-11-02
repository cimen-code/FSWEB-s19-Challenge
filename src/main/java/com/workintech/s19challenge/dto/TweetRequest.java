package com.workintech.s19challenge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TweetRequest(
    @NotBlank @Size(max = 280) String content
) {
}
