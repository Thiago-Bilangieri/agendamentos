package com.bilangieri.agendamento.appointment.service;

import com.bilangieri.agendamento.appointment.dto.AppointmentCreateRequest;
import com.bilangieri.agendamento.appointment.dto.AppointmentResponse;
import com.bilangieri.agendamento.appointment.dto.AppointmentUpdateRequest;
import com.bilangieri.agendamento.appointment.entity.Appointment;
import com.bilangieri.agendamento.appointment.entity.AppointmentStatus;
import com.bilangieri.agendamento.appointment.repository.AppointmentRepository;
import com.bilangieri.agendamento.customer.entity.Customer;
import com.bilangieri.agendamento.customer.repository.CustomerRepository;
import com.bilangieri.agendamento.exception.BusinessException;
import com.bilangieri.agendamento.exception.NotFoundException;
import com.bilangieri.agendamento.service.entity.Service;
import com.bilangieri.agendamento.service.repository.ServiceRepository;
import com.bilangieri.agendamento.user.entity.User;
import com.bilangieri.agendamento.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final CustomerRepository customerRepository;
    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;

    @Transactional
    public AppointmentResponse create(AppointmentCreateRequest request) {
        // 1. Buscar e validar se o cliente existe
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado com o ID: " + request.customerId()));

        // 2. Buscar e validar se o serviço existe
        Service service = serviceRepository.findById(request.serviceId())
                .orElseThrow(() -> new NotFoundException("Serviço não encontrado com o ID: " + request.serviceId()));

        // 3. Buscar e validar se o profissional existe
        User professional = userRepository.findById(request.professionalId())
                .orElseThrow(() -> new NotFoundException("Profissional não encontrado com o ID: " + request.professionalId()));

        // 4. Calcular o endAt com base na duração do serviço em minutos
        LocalDateTime startAt = request.startAt();
        LocalDateTime endAt = startAt.plusMinutes(service.getDurationMinutes());

        // 5. Validar regra de negócio de conflito de horários para o profissional
        List<Appointment> conflicts = appointmentRepository.findConflictingAppointments(
                professional.getId(), startAt, endAt
        );

        if (!conflicts.isEmpty()) {
            throw new BusinessException("O profissional já possui um agendamento conflituoso neste intervalo de horários.");
        }

        // 6. Criar e gravar o agendamento
        Appointment appointment = Appointment.builder()
                .customer(customer)
                .service(service)
                .professional(professional)
                .startAt(startAt)
                .endAt(endAt)
                .status(AppointmentStatus.SCHEDULED)
                .notes(request.notes())
                .build();

        Appointment savedAppointment = appointmentRepository.save(appointment);
        return AppointmentResponse.fromEntity(savedAppointment);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> findAll() {
        return appointmentRepository.findAll().stream()
                .map(AppointmentResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public AppointmentResponse findById(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Agendamento não encontrado com o ID: " + id));
        return AppointmentResponse.fromEntity(appointment);
    }

    @Transactional
    public AppointmentResponse update(Long id, AppointmentUpdateRequest request) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Agendamento não encontrado com o ID: " + id));

        LocalDateTime startAt = request.startAt();
        LocalDateTime endAt = startAt.plusMinutes(appointment.getService().getDurationMinutes());

        // Validar conflitos ignorando o próprio agendamento atual
        List<Appointment> conflicts = appointmentRepository.findConflictingAppointments(
                appointment.getProfessional().getId(), startAt, endAt
        );

        boolean hasConflict = conflicts.stream().anyMatch(a -> !a.getId().equals(id));
        if (hasConflict) {
            throw new BusinessException("O profissional já possui um agendamento conflituoso neste novo horário.");
        }

        appointment.setStartAt(startAt);
        appointment.setEndAt(endAt);
        appointment.setStatus(request.status());
        appointment.setNotes(request.notes());

        Appointment updated = appointmentRepository.save(appointment);
        return AppointmentResponse.fromEntity(updated);
    }

    @Transactional
    public AppointmentResponse updateStatus(Long id, AppointmentStatus status) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Agendamento não encontrado com o ID: " + id));

        appointment.setStatus(status);
        Appointment updated = appointmentRepository.save(appointment);
        return AppointmentResponse.fromEntity(updated);
    }
}