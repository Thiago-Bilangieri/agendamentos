package com.bilangieri.agendamento.exception;

import com.bilangieri.agendamento.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Todas as origens de erro respondem no mesmo formato Problem Details (RFC 9457)
class ErrorResponseIntegrationTest extends IntegrationTest {

    @Test
    void businessErrorsAreProblemDetails() throws Exception {
        mockMvc.perform(get("/api/services/{id}", 999999).header("Authorization", bearer(ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                // "type" ausente equivale a "about:blank" (RFC 9457)
                .andExpect(jsonPath("$.type").doesNotExist())
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Serviço não encontrado com o ID: 999999"))
                .andExpect(jsonPath("$.instance").value("/api/services/999999"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void missingTokenIsAProblemDetail() throws Exception {
        mockMvc.perform(get("/api/services"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Não autenticado"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void wrongPasswordIsAProblemDetail() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "errada"}
                                """.formatted(JOAO)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Não autenticado"));
    }

    @Test
    void forbiddenByUrlRuleIsAProblemDetail() throws Exception {
        // Barrado pelo SecurityConfig, antes de chegar ao controller
        mockMvc.perform(get("/api/customers").header("Authorization", bearer(JOAO)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Acesso negado"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void validationErrorsListEachField() throws Exception {
        mockMvc.perform(post("/api/categories").header("Authorization", bearer(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.errors.name").value("O nome da categoria é obrigatório"));
    }

    @Test
    void malformedJsonIsABadRequest() throws Exception {
        mockMvc.perform(post("/api/categories").header("Authorization", bearer(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ isto não é json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void unsupportedMethodIsA405() throws Exception {
        mockMvc.perform(delete("/api/categories").header("Authorization", bearer(ADMIN)))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void invalidPathVariableTypeIsABadRequest() throws Exception {
        mockMvc.perform(get("/api/services/{id}", "abc").header("Authorization", bearer(ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void unknownRouteIsA404() throws Exception {
        mockMvc.perform(get("/api/nao-existe").header("Authorization", bearer(ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.detail").value("Não existe nenhum endpoint GET /api/nao-existe."));
    }
}
