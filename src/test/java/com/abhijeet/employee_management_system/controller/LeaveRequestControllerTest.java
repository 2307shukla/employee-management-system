package com.abhijeet.employee_management_system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
class LeaveRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static org.springframework.test.web.servlet.request.RequestPostProcessor adminAuth() {
        return user("admin").roles("ADMIN");
    }

    private String registerLinkedEmployee(String username, String employeeCode) throws Exception {
        Map<String, Object> employee = new HashMap<>();
        employee.put("employeeCode", employeeCode);
        employee.put("firstName", username);
        employee.put("lastName", "Test");
        employee.put("email", username + "@example.com");
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

    private String leavePayload(String type, String start, String end, String reason) {
        Map<String, Object> body = new HashMap<>();
        body.put("leaveType", type);
        body.put("startDate", start);
        body.put("endDate", end);
        body.put("reason", reason);
        return toJson(body);
    }

    private String toJson(Map<String, Object> body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void submittingLeaveRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/leave-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(leavePayload("SICK", "2027-01-01", "2027-01-02", "flu")))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void employeeCanSubmitAndViewOwnLeaveRequest() throws Exception {
        String username = registerLinkedEmployee("leave1", "EMP-LV-1");

        mockMvc.perform(post("/api/leave-requests").with(user(username).roles("EMPLOYEE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(leavePayload("SICK", "2027-01-01", "2027-01-02", "flu")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(get("/api/leave-requests/me").with(user(username).roles("EMPLOYEE")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void employeeCannotViewAllOrApproveLeaveRequests() throws Exception {
        String username = registerLinkedEmployee("leave2", "EMP-LV-2");

        String response = mockMvc.perform(post("/api/leave-requests").with(user(username).roles("EMPLOYEE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(leavePayload("CASUAL", "2027-02-01", "2027-02-02", "trip")))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/leave-requests").with(user(username).roles("EMPLOYEE")))
            .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/leave-requests/" + id + "/approve").with(user(username).roles("EMPLOYEE")))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminCanApproveAndRejectLeaveRequests() throws Exception {
        String username = registerLinkedEmployee("leave3", "EMP-LV-3");

        String response = mockMvc.perform(post("/api/leave-requests").with(user(username).roles("EMPLOYEE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(leavePayload("SICK", "2027-03-01", "2027-03-02", "flu")))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(put("/api/leave-requests/" + id + "/approve").with(adminAuth()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(put("/api/leave-requests/" + id + "/approve").with(adminAuth()))
            .andExpect(status().isBadRequest());

        String response2 = mockMvc.perform(post("/api/leave-requests").with(user(username).roles("EMPLOYEE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(leavePayload("SICK", "2027-04-01", "2027-04-02", "flu")))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        Long id2 = objectMapper.readTree(response2).get("id").asLong();

        mockMvc.perform(put("/api/leave-requests/" + id2 + "/reject").with(adminAuth()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void endDateBeforeStartDateFails() throws Exception {
        String username = registerLinkedEmployee("leave4", "EMP-LV-4");

        mockMvc.perform(post("/api/leave-requests").with(user(username).roles("EMPLOYEE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(leavePayload("SICK", "2027-05-10", "2027-05-01", "flu")))
            .andExpect(status().isBadRequest());
    }
}
