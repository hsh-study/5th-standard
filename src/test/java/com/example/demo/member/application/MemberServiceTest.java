package com.example.demo.member.application;

import com.example.demo.member.domain.Member;
import com.example.demo.member.domain.MemberRepository;
import com.example.demo.member.domain.MemberRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    MemberRepository memberRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    MemberService memberService;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode(anyString())).thenReturn("dummy-password-hash");
        memberService = new MemberService(memberRepository, passwordEncoder);
        memberService.encodeDummyPassword();
    }

    @Test
    void 회원과_비밀번호가_일치하면_인증된_권한을_반환한다() {
        Member member = Member.create(
                "admin",
                "관리자",
                "encoded-password",
                true,
                Set.of(MemberRole.MEMBER, MemberRole.ADMIN));
        when(memberRepository.findById("admin")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);

        var authentication = memberService.authenticate("admin", "password");

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getName()).isEqualTo("admin");
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN", "ROLE_MEMBER");
    }

    @Test
    void 없는_회원도_dummy_hash를_비교한_뒤_같은_인증_실패로_처리한다() {
        when(memberRepository.findById("unknown-member")).thenReturn(Optional.empty());
        when(passwordEncoder.matches("wrong-password", "dummy-password-hash")).thenReturn(false);

        assertThatThrownBy(() -> memberService.authenticate("unknown-member", "wrong-password"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("인증에 실패했습니다.");
        verify(passwordEncoder).matches("wrong-password", "dummy-password-hash");
    }
}
