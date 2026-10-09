package com.bilangieri.agendamento.appointment.dto;

import java.time.LocalDateTime;

public record TimeSlot(
        LocalDateTime startAt,
        LocalDateTime endAt
) {
}
