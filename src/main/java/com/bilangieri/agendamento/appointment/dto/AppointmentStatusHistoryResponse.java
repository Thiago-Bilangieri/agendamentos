package com.bilangieri.agendamento.appointment.dto;

import com.bilangieri.agendamento.appointment.entity.AppointmentStatus;
import com.bilangieri.agendamento.appointment.entity.AppointmentStatusHistory;
import com.bilangieri.agendamento.user.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record AppointmentStatusHistoryResponse(
        @Schema(description = "Estado anterior; null na criação do agendamento")
        AppointmentStatus fromStatus,
        AppointmentStatus toStatus,
        String changedByName,
        Role changedByRole,
        String reason,
        LocalDateTime changedAt
) {
    public static AppointmentStatusHistoryResponse fromEntity(AppointmentStatusHistory history) {
        return new AppointmentStatusHistoryResponse(
                history.getFromStatus(),
                history.getToStatus(),
                history.getChangedBy().getName(),
                history.getChangedByRole(),
                history.getReason(),
                history.getChangedAt()
        );
    }
}
