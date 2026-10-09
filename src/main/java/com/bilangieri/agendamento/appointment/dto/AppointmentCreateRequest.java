package com.bilangieri.agendamento.appointment.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AppointmentCreateRequest(
        @NotNull(message = "O ID do cliente é obrigatório")
        Long customerId,

        @NotNull(message = "O ID do serviço é obrigatório")
        Long serviceId,

        @NotNull(message = "A data e hora de início são obrigatórias")
        @Future(message = "A data de início deve ser no futuro")
        LocalDateTime startAt,

        String notes
) {
}