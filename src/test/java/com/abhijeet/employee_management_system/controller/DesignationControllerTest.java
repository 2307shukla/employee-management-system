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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "jwt.secret=test-only-secret-not-used-anywhere-else-0123456789")
@AutoConfigureMockMvc
@Transactional
class DesignationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/designations"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createListAndFetchDesignation() throws Exception {
        String response = mockMvc.perform(post("/api/designations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("title", "Senior Engineer"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Senior Engineer"))
            .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/designations/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Senior Engineer"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employeeRoleCanListButNotCreate() throws Exception {
        mockMvc.perform(get("/api/designations"))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/designations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("title", "Should Fail"))))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void creatingWithoutTitleFailsValidation() throws Exception {
        mockMvc.perform(post("/api/designations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.title").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateAndDeleteDesignation() throws Exception {
        String response = mockMvc.perform(post("/api/designations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("title", "Manager"))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(put("/api/designations/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("title", "Senior Manager"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Senior Manager"));

        mockMvc.perform(delete("/api/designations/" + id))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/designations/" + id))
            .andExpect(status().isNotFound());
    }
}
