package com.bilangieri.agendamento.service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ServiceUpdateRequest(
        @NotBlank(message = "O nome do serviço é obrigatório")
        String name,

        String description,

        @NotNull(message = "O preço é obrigatório")
        @DecimalMin(value = "0.01", message = "O preço deve ser maior que zero")
        BigDecimal price,

        @NotNull(message = "A duração em minutos é obrigatória")
        @Min(value = 5, message = "A duração mínima deve ser de 5 minutos")
        Integer durationMinutes,

        @NotNull(message = "O estado ativo é obrigatório")
        Boolean active,

        // Opcional; null remove a categoria
        Long categoryId
) {
}