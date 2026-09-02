package com.example.demo.auth.api;

import com.example.demo.auth.api.request.LoginRequest;
import com.example.demo.auth.application.JwtUtil;
import com.example.demo.auth.application.dto.IssuedToken;
import com.example.demo.member.application.MemberService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final MemberService memberService;
    private final JwtUtil jwtUtil;

    public AuthController(MemberService memberService, JwtUtil jwtUtil) {
        this.memberService = memberService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/token")
    public IssuedToken token(@Valid @RequestBody LoginRequest request) {
        var authentication = memberService.authenticate(request.memberId(), request.password());
        return jwtUtil.generateToken(authentication);
    }
}
