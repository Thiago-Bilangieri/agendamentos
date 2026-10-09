package com.bilangieri.agendamento.service.dto;

import com.bilangieri.agendamento.service.entity.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ServiceResponse(
        Long id,
        Long professionalId,
        String professionalName,
        Long categoryId,
        String categoryName,
        String name,
        String description,
        BigDecimal price,
        Integer durationMinutes,
        Boolean active,
        LocalDateTime createdAt
) {
    public static ServiceResponse fromEntity(Service service) {
        return new ServiceResponse(
                service.getId(),
                service.getProfessional().getId(),
                service.getProfessional().getName(),
                service.getCategory() != null ? service.getCategory().getId() : null,
                service.getCategory() != null ? service.getCategory().getName() : null,
                service.getName(),
                service.getDescription(),
                service.getPrice(),
                service.getDurationMinutes(),
                service.getActive(),
                service.getCreatedAt()
        );
    }
}