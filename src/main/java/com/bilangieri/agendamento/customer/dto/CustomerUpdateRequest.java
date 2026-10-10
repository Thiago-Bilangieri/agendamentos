package com.bilangieri.agendamento.customer.dto;

import com.bilangieri.agendamento.user.Emails;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CustomerUpdateRequest(
        @NotBlank(message = "O nome é obrigatório")
        String name,

        @Email(message = "Email inválido")
        @NotBlank(message = "O email é obrigatório")
        String email,

        @NotBlank(message = "O telefone é obrigatório")
        String phone,

        String notes
) {

    public CustomerUpdateRequest {
        email = Emails.normalize(email);
    }
}
