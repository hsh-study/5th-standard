package com.example.demo.auth.api.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank String memberId, @NotBlank String password
) {
}
