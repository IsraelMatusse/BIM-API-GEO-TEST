package com.bim.api_test.interfaces.controllers;

import com.bim.api_test.infrastructure.logging.CorrelationIdFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HealthController.class, properties = "api.version=9.9.9")
@Import(CorrelationIdFilter.class)
@DisplayName("GET /health")
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("returns 200 with the status, the configured version and a correlationId")
    void returnsApplicationStatus() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.version").value("9.9.9"))
                .andExpect(header().exists(CorrelationIdFilter.CORRELATION_ID_HEADER));
    }
}
