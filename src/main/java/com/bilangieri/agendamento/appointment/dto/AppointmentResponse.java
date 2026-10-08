package com.bilangieri.agendamento.appointment.dto;

import com.bilangieri.agendamento.appointment.entity.Appointment;
import com.bilangieri.agendamento.appointment.entity.AppointmentStatus;

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
        LocalDateTime createdAt
) {
    public static AppointmentResponse fromEntity(Appointment appointment) {
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
                appointment.getCreatedAt()
        );
    }
}