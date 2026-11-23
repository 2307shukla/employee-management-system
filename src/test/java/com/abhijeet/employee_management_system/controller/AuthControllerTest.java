package com.abhijeet.employee_management_system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "jwt.secret=test-only-secret-not-used-anywhere-else-0123456789")
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Spring Boot 4.x's auto-configured ObjectMapper bean is Jackson 3.x
    // (tools.jackson.databind.ObjectMapper) — this project still uses
    // Jackson 2.x (com.fasterxml.jackson.*) elsewhere (via jjwt-jackson), so
    // a plain unmanaged instance here avoids the Jackson-2-vs-3 mismatch.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void registerThenLoginSucceeds() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
            "username", "alice", "password", "a-strong-password"));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.username").value("alice"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void registeringTheSameUsernameTwiceFails() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
            "username", "bob", "password", "a-strong-password"));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isConflict());
    }

    @Test
    void loginWithWrongPasswordFails() throws Exception {
        String registerBody = objectMapper.writeValueAsString(Map.of(
            "username", "carol", "password", "the-real-password"));
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(registerBody))
            .andExpect(status().isCreated());

        String wrongBody = objectMapper.writeValueAsString(Map.of(
            "username", "carol", "password", "totally-wrong"));
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(wrongBody))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithUnknownUsernameFails() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
            "username", "nobody-registered", "password", "whatever123"));
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void registerWithShortPasswordFailsValidation() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
            "username", "dave", "password", "short"));
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.password").exists());
    }
}
