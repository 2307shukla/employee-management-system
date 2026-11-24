package com.abhijeet.employee_management_system;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "jwt.secret=test-only-secret-not-used-anywhere-else-0123456789")
@AutoConfigureMockMvc
class ProductionHardeningTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void h2ConsoleIsNotExposedByDefault() throws Exception {
        mockMvc.perform(get("/h2-console/")).andExpect(status().isUnauthorized());
    }

    @Test
    void framingIsDenied() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    void corsAllowsConfiguredFrontendOrigin() throws Exception {
        mockMvc.perform(options("/api/employees")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    @Test
    void corsRejectsUnknownOrigin() throws Exception {
        mockMvc.perform(options("/api/employees")
                .header("Origin", "https://evil.example")
                .header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isForbidden());
    }
}
