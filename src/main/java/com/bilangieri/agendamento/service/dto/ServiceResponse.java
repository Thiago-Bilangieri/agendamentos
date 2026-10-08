package com.bilangieri.agendamento.service.dto;

import com.bilangieri.agendamento.service.entity.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ServiceResponse(
        Long id,
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
                service.getName(),
                service.getDescription(),
                service.getPrice(),
                service.getDurationMinutes(),
                service.getActive(),
                service.getCreatedAt()
        );
    }
}