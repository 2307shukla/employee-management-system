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
class DepartmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/departments"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createListAndFetchDepartment() throws Exception {
        String response = mockMvc.perform(post("/api/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", "Finance"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Finance"))
            .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/departments/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Finance"));

        mockMvc.perform(get("/api/departments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").exists());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employeeRoleCanListButNotCreate() throws Exception {
        mockMvc.perform(get("/api/departments"))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", "Should Fail"))))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void creatingWithoutNameFailsValidation() throws Exception {
        mockMvc.perform(post("/api/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.name").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateAndDeleteDepartment() throws Exception {
        String response = mockMvc.perform(post("/api/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", "HR"))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(put("/api/departments/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", "Human Resources"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Human Resources"));

        mockMvc.perform(delete("/api/departments/" + id))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/departments/" + id))
            .andExpect(status().isNotFound());
    }
}
