package com.bilangieri.agendamento.auth.dto;

import com.bilangieri.agendamento.user.Emails;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// Registo de cliente (CUSTOMER)
public record RegisterRequest(
        @NotBlank(message = "O nome é obrigatório")
        String name,

        @Email(message = "Email inválido")
        @NotBlank(message = "O email é obrigatório")
        String email,

        @NotBlank(message = "A password é obrigatória")
        String password,

        @NotBlank(message = "O telefone é obrigatório")
        String phone

) {

    public RegisterRequest {
        email = Emails.normalize(email);
    }
}
