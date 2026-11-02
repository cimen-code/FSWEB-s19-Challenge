package com.workintech.s19challenge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRequest(
    @NotBlank
    @Size(max = 100)
    String username,

    @NotBlank
    @Size(min = 8, max = 72)
    String password
) {
}
