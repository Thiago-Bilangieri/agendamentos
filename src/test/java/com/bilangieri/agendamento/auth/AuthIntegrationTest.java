package com.bilangieri.agendamento.auth;

import com.bilangieri.agendamento.IntegrationTest;
import com.bilangieri.agendamento.auth.service.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends IntegrationTest {

    private static final String PASSWORD_POLICY =
            "A password deve ter entre 8 e 72 caracteres e incluir pelo menos uma letra e um número.";

    @Value("${api.security.token.secret}")
    private String tokenSecret;

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

    // Email só deste teste: o bloqueio vive em memória e não é revertido com a transação
    @Test
    void repeatedWrongPasswordsLockTheEmailEvenForTheRightPassword() throws Exception {
        String email = "brute.force@email.com";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Alvo", "email": "%s", "password": "secret123", "phone": "912000000"}
                                """.formatted(email)))
                .andExpect(status().isCreated());

        for (int i = 0; i < 5; i++) {
            login(email, "wrong-password").andExpect(status().isUnauthorized());
        }

        login(email, "secret123")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.title").value("Demasiados pedidos"));
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
    void loginIgnoresEmailCaseAndSurroundingSpaces() throws Exception {
        login("  " + JOAO.toUpperCase() + " ", "password").andExpect(status().isOk());
    }

    @Test
    void registerRejectsAnExistingEmailWithDifferentCase() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Outro João", "email": "%s", "password": "secret123", "phone": "912000000"}
                                """.formatted(JOAO.toUpperCase())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("Já existe um utilizador")));
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

    @ParameterizedTest
    @ValueSource(strings = {"password", "12345678", "abc123"})
    void registerRejectsWeakPasswords(String weakPassword) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Cliente", "email": "fraca@email.com", "password": "%s", "phone": "912000000"}
                                """.formatted(weakPassword)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").value(PASSWORD_POLICY));

        mockMvc.perform(post("/api/auth/register/professional")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Prestador", "email": "fraca@email.com", "password": "%s"}
                                """.formatted(weakPassword)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").value(PASSWORD_POLICY));
    }

    // A política só vale para passwords novas: as contas de teste com "password" continuam a entrar
    @Test
    void existingAccountsWithWeakPasswordsCanStillLogIn() throws Exception {
        login(MARIA, "password").andExpect(status().isOk());
    }

    @Test
    void protectedEndpointsRequireToken() throws Exception {
        mockMvc.perform(get("/api/services")).andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        String expired = new JwtService(tokenSecret, -1_000).generateToken(JOAO);

        mockMvc.perform(get("/api/services").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() throws Exception {
        String forged = new JwtService("outra-chave-com-pelo-menos-32-caracteres!", 60_000).generateToken(ADMIN);

        mockMvc.perform(get("/api/services").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenOfAnAccountThatNoLongerExistsIsRejected() throws Exception {
        mockMvc.perform(get("/api/services").header("Authorization", bearer("apagado@email.com")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/services").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }
}
