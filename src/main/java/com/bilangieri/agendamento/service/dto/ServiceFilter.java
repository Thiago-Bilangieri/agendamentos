package com.bilangieri.agendamento.service.dto;

// Filtros opcionais e combináveis de GET /api/services
public record ServiceFilter(
        Long professionalId,
        Long categoryId,
        String name
) {
}
