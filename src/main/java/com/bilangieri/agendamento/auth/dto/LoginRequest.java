package com.bilangieri.agendamento.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Email(message = "Email inválido")
        @NotBlank(message = "O email é obrigatório")
        @Schema(example = "joao.silva@email.com")
        String email,

        @NotBlank(message = "A password é obrigatória")
        @Schema(example = "password")
        String password
) {
}