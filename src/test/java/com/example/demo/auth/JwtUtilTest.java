package com.example.demo.auth;

import com.example.demo.auth.application.JwtUtil;
import com.example.demo.auth.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private static final String BASE64_SECRET =
            "dGVzdC1zZWNyZXQtbXVzdC1iZS1hdC1sZWFzdC10aGlydHktdHdvLWJ5dGVzLWxvbmc=";

    private final JwtUtil jwtUtil = new JwtUtil(new JwtProperties(
            "live-commerce-standard-test",
            Duration.ofMinutes(30),
            BASE64_SECRET
    ));

    @Test
    @SuppressWarnings("unchecked")
    void 발급한_token을_검증하면_subject와_역할을_복원한다() {
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                "member-1",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))
        );

        var issuedToken = jwtUtil.generateToken(authentication);
        Claims claims = jwtUtil.validateToken(issuedToken.accessToken());

        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo("member-1");
        assertThat(claims.get("roles", List.class)).containsExactly("MEMBER");
        assertThat(claims.getIssuer()).isEqualTo("live-commerce-standard-test");
    }

    @Test
    void 변조된_token은_null을_반환한다() {
        assertThat(jwtUtil.validateToken("tampered-token")).isNull();
    }

    @Test
    void 만료된_token은_null을_반환한다() {
        Instant now = Instant.now();
        String expiredToken = token("live-commerce-standard-test", now.minusSeconds(120), now.minusSeconds(60));

        assertThat(jwtUtil.validateToken(expiredToken)).isNull();
    }

    @Test
    void issuer가_다른_token은_null을_반환한다() {
        Instant now = Instant.now();
        String otherIssuerToken = token("other-service", now, now.plusSeconds(1800));

        assertThat(jwtUtil.validateToken(otherIssuerToken)).isNull();
    }

    private String token(String issuer, Instant issuedAt, Instant expiresAt) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(BASE64_SECRET));
        return Jwts.builder()
                .issuer(issuer)
                .subject("member-1")
                .claim("roles", List.of("MEMBER"))
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }
}
