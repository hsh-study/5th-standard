package com.example.demo.auth;

import com.example.demo.member.domain.Member;
import com.example.demo.member.domain.MemberRepository;
import com.example.demo.member.domain.MemberRole;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/sql/session-23-members.sql")
class SecurityBaselineIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void 교육용_회원과_역할을_DB에_저장한다() {
        var member = memberRepository.findById("member-1").orElseThrow();

        assertThat(member.getDisplayName()).isEqualTo("회원 1");
        assertThat(member.getEncodedPassword()).isNotEqualTo("password");
        assertThat(passwordEncoder.matches("password", member.getEncodedPassword())).isTrue();
        assertThat(member.getRoles())
            .extracting(Enum::name)
            .containsExactly("MEMBER");
    }

    @Test
    void 로그인하면_서명된_jwt를_발급한다() throws Exception {
        mockMvc.perform(post("/api/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson("member-1")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.memberId").value("member-1"))
            .andExpect(jsonPath("$.roles[0]").value("MEMBER"));
    }

    @Test
    void DB에_없는_회원은_로그인할_수_없다() throws Exception {
        mockMvc.perform(post("/api/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson("unknown-member")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void 틀린_비밀번호도_회원이_없는_경우와_같은_401로_처리한다() throws Exception {
        mockMvc.perform(post("/api/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":\"member-1\",\"password\":\"wrong-password\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void DB에서_정지한_회원은_로그인할_수_없다() throws Exception {
        memberRepository.save(new Member(
            "disabled-member",
            "정지 회원",
            passwordEncoder.encode("password"),
            false,
            Set.of(MemberRole.MEMBER)));

        mockMvc.perform(post("/api/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson("disabled-member")))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void DB에_저장한_관리자_역할이_jwt에_담긴다() throws Exception {
        mockMvc.perform(post("/api/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson("admin")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.roles[0]").value("ADMIN"))
            .andExpect(jsonPath("$.roles[1]").value("MEMBER"));
    }

    @Test
    void token이_없으면_라이브_채팅방에_참여할_수_없다() throws Exception {
        mockMvc.perform(post("/api/live-sales/sale-1/chat/join"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void jwt_subject가_라이브_채팅방_참여자가_된다() throws Exception {
        String token = login("member-1");

        mockMvc.perform(post("/api/live-sales/sale-1/chat/join")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.liveSaleId").value("sale-1"))
            .andExpect(jsonPath("$.memberId").value("member-1"));
    }

    private String login(String memberId) throws Exception {
        String response = mockMvc.perform(post("/api/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(memberId)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.get("accessToken").asText();
    }

    private String loginJson(String memberId) {
        return "{\"memberId\":\"" + memberId + "\",\"password\":\"password\"}";
    }
}
