package com.bilangieri.agendamento;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HealthIntegrationTest extends IntegrationTest {

    @Test
    void healthProbesArePublic() throws Exception {
        for (String path : new String[]{"/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness"}) {
            mockMvc.perform(get(path))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"));
        }
    }

    @Test
    void otherActuatorEndpointsAreNotExposed() throws Exception {
        mockMvc.perform(get("/actuator/env").header("Authorization", bearer(ADMIN)))
                .andExpect(status().isNotFound());
    }
}
