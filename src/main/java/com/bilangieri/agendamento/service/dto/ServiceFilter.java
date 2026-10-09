package com.bilangieri.agendamento.service.dto;

import io.swagger.v3.oas.annotations.Parameter;

// Filtros opcionais e combináveis de GET /api/services
public record ServiceFilter(
        @Parameter(description = "Só os serviços deste prestador (ignorado para PROFESSIONAL)")
        Long professionalId,

        @Parameter(description = "Só os serviços desta categoria")
        Long categoryId,

        @Parameter(description = "Parte do nome, sem distinguir maiúsculas", example = "corte")
        String name
) {
}
