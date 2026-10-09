package com.bilangieri.agendamento.service.service;

import com.bilangieri.agendamento.appointment.repository.AppointmentRepository;
import com.bilangieri.agendamento.exception.BusinessException;
import com.bilangieri.agendamento.exception.ConflictException;
import com.bilangieri.agendamento.exception.NotFoundException;
import com.bilangieri.agendamento.security.CurrentUserService;
import com.bilangieri.agendamento.service.dto.ServiceCreateRequest;
import com.bilangieri.agendamento.service.dto.ServiceResponse;
import com.bilangieri.agendamento.service.dto.ServiceUpdateRequest;
import com.bilangieri.agendamento.service.entity.Service;
import com.bilangieri.agendamento.service.repository.ServiceRepository;
import com.bilangieri.agendamento.user.entity.ApprovalStatus;
import com.bilangieri.agendamento.user.entity.Role;
import com.bilangieri.agendamento.user.entity.User;
import com.bilangieri.agendamento.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

@org.springframework.stereotype.Service // Evita conflito com o nome da classe Service
@RequiredArgsConstructor
public class ServiceService {

    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public ServiceResponse create(ServiceCreateRequest request) {
        User professional = resolveProfessional(request.professionalId());

        if (serviceRepository.findByNameAndProfessionalId(request.name(), professional.getId()).isPresent()) {
            throw new BusinessException("Já tem um serviço registado com este nome.");
        }

        Service service = Service.builder()
                .professional(professional)
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .durationMinutes(request.durationMinutes())
                .active(true)
                .build();

        Service savedService = serviceRepository.save(service);
        return ServiceResponse.fromEntity(savedService);
    }

    // CUSTOMER vê o catálogo disponível, PROFESSIONAL vê os seus serviços, ADMIN vê todos.
    // professionalId (opcional) filtra por prestador; para o PROFESSIONAL é ignorado.
    @Transactional(readOnly = true)
    public Page<ServiceResponse> findAll(Long professionalId, Pageable pageable) {
        Page<Service> services;
        if (currentUserService.hasRole(Role.ADMIN)) {
            services = professionalId == null
                    ? serviceRepository.findAll(pageable)
                    : serviceRepository.findByProfessionalId(professionalId, pageable);
        } else if (currentUserService.hasRole(Role.PROFESSIONAL)) {
            services = serviceRepository.findByProfessionalId(currentUserService.getUser().getId(), pageable);
        } else {
            services = professionalId == null
                    ? serviceRepository.findAvailable(pageable)
                    : serviceRepository.findAvailableByProfessionalId(professionalId, pageable);
        }

        return services.map(ServiceResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public ServiceResponse findById(Long id) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Serviço não encontrado com o ID: " + id));

        // Serviços fora do catálogo respondem como inexistentes para quem não os pode ver
        if (!canView(service)) {
            throw new NotFoundException("Serviço não encontrado com o ID: " + id);
        }

        return ServiceResponse.fromEntity(service);
    }

    @Transactional
    public ServiceResponse update(Long id, ServiceUpdateRequest request) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Serviço não encontrado com o ID: " + id));
        checkOwnership(service);

        serviceRepository.findByNameAndProfessionalId(request.name(), service.getProfessional().getId())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new BusinessException("Já tem outro serviço com este nome.");
                    }
                });

        service.setName(request.name());
        service.setDescription(request.description());
        service.setPrice(request.price());
        service.setDurationMinutes(request.durationMinutes());
        service.setActive(request.active());

        Service updatedService = serviceRepository.save(service);
        return ServiceResponse.fromEntity(updatedService);
    }

    @Transactional
    public void delete(Long id) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Serviço não encontrado com o ID: " + id));
        checkOwnership(service);

        if (appointmentRepository.existsByServiceId(id)) {
            throw new ConflictException("Este serviço tem agendamentos associados e não pode ser removido. Desative-o (active = false).");
        }

        serviceRepository.delete(service);
    }

    // PROFESSIONAL cria sempre para si próprio; ADMIN tem de indicar o prestador
    private User resolveProfessional(Long professionalId) {
        if (currentUserService.hasRole(Role.PROFESSIONAL)) {
            return currentUserService.getUser();
        }

        if (professionalId == null) {
            throw new BusinessException("O ID do prestador é obrigatório.");
        }

        return userRepository.findById(professionalId)
                .filter(user -> user.getRole() == Role.PROFESSIONAL)
                .orElseThrow(() -> new NotFoundException("Prestador não encontrado com o ID: " + professionalId));
    }

    // ADMIN vê tudo, PROFESSIONAL vê os seus; os restantes só o catálogo disponível
    private boolean canView(Service service) {
        if (currentUserService.hasRole(Role.ADMIN)) {
            return true;
        }

        if (currentUserService.hasRole(Role.PROFESSIONAL)
                && service.getProfessional().getEmail().equals(currentUserService.getEmail())) {
            return true;
        }

        User professional = service.getProfessional();
        return Boolean.TRUE.equals(service.getActive())
                && Boolean.TRUE.equals(professional.getActive())
                && professional.getApprovalStatus() == ApprovalStatus.APPROVED;
    }

    private void checkOwnership(Service service) {
        if (currentUserService.hasRole(Role.PROFESSIONAL)
                && !service.getProfessional().getEmail().equals(currentUserService.getEmail())) {
            throw new AccessDeniedException("Não tem acesso a este serviço.");
        }
    }
}
