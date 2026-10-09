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
import com.bilangieri.agendamento.professional.service.WorkingHoursService;
import com.bilangieri.agendamento.security.CurrentUserService;
import com.bilangieri.agendamento.service.entity.Service;
import com.bilangieri.agendamento.service.repository.ServiceRepository;
import com.bilangieri.agendamento.user.entity.Role;
import com.bilangieri.agendamento.user.entity.User;
import com.bilangieri.agendamento.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class AppointmentService {

    private static final String OVERLAP_CONSTRAINT = "appointments_no_overlap";

    private final AppointmentRepository appointmentRepository;
    private final CustomerRepository customerRepository;
    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;
    private final WorkingHoursService workingHoursService;
    private final CurrentUserService currentUserService;

    @Transactional
    public AppointmentResponse create(AppointmentCreateRequest request) {
        // 1. Buscar e validar se o cliente existe
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado com o ID: " + request.customerId()));

        // Um CUSTOMER só pode criar agendamentos para si próprio
        currentCustomer().ifPresent(current -> {
            if (!current.getId().equals(customer.getId())) {
                throw new AccessDeniedException("Não pode criar agendamentos para outro cliente.");
            }
        });

        // 2. Buscar e validar se o serviço existe
        Service service = serviceRepository.findById(request.serviceId())
                .orElseThrow(() -> new NotFoundException("Serviço não encontrado com o ID: " + request.serviceId()));

        // 3. O profissional é o prestador dono do serviço, e tem de estar disponível
        User professional = service.getProfessional();
        if (!service.isBookable()) {
            throw new BusinessException("Este serviço não está disponível para agendamento.");
        }

        // 4. Calcular o endAt com base na duração do serviço em minutos
        LocalDateTime startAt = request.startAt();
        LocalDateTime endAt = startAt.plusMinutes(service.getDurationMinutes());
        checkWorkingHours(professional.getId(), startAt, endAt);

        // 5. Validar regra de negócio de conflito de horários para o profissional.
        // O bloqueio faz pedidos simultâneos para o mesmo prestador esperarem uns pelos outros
        userRepository.lockById(professional.getId());
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

        Appointment savedAppointment = saveCheckingOverlap(appointment,
                "O profissional já possui um agendamento conflituoso neste intervalo de horários.");
        return AppointmentResponse.fromEntity(savedAppointment);
    }

    // CUSTOMER vê os seus agendamentos, PROFESSIONAL os que recebeu, ADMIN todos
    @Transactional(readOnly = true)
    public Page<AppointmentResponse> findAll(Pageable pageable) {
        Page<Appointment> appointments;
        if (currentUserService.hasRole(Role.CUSTOMER)) {
            appointments = appointmentRepository.findByCustomerId(currentCustomer().orElseThrow().getId(), pageable);
        } else if (currentUserService.hasRole(Role.PROFESSIONAL)) {
            appointments = appointmentRepository.findByProfessionalId(currentUserService.getUser().getId(), pageable);
        } else {
            appointments = appointmentRepository.findAll(pageable);
        }

        return appointments.map(AppointmentResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public AppointmentResponse findById(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Agendamento não encontrado com o ID: " + id));
        checkOwnership(appointment);
        return AppointmentResponse.fromEntity(appointment);
    }

    @Transactional
    public AppointmentResponse update(Long id, AppointmentUpdateRequest request) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Agendamento não encontrado com o ID: " + id));
        checkOwnership(appointment);

        if (appointment.getStatus().isFinal()) {
            throw new BusinessException("Não é possível alterar um agendamento com o estado " + appointment.getStatus() + ".");
        }

        LocalDateTime startAt = request.startAt();
        LocalDateTime endAt = startAt.plusMinutes(appointment.getService().getDurationMinutes());

        if (request.status() != appointment.getStatus()) {
            validateTransition(appointment.getStatus(), request.status(), startAt);
        }
        checkWorkingHours(appointment.getProfessional().getId(), startAt, endAt);

        // Validar conflitos ignorando o próprio agendamento atual
        userRepository.lockById(appointment.getProfessional().getId());
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

        Appointment updated = saveCheckingOverlap(appointment,
                "O profissional já possui um agendamento conflituoso neste novo horário.");
        return AppointmentResponse.fromEntity(updated);
    }

    @Transactional
    public AppointmentResponse updateStatus(Long id, AppointmentStatus status) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Agendamento não encontrado com o ID: " + id));
        checkOwnership(appointment);
        validateTransition(appointment.getStatus(), status, appointment.getStartAt());

        appointment.setStatus(status);
        Appointment updated = appointmentRepository.save(appointment);
        return AppointmentResponse.fromEntity(updated);
    }

    // O bloqueio do prestador serializa as marcações feitas por este serviço; a restrição
    // appointments_no_overlap (V7) é a rede de segurança na base de dados para qualquer outra escrita.
    // O flush imediato permite traduzir a violação para a mesma resposta do conflito normal.
    private Appointment saveCheckingOverlap(Appointment appointment, String conflictMessage) {
        try {
            return appointmentRepository.saveAndFlush(appointment);
        } catch (DataIntegrityViolationException ex) {
            String cause = ex.getMostSpecificCause().getMessage();
            if (cause != null && cause.contains(OVERLAP_CONSTRAINT)) {
                throw new BusinessException(conflictMessage);
            }
            throw ex;
        }
    }

    private void checkWorkingHours(Long professionalId, LocalDateTime startAt, LocalDateTime endAt) {
        if (!workingHoursService.isWithinWorkingHours(professionalId, startAt, endAt)) {
            throw new BusinessException("O horário escolhido está fora do horário de trabalho do prestador.");
        }
    }

    private void validateTransition(AppointmentStatus current, AppointmentStatus target, LocalDateTime startAt) {
        if (!current.canTransitionTo(target)) {
            throw new BusinessException("Não é possível alterar o estado do agendamento de " + current + " para " + target + ".");
        }

        if (target.requiresStarted() && startAt.isAfter(LocalDateTime.now())) {
            throw new BusinessException("Não é possível marcar como " + target + " um agendamento que ainda não começou.");
        }
    }

    // Devolve o Customer ligado ao utilizador autenticado se este tiver a role CUSTOMER
    private Optional<Customer> currentCustomer() {
        if (!currentUserService.hasRole(Role.CUSTOMER)) {
            return Optional.empty();
        }

        return Optional.of(customerRepository.findByUserEmail(currentUserService.getEmail())
                .orElseThrow(() -> new AccessDeniedException("Utilizador sem cliente associado.")));
    }

    private void checkOwnership(Appointment appointment) {
        boolean allowed;
        if (currentUserService.hasRole(Role.CUSTOMER)) {
            allowed = appointment.getCustomer().getId().equals(currentCustomer().orElseThrow().getId());
        } else if (currentUserService.hasRole(Role.PROFESSIONAL)) {
            allowed = appointment.getProfessional().getEmail().equals(currentUserService.getEmail());
        } else {
            allowed = currentUserService.hasRole(Role.ADMIN);
        }

        if (!allowed) {
            throw new AccessDeniedException("Não tem acesso a este agendamento.");
        }
    }
}
