package com.bilangieri.agendamento.user.dto;

import com.bilangieri.agendamento.customer.entity.Customer;
import com.bilangieri.agendamento.user.entity.Role;
import com.bilangieri.agendamento.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record UserProfileResponse(
        Long id,
        String name,
        String email,
        Role role,
        @Schema(description = "Telefone do cliente; null para PROFESSIONAL e ADMIN", nullable = true)
        String phone,
        LocalDateTime createdAt
) {
    // customer é null para quem não é cliente
    public static UserProfileResponse of(User user, Customer customer) {
        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                customer == null ? null : customer.getPhone(),
                user.getCreatedAt()
        );
    }
}
