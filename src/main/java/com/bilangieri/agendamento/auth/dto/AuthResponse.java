package com.bilangieri.agendamento.auth.dto;

public record AuthResponse(
        String token,
        String email,
        String role
) {
}