package com.bilangieri.agendamento.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// Registo de prestador de serviços (PROFESSIONAL), sujeito a aprovação do ADMIN
public record ProfessionalRegisterRequest(
        @NotBlank(message = "O nome é obrigatório")
        String name,

        @Email(message = "Email inválido")
        @NotBlank(message = "O email é obrigatório")
        String email,

        @NotBlank(message = "A password é obrigatória")
        String password

) {
}
