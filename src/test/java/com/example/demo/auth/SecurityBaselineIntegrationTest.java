package com.example.demo.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityBaselineIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

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
    void token이_없으면_라이브_채티방에_참여할_수_없다() throws Exception {
        mockMvc.perform(post("/api/live-sales/sale-1/chat/join"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void jwt_subject가_라이브_채티방_참여자가_된다() throws Exception {
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
