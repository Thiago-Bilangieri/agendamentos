package com.bilangieri.agendamento.auth.dto;

import com.bilangieri.agendamento.user.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterRequest(
        @NotBlank(message = "O nome é obrigatório")
        String name,

        @Email(message = "Email inválido")
        @NotBlank(message = "O email é obrigatório")
        String email,

        @NotBlank(message = "A password é obrigatória")
        String password,

        @NotNull(message = "O papel (Role) é obrigatório")
        Role role
) {
}