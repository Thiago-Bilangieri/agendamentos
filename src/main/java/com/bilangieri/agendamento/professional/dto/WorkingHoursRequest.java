package com.bilangieri.agendamento.professional.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record WorkingHoursRequest(
        @NotNull(message = "O dia da semana é obrigatório")
        @Schema(example = "MONDAY")
        DayOfWeek dayOfWeek,

        @NotNull(message = "A hora de início é obrigatória")
        @Schema(type = "string", example = "09:00")
        LocalTime startTime,

        @NotNull(message = "A hora de fim é obrigatória")
        @Schema(type = "string", example = "13:00")
        LocalTime endTime
) {
}
