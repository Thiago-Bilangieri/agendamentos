package com.bilangieri.agendamento.auth;

import com.bilangieri.agendamento.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends IntegrationTest {

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, password)));
    }

    @Test
    void loginReturnsTokenAndRole() throws Exception {
        login(JOAO, "password")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void loginWithWrongPasswordIsUnauthorized() throws Exception {
        login(JOAO, "wrong-password").andExpect(status().isUnauthorized());
    }

    @Test
    void pendingProfessionalCannotLogIn() throws Exception {
        login(DIOGO_PENDING, "password")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail", containsString("aguarda aprovação")));
    }

    @Test
    void registeredCustomerCanLogIn() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Novo Cliente", "email": "novo@email.com", "password": "secret123", "phone": "912000000"}
                                """))
                .andExpect(status().isCreated());

        login("novo@email.com", "secret123").andExpect(status().isOk());
    }

    @Test
    void registeredProfessionalWaitsForApproval() throws Exception {
        mockMvc.perform(post("/api/auth/register/professional")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Novo Prestador", "email": "prestador@email.com", "password": "secret123"}
                                """))
                .andExpect(status().isAccepted());

        login("prestador@email.com", "secret123").andExpect(status().isForbidden());
    }

    @Test
    void registerValidatesRequiredFields() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "", "email": "not-an-email", "password": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.phone").exists());
    }

    @Test
    void protectedEndpointsRequireToken() throws Exception {
        mockMvc.perform(get("/api/services")).andExpect(status().isUnauthorized());
    }

    @Test
    void invalidTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/services").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }
}
