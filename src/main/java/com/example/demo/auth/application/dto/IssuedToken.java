package com.example.demo.auth.application.dto;

import java.time.Instant;
import java.util.List;

public record IssuedToken(
    String accessToken,
    String tokenType,
    Instant expiresAt,
    String memberId,
    List<String> roles
) {
}
