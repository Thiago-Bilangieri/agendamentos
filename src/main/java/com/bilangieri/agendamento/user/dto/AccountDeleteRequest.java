package com.bilangieri.agendamento.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record AccountDeleteRequest(
        @NotBlank(message = "A password atual é obrigatória")
        @Schema(example = "password")
        String currentPassword
) {
}
