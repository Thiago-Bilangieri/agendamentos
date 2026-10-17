package com.bilangieri.agendamento.user.dto;

import com.bilangieri.agendamento.user.Emails;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailChangeRequest(
        @Email(message = "Email inválido")
        @NotBlank(message = "O email é obrigatório")
        @Schema(example = "joao.novo@email.com")
        String newEmail,

        @NotBlank(message = "A password atual é obrigatória")
        @Schema(example = "password")
        String currentPassword
) {

    public EmailChangeRequest {
        newEmail = Emails.normalize(newEmail);
    }
}
