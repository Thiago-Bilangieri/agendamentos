package com.bilangieri.agendamento.appointment.dto;

import com.bilangieri.agendamento.appointment.entity.AppointmentStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AppointmentUpdateRequest(
        @NotNull(message = "A data e hora de início são obrigatórias")
        @Future(message = "A data de início deve ser no futuro")
        LocalDateTime startAt,

        @NotNull(message = "O status do agendamento é obrigatório")
        AppointmentStatus status,

        String notes
) {
}