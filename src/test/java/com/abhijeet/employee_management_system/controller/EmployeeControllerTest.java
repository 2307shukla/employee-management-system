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
    @WithMockUser(roles = "ADMIN")
    void createAndFetchEmployee() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
            "firstName", "Jane", "lastName", "Doe",
            "email", "jane.doe@example.com", "department", "Engineering",
            "position", "Developer", "salary", 75000.0));

        String response = mockMvc.perform(post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.email").value("jane.doe@example.com"))
            .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/employees/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("Jane"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employeeRoleCanListButNotCreate() throws Exception {
        mockMvc.perform(get("/api/employees"))
            .andExpect(status().isOk());

        String body = objectMapper.writeValueAsString(Map.of(
            "firstName", "Should", "lastName", "Fail", "email", "should.fail@example.com"));
        mockMvc.perform(post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employeeRoleCannotDeleteOrUpdate() throws Exception {
        mockMvc.perform(put("/api/employees/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "firstName", "X", "lastName", "Y", "email", "x.y@example.com"))))
            .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/employees/1"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void creatingEmployeeWithoutEmailFailsValidation() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
            "firstName", "No", "lastName", "Email"));
        mockMvc.perform(post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.email").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void creatingEmployeeWithInvalidEmailFailsValidation() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
            "firstName", "Bad", "lastName", "Email", "email", "not-an-email"));
        mockMvc.perform(post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.email").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deletingAnEmployeeRemovesIt() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
            "firstName", "To", "lastName", "Delete", "email", "to.delete@example.com"));
        String response = mockMvc.perform(post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(delete("/api/employees/" + id))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/employees/" + id))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void fetchingUnknownEmployeeReturns404() throws Exception {
        mockMvc.perform(get("/api/employees/99999999"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updatingUnknownEmployeeReturns404() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
            "firstName", "Ghost", "lastName", "Employee", "email", "ghost@example.com"));
        mockMvc.perform(put("/api/employees/99999999")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listIsPaginatedAndSortable() throws Exception {
        for (int i = 0; i < 3; i++) {
            String body = objectMapper.writeValueAsString(Map.of(
                "firstName", "Page" + i, "lastName", "Test", "email", "page" + i + "@example.com"));
            mockMvc.perform(post("/api/employees")
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/employees?page=0&size=2&sort=firstName,asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.page.totalElements").value(3))
            .andExpect(jsonPath("$.page.totalPages").value(2))
            .andExpect(jsonPath("$.content[0].firstName").value("Page0"))
            .andExpect(jsonPath("$.content[1].firstName").value("Page1"));
    }
}
