package com.bilangieri.agendamento.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 100, message = "O nome não pode ter mais de 100 caracteres")
        @Schema(example = "João Silva")
        String name,

        // Obrigatório só para o CUSTOMER (validado no UserProfileService)
        @Size(max = 50, message = "O telefone não pode ter mais de 50 caracteres")
        @Schema(description = "Obrigatório para CUSTOMER; ignorado nos outros perfis", example = "912345678")
        String phone
) {

    public ProfileUpdateRequest {
        name = name == null ? null : name.trim();
        phone = phone == null ? null : phone.trim();
    }
}
