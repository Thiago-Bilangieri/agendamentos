package com.bilangieri.agendamento.user.dto;

import com.bilangieri.agendamento.user.validation.StrongPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record PasswordChangeRequest(
        @NotBlank(message = "A password atual é obrigatória")
        @Schema(example = "password")
        String currentPassword,

        @NotBlank(message = "A nova password é obrigatória")
        @StrongPassword
        @Schema(description = "8 a 72 caracteres, com pelo menos uma letra e um número", example = "novaPassword1")
        String newPassword
) {
}
