package com.abhijeet.employee_management_system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "jwt.secret=test-only-secret-not-used-anywhere-else-0123456789")
@AutoConfigureMockMvc
@Transactional
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static org.springframework.test.web.servlet.request.RequestPostProcessor adminAuth() {
        return user("admin").roles("ADMIN");
    }

    @Test
    void listingUsersRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employeeCannotListUsersOrChangeRoles() throws Exception {
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/users/1/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("role", "HR"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminCanPromoteUserToHr() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("username", "promoteme", "password", "pass1234"))))
            .andExpect(status().isCreated());

        String response = mockMvc.perform(get("/api/users").with(adminAuth()))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        var users = objectMapper.readTree(response);
        Long userId = null;
        for (var node : users) {
            if (node.get("username").asText().equals("promoteme")) {
                userId = node.get("id").asLong();
            }
        }

        mockMvc.perform(put("/api/users/" + userId + "/role").with(adminAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("role", "HR"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("HR"));

        mockMvc.perform(get("/api/dashboard").with(user("promoteme").roles("HR")))
            .andExpect(status().isOk());
    }

    @Test
    void userListNeverExposesPasswordHash() throws Exception {
        mockMvc.perform(get("/api/users").with(adminAuth()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].passwordHash").doesNotExist());
    }
}
