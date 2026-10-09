package com.bilangieri.agendamento.professional.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

// Substitui o horário semanal completo do prestador; uma lista vazia remove todos os blocos
public record WeeklyScheduleRequest(
        @NotNull(message = "A lista de horários é obrigatória")
        List<@Valid @NotNull WorkingHoursRequest> hours
) {
}
