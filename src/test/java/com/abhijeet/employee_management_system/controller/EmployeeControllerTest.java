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

    private Map<String, Object> minimalEmployee(String code, String first, String last, String email) {
        Map<String, Object> body = new HashMap<>();
        body.put("employeeCode", code);
        body.put("firstName", first);
        body.put("lastName", last);
        body.put("email", email);
        return body;
    }

    private Long createDepartment(String name) throws Exception {
        String response = mockMvc.perform(post("/api/departments").with(adminAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", name))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private Long createDesignation(String title) throws Exception {
        String response = mockMvc.perform(post("/api/designations").with(adminAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("title", title))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor adminAuth() {
        return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN");
    }

    @Test
    void listEmployeesRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/employees"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createAndFetchEmployee() throws Exception {
        Long deptId = createDepartment("Engineering");
        Long desigId = createDesignation("Developer");

        Map<String, Object> body = minimalEmployee("EMP-001", "Jane", "Doe", "jane.doe@example.com");
        body.put("departmentId", deptId);
        body.put("designationId", desigId);
        body.put("salary", 75000.0);

        String response = mockMvc.perform(post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.email").value("jane.doe@example.com"))
            .andExpect(jsonPath("$.department.name").value("Engineering"))
            .andExpect(jsonPath("$.designation.title").value("Developer"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/employees/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("Jane"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void creatingEmployeeWithUnknownDepartmentReturns404() throws Exception {
        Map<String, Object> body = minimalEmployee("EMP-002", "No", "Dept", "no.dept@example.com");
        body.put("departmentId", 999999L);
        mockMvc.perform(post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employeeRoleCanListButNotCreate() throws Exception {
        mockMvc.perform(get("/api/employees"))
            .andExpect(status().isOk());

        String body = objectMapper.writeValueAsString(minimalEmployee("EMP-003", "Should", "Fail", "should.fail@example.com"));
        mockMvc.perform(post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employeeRoleCannotDeleteOrUpdate() throws Exception {
        mockMvc.perform(put("/api/employees/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(minimalEmployee("EMP-004", "X", "Y", "x.y@example.com"))))
            .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/employees/1"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void creatingEmployeeWithoutEmailFailsValidation() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("employeeCode", "EMP-005");
        body.put("firstName", "No");
        body.put("lastName", "Email");
        mockMvc.perform(post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.email").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void creatingEmployeeWithoutEmployeeCodeFailsValidation() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("firstName", "No");
        body.put("lastName", "Code");
        body.put("email", "no.code@example.com");
        mockMvc.perform(post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.employeeCode").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void creatingEmployeeWithInvalidEmailFailsValidation() throws Exception {
        Map<String, Object> body = minimalEmployee("EMP-006", "Bad", "Email", "not-an-email");
        mockMvc.perform(post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.email").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deletingAnEmployeeRemovesIt() throws Exception {
        String body = objectMapper.writeValueAsString(minimalEmployee("EMP-007", "To", "Delete", "to.delete@example.com"));
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
        String body = objectMapper.writeValueAsString(minimalEmployee("EMP-008", "Ghost", "Employee", "ghost@example.com"));
        mockMvc.perform(put("/api/employees/99999999")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listIsPaginatedAndSortable() throws Exception {
        for (int i = 0; i < 3; i++) {
            String body = objectMapper.writeValueAsString(minimalEmployee("EMP-PAGE-" + i, "Page" + i, "Test", "page" + i + "@example.com"));
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

    @Test
    @WithMockUser(roles = "ADMIN")
    void searchByNameMatchesFirstOrLastName() throws Exception {
        mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(minimalEmployee("EMP-S1", "Alice", "Wonderland", "alice@example.com"))))
            .andExpect(status().isCreated());
        mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(minimalEmployee("EMP-S2", "Bob", "Alison", "bob@example.com"))))
            .andExpect(status().isCreated());
        mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(minimalEmployee("EMP-S3", "Carol", "Smith", "carol@example.com"))))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/employees?name=ali"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void searchByDepartmentAndDesignationFiltersCorrectly() throws Exception {
        Long engId = createDepartment("SearchEng");
        Long salesId = createDepartment("SearchSales");
        Long devId = createDesignation("SearchDev");

        Map<String, Object> inEng = minimalEmployee("EMP-S4", "Dana", "Eng", "dana@example.com");
        inEng.put("departmentId", engId);
        inEng.put("designationId", devId);
        mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inEng)))
            .andExpect(status().isCreated());

        Map<String, Object> inSales = minimalEmployee("EMP-S5", "Evan", "Sales", "evan@example.com");
        inSales.put("departmentId", salesId);
        mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inSales)))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/employees?departmentId=" + engId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].firstName").value("Dana"));

        mockMvc.perform(get("/api/employees?designationId=" + devId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].firstName").value("Dana"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void searchBySalaryRangeFiltersCorrectly() throws Exception {
        Map<String, Object> low = minimalEmployee("EMP-S6", "Low", "Pay", "low@example.com");
        low.put("salary", 30000.0);
        mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(low)))
            .andExpect(status().isCreated());

        Map<String, Object> mid = minimalEmployee("EMP-S7", "Mid", "Pay", "mid@example.com");
        mid.put("salary", 60000.0);
        mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mid)))
            .andExpect(status().isCreated());

        Map<String, Object> high = minimalEmployee("EMP-S8", "High", "Pay", "high@example.com");
        high.put("salary", 100000.0);
        mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(high)))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/employees?minSalary=50000&maxSalary=90000"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].firstName").value("Mid"));
    }
}
