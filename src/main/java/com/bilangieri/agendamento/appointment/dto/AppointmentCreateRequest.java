package com.bilangieri.agendamento.appointment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AppointmentCreateRequest(
        @NotNull(message = "O ID do cliente é obrigatório")
        @Schema(description = "O CUSTOMER só pode indicar o seu próprio id", example = "2")
        Long customerId,

        @NotNull(message = "O ID do serviço é obrigatório")
        @Schema(example = "2")
        Long serviceId,

        @NotNull(message = "A data e hora de início são obrigatórias")
        @Future(message = "A data de início deve ser no futuro")
        @Schema(description = "Use um horário devolvido por GET /api/services/{id}/availability", example = "2030-01-16T10:00:00")
        LocalDateTime startAt,

        @Schema(example = "Primeira visita")

        String notes
) {
}