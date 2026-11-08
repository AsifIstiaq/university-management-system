package com.example.university.demo;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DEMO ONLY - this integration test FAILS ON PURPOSE to show a red build in GitHub Actions.
 * Excluded from the normal build by the "demo-failure" tag; run it with:  mvn test -Pdemo-failure
 */
@Tag("demo-failure")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DemoFailingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void demo_anonymousApiCallExpectedToSucceed_failsOnPurpose() throws Exception {
        // Wrong expectation on purpose: without credentials the API correctly answers 401, not 200.
        mockMvc.perform(get("/api/teachers"))
                .andExpect(status().isOk());
    }
}
