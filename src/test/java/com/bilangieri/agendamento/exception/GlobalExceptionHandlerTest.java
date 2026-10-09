package com.bilangieri.agendamento.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void unexpectedErrorsBecomeAGeneric500WithoutLeakingInternals() {
        ProblemDetail problem = handler.handleUnexpected(
                new IllegalStateException("password=segredo na stack trace"));

        assertThat(problem.getStatus()).isEqualTo(500);
        assertThat(problem.getTitle()).isEqualTo("Erro interno");
        assertThat(problem.getDetail())
                .isEqualTo("Ocorreu um erro inesperado. Tente novamente mais tarde.")
                .doesNotContain("segredo");
        assertThat(problem.getProperties()).containsKey("timestamp");
    }
}
