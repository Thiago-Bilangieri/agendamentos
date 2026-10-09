package com.bilangieri.agendamento.appointment.service;

import com.bilangieri.agendamento.appointment.dto.TimeSlot;
import com.bilangieri.agendamento.appointment.entity.Appointment;
import com.bilangieri.agendamento.appointment.repository.AppointmentRepository;
import com.bilangieri.agendamento.professional.entity.WorkingHours;
import com.bilangieri.agendamento.professional.service.WorkingHoursService;
import com.bilangieri.agendamento.service.entity.Service;
import com.bilangieri.agendamento.service.service.ServiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class AvailabilityService {

    // Intervalo entre inícios possíveis (09:00, 09:30, 10:00...)
    static final int SLOT_STEP_MINUTES = 30;

    private final ServiceService serviceService;
    private final WorkingHoursService workingHoursService;
    private final AppointmentRepository appointmentRepository;

    // Horários em que o serviço pode ser marcado nesse dia: dentro do horário de trabalho do prestador,
    // sem sobrepor agendamentos ativos e ainda no futuro. Usa as mesmas regras do AppointmentService.
    @Transactional(readOnly = true)
    public List<TimeSlot> findAvailableSlots(Long serviceId, LocalDate date) {
        Service service = serviceService.getVisibleService(serviceId);
        if (!service.isBookable()) {
            return List.of();
        }

        Long professionalId = service.getProfessional().getId();
        int duration = service.getDurationMinutes();
        List<Appointment> booked = appointmentRepository.findConflictingAppointments(
                professionalId, date.atStartOfDay(), date.plusDays(1).atStartOfDay());
        LocalDateTime now = LocalDateTime.now();

        List<TimeSlot> slots = new ArrayList<>();
        for (WorkingHours block : workingHoursService.blocksOn(professionalId, date.getDayOfWeek())) {
            LocalDateTime blockEnd = date.atTime(block.getEndTime());
            for (LocalDateTime start = date.atTime(block.getStartTime());
                 !start.plusMinutes(duration).isAfter(blockEnd);
                 start = start.plusMinutes(SLOT_STEP_MINUTES)) {

                LocalDateTime end = start.plusMinutes(duration);
                if (start.isAfter(now) && isFree(booked, start, end)) {
                    slots.add(new TimeSlot(start, end));
                }
            }
        }
        return slots;
    }

    private boolean isFree(List<Appointment> booked, LocalDateTime start, LocalDateTime end) {
        return booked.stream().noneMatch(a -> a.getStartAt().isBefore(end) && a.getEndAt().isAfter(start));
    }
}
