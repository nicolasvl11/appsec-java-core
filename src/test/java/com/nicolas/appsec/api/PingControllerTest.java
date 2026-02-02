package com.nicolas.appsec.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PingController.class)
@AutoConfigureMockMvc(addFilters = false)
class PingControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void ping_returns_ok() throws Exception {
        mvc.perform(get("/api/v1/ping"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status").value("ok"))
           .andExpect(jsonPath("$.service").value("appsec-java-core"))
           .andExpect(jsonPath("$.time").exists());
    }
}
