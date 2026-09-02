package com.example.demo.auth.application;

import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JwtAuthenticationFactory {

    public Authentication create(Claims claims) {
        String memberId = claims.getSubject();
        if (memberId == null || memberId.isBlank()) {
            throw new BadCredentialsException("JWT sub가 필요합니다.");
        }

        List<?> roles = claims.get("roles", List.class);
        List<SimpleGrantedAuthority> authorities = roles == null
            ? List.of()
            : roles.stream()
            .map(String::valueOf)
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .toList();

        return new UsernamePasswordAuthenticationToken(memberId, null, authorities);
    }
}
