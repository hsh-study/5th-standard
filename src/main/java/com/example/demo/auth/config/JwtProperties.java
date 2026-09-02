package com.example.demo.auth.config;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.DecodingException;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(
        String issuer,
        Duration accessTokenTtl,
        String secret
) {
    public JwtProperties {
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("JWT issuer는 비어 있을 수 없습니다.");
        }
        if (accessTokenTtl == null || accessTokenTtl.isZero() || accessTokenTtl.isNegative()) {
            throw new IllegalArgumentException("JWT 만료 시간은 0보다 커야 합니다.");
        }
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("Base64 JWT secret은 비어 있을 수 없습니다.");
        }
        try {
            if (Decoders.BASE64.decode(secret).length < 32) {
                throw new IllegalArgumentException("HS256 JWT secret은 Base64 복호화 후 최소 32바이트여야 합니다.");
            }
        } catch (DecodingException exception) {
            throw new IllegalArgumentException("JWT secret은 올바른 Base64 문자열이어야 합니다.", exception);
        }
    }
}
