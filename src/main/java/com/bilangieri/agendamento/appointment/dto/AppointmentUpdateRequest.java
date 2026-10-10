package com.bilangieri.agendamento.appointment.dto;

import com.bilangieri.agendamento.appointment.entity.AppointmentStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AppointmentUpdateRequest(
        // Sem @Future: só um novo horário tem de ser no futuro (validado no AppointmentService),
        // para que as notas ou o estado de um agendamento que já começou possam ser alterados
        @NotNull(message = "A data e hora de início são obrigatórias")
        LocalDateTime startAt,

        @NotNull(message = "O status do agendamento é obrigatório")
        AppointmentStatus status,

        String notes
) {
}
