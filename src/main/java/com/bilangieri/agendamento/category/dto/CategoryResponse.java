package com.bilangieri.agendamento.category.dto;

import com.bilangieri.agendamento.category.entity.ServiceCategory;

public record CategoryResponse(
        Long id,
        String name,
        String description
) {
    public static CategoryResponse fromEntity(ServiceCategory category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getDescription());
    }
}
