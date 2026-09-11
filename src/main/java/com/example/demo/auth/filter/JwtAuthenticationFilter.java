package com.example.demo.auth.filter;

import com.example.demo.auth.application.JwtAuthenticationFactory;
import com.example.demo.auth.application.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final JwtAuthenticationFactory authenticationFactory;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = bearerToken(authorization);
            Claims claims = jwtUtil.validateToken(token);
            if (claims == null) {
                throw new BadCredentialsException("유효하지 않은 JWT입니다.");
            }
            Authentication authentication = authenticationFactory.create(claims);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (AuthenticationException exception) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(
                request,
                response,
                new BadCredentialsException("유효하지 않은 JWT입니다.", exception)
            );
        }
    }

    private String bearerToken(String authorization) {
        if (!authorization.startsWith(BEARER_PREFIX)) {
            throw new BadCredentialsException("Authorization Header는 Bearer 방식이어야 합니다.");
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new BadCredentialsException("Bearer JWT가 필요합니다.");
        }

        return token;
    }
}
