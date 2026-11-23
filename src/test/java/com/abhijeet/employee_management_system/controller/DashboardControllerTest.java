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
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static org.springframework.test.web.servlet.request.RequestPostProcessor adminAuth() {
        return user("admin").roles("ADMIN");
    }

    @Test
    void dashboardRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employeeCannotViewDashboard() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
            .andExpect(status().isForbidden());
    }

    @Test
    void dashboardReflectsRealData() throws Exception {
        mockMvc.perform(post("/api/departments").with(adminAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", "Dash Dept"))))
            .andExpect(status().isCreated());

        Map<String, Object> employee = new HashMap<>();
        employee.put("employeeCode", "EMP-DASH-1");
        employee.put("firstName", "Dash");
        employee.put("lastName", "Board");
        employee.put("email", "dash@example.com");
        mockMvc.perform(post("/api/employees").with(adminAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(employee)))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/dashboard").with(adminAuth()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalEmployees").value(1))
            .andExpect(jsonPath("$.activeEmployees").value(1))
            .andExpect(jsonPath("$.totalDepartments").value(1))
            .andExpect(jsonPath("$.employeesOnLeave").value(0));
    }

    @Test
    void hrRoleCanViewDashboard() throws Exception {
        mockMvc.perform(get("/api/dashboard").with(user("hr").roles("HR")))
            .andExpect(status().isOk());
    }
}
