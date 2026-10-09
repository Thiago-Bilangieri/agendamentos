package com.bilangieri.agendamento.professional.dto;

import com.bilangieri.agendamento.user.entity.ApprovalStatus;
import com.bilangieri.agendamento.user.entity.User;

import java.time.LocalDateTime;

public record ProfessionalResponse(
        Long id,
        String name,
        String email,
        ApprovalStatus approvalStatus,
        Boolean active,
        LocalDateTime createdAt
) {
    public static ProfessionalResponse fromEntity(User user) {
        return new ProfessionalResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getApprovalStatus(),
                user.getActive(),
                user.getCreatedAt()
        );
    }
}
