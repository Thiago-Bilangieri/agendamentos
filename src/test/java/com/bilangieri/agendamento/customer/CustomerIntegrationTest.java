package com.bilangieri.agendamento.customer;

import com.bilangieri.agendamento.IntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CustomerIntegrationTest extends IntegrationTest {

    @Test
    void customerSeesOwnProfile() throws Exception {
        mockMvc.perform(get("/api/customers/me").header("Authorization", bearer(JOAO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(JOAO));
    }

    @Test
    void onlyAdminListsCustomers() throws Exception {
        mockMvc.perform(get("/api/customers").header("Authorization", bearer(JOAO)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/customers").header("Authorization", bearer(ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void deletingCustomerWithAppointmentsIsAConflict() throws Exception {
        mockMvc.perform(delete("/api/customers/{id}", customer(JOAO).getId()).header("Authorization", bearer(ADMIN)))
                .andExpect(status().isConflict());
    }
}
