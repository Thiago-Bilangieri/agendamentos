package com.bilangieri.agendamento.professional.dto;

import com.bilangieri.agendamento.professional.entity.WorkingHours;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record WorkingHoursResponse(
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {
    public static WorkingHoursResponse fromEntity(WorkingHours hours) {
        return new WorkingHoursResponse(hours.getDayOfWeek(), hours.getStartTime(), hours.getEndTime());
    }
}
