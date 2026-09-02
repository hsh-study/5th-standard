package com.example.demo.member.application;

import com.example.demo.member.domain.Member;
import com.example.demo.member.domain.MemberRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class MemberService {

    private static final String AUTHENTICATION_FAILED_MESSAGE = "인증에 실패했습니다.";
    private static final String NOT_FOUND_PROTECTION_PASSWORD = "member-not-found-protection";

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final String dummyPasswordHash;

    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.dummyPasswordHash = passwordEncoder.encode(NOT_FOUND_PROTECTION_PASSWORD);
    }

    public Authentication authenticate(String memberId, String rawPassword) {
        Member member = memberRepository.findById(memberId).orElse(null);
        String storedPassword = member == null ? dummyPasswordHash : member.getEncodedPassword();
        boolean passwordMatches = passwordEncoder.matches(rawPassword, storedPassword);

        if (member == null || !member.isEnabled() || !passwordMatches) {
            throw new BadCredentialsException(AUTHENTICATION_FAILED_MESSAGE);
        }

        List<GrantedAuthority> authorities = member.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
            .map(GrantedAuthority.class::cast)
            .sorted((left, right) -> left.getAuthority().compareTo(right.getAuthority()))
            .toList();

        return UsernamePasswordAuthenticationToken.authenticated(
            member.getMemberId(),
            null,
            authorities
        );
    }

}
