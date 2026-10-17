package com.bilangieri.agendamento.appointment.dto;

import com.bilangieri.agendamento.appointment.entity.Appointment;
import com.bilangieri.agendamento.appointment.entity.AppointmentStatus;
import com.bilangieri.agendamento.appointment.entity.AppointmentStatusHistory;
import com.bilangieri.agendamento.user.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record AppointmentResponse(
        Long id,
        Long customerId,
        String customerName,
        Long serviceId,
        String serviceName,
        Long professionalId,
        String professionalName,
        LocalDateTime startAt,
        LocalDateTime endAt,
        AppointmentStatus status,
        String notes,
        LocalDateTime createdAt,

        @Schema(description = "Papel de quem cancelou; null se o agendamento não estiver CANCELLED "
                + "ou tiver sido cancelado antes da existência do histórico")
        Role cancelledByRole,

        @Schema(description = "Motivo do cancelamento; null se não estiver CANCELLED ou se o cliente não o indicou")
        String cancellationReason
) {
    // A entrada de cancelamento do histórico só é usada se o agendamento estiver de facto CANCELLED
    public static AppointmentResponse fromEntity(Appointment appointment, AppointmentStatusHistory cancellation) {
        boolean cancelled = appointment.getStatus() == AppointmentStatus.CANCELLED && cancellation != null;
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getCustomer().getId(),
                appointment.getCustomer().getName(),
                appointment.getService().getId(),
                appointment.getService().getName(),
                appointment.getProfessional().getId(),
                appointment.getProfessional().getName(),
                appointment.getStartAt(),
                appointment.getEndAt(),
                appointment.getStatus(),
                appointment.getNotes(),
                appointment.getCreatedAt(),
                cancelled ? cancellation.getChangedByRole() : null,
                cancelled ? cancellation.getReason() : null
        );
    }
}
