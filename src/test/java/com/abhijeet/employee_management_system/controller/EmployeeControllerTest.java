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
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Spring Boot 4.x's auto-configured ObjectMapper bean is Jackson 3.x
    // (tools.jackson.databind.ObjectMapper) — this project still uses
    // Jackson 2.x (com.fasterxml.jackson.*) elsewhere (via jjwt-jackson), so
    // a plain unmanaged instance here avoids the Jackson-2-vs-3 mismatch.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void listEmployeesRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/employees"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void createAndFetchEmployee() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
            "firstName", "Jane", "lastName", "Doe",
            "email", "jane.doe@example.com", "department", "Engineering",
            "position", "Developer", "salary", 75000.0));

        String response = mockMvc.perform(post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.email").value("jane.doe@example.com"))
            .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/employees/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("Jane"));
    }

    @Test
    @WithMockUser
    void creatingEmployeeWithoutEmailFailsValidation() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
            "firstName", "No", "lastName", "Email"));
        mockMvc.perform(post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.email").exists());
    }

    @Test
    @WithMockUser
    void creatingEmployeeWithInvalidEmailFailsValidation() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
            "firstName", "Bad", "lastName", "Email", "email", "not-an-email"));
        mockMvc.perform(post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.email").exists());
    }

    @Test
    @WithMockUser
    void deletingAnEmployeeRemovesIt() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
            "firstName", "To", "lastName", "Delete", "email", "to.delete@example.com"));
        String response = mockMvc.perform(post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(delete("/api/employees/" + id))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/employees/" + id))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void fetchingUnknownEmployeeReturns404() throws Exception {
        mockMvc.perform(get("/api/employees/99999999"))
            .andExpect(status().isNotFound());
    }
}
