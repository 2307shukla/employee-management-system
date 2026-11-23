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
import java.util.HashMap;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "jwt.secret=test-only-secret-not-used-anywhere-else-0123456789")
@AutoConfigureMockMvc
@Transactional
class AttendanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static org.springframework.test.web.servlet.request.RequestPostProcessor adminAuth() {
        return user("admin").roles("ADMIN");
    }

    private String registerLinkedEmployee(String username, String employeeCode) throws Exception {
        Long deptId = objectMapper.readTree(mockMvc.perform(post("/api/departments").with(adminAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", "Dept-" + username))))
            .andReturn().getResponse().getContentAsString()).get("id").asLong();

        Map<String, Object> employee = new HashMap<>();
        employee.put("employeeCode", employeeCode);
        employee.put("firstName", username);
        employee.put("lastName", "Test");
        employee.put("email", username + "@example.com");
        employee.put("departmentId", deptId);
        mockMvc.perform(post("/api/employees").with(adminAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(employee)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    Map.of("username", username, "password", "pass1234", "employeeCode", employeeCode))))
            .andExpect(status().isCreated());

        return username;
    }

    @Test
    void attendanceRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/attendance/check-in"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void checkInThenCheckOutSucceeds() throws Exception {
        String username = registerLinkedEmployee("att1", "EMP-ATT-1");

        mockMvc.perform(post("/api/attendance/check-in").with(user(username).roles("EMPLOYEE")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.checkIn").exists())
            .andExpect(jsonPath("$.checkOut").doesNotExist());

        mockMvc.perform(post("/api/attendance/check-out").with(user(username).roles("EMPLOYEE")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.checkOut").exists());

        mockMvc.perform(get("/api/attendance/me").with(user(username).roles("EMPLOYEE")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void doubleCheckInFails() throws Exception {
        String username = registerLinkedEmployee("att2", "EMP-ATT-2");

        mockMvc.perform(post("/api/attendance/check-in").with(user(username).roles("EMPLOYEE")))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/attendance/check-in").with(user(username).roles("EMPLOYEE")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Already checked in today"));
    }

    @Test
    void checkOutWithoutCheckInFails() throws Exception {
        String username = registerLinkedEmployee("att3", "EMP-ATT-3");

        mockMvc.perform(post("/api/attendance/check-out").with(user(username).roles("EMPLOYEE")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("You haven't checked in today"));
    }

    @Test
    @WithMockUser(username = "unlinked", roles = "EMPLOYEE")
    void unlinkedAccountCannotCheckIn() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("username", "unlinked", "password", "pass1234"))))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/attendance/check-in").with(user("unlinked").roles("EMPLOYEE")))
            .andExpect(status().isNotFound());
    }

    @Test
    void employeeCannotViewOthersAttendance() throws Exception {
        String username = registerLinkedEmployee("att4", "EMP-ATT-4");

        mockMvc.perform(get("/api/attendance/employee/1").with(user(username).roles("EMPLOYEE")))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminCanViewAllAttendance() throws Exception {
        String username = registerLinkedEmployee("att5", "EMP-ATT-5");
        mockMvc.perform(post("/api/attendance/check-in").with(user(username).roles("EMPLOYEE")))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/attendance").with(adminAuth()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements").value(1));
    }
}
