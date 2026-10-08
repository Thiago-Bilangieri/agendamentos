package com.bilangieri.agendamento.customer.dto;

import com.bilangieri.agendamento.customer.entity.Customer;

import java.time.LocalDateTime;

public record CustomerResponse(
        Long id,
        String name,
        String email,
        String phone,
        String notes,
        LocalDateTime createdAt
) {
    // Um helper prático para converter a Entity num Response DTO
    public static CustomerResponse fromEntity(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getNotes(),
                customer.getCreatedAt()
        );
    }
}