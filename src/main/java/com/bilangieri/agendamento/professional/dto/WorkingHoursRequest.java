package com.bilangieri.agendamento.professional.dto;

import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record WorkingHoursRequest(
        @NotNull(message = "O dia da semana é obrigatório")
        DayOfWeek dayOfWeek,

        @NotNull(message = "A hora de início é obrigatória")
        LocalTime startTime,

        @NotNull(message = "A hora de fim é obrigatória")
        LocalTime endTime
) {
}
