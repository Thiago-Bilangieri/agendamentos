package com.bilangieri.agendamento.service.service;

import com.bilangieri.agendamento.appointment.repository.AppointmentRepository;
import com.bilangieri.agendamento.category.entity.ServiceCategory;
import com.bilangieri.agendamento.category.service.CategoryService;
import com.bilangieri.agendamento.exception.BusinessException;
import com.bilangieri.agendamento.exception.ConflictException;
import com.bilangieri.agendamento.exception.NotFoundException;
import com.bilangieri.agendamento.security.CurrentUserService;
import com.bilangieri.agendamento.service.dto.ServiceCreateRequest;
import com.bilangieri.agendamento.service.dto.ServiceFilter;
import com.bilangieri.agendamento.service.dto.ServiceResponse;
import com.bilangieri.agendamento.service.dto.ServiceUpdateRequest;
import com.bilangieri.agendamento.service.entity.Service;
import com.bilangieri.agendamento.service.repository.ServiceRepository;
import com.bilangieri.agendamento.service.repository.ServiceSpecifications;
import com.bilangieri.agendamento.user.entity.Role;
import com.bilangieri.agendamento.user.entity.User;
import com.bilangieri.agendamento.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@org.springframework.stereotype.Service // Evita conflito com o nome da classe Service
@RequiredArgsConstructor
public class ServiceService {

    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final CurrentUserService currentUserService;
    private final CategoryService categoryService;

    @Transactional
    public ServiceResponse create(ServiceCreateRequest request) {
        User professional = resolveProfessional(request.professionalId());

        if (serviceRepository.findByNameAndProfessionalId(request.name(), professional.getId()).isPresent()) {
            throw new BusinessException("Já tem um serviço registado com este nome.");
        }

        Service service = Service.builder()
                .professional(professional)
                .category(resolveCategory(request.categoryId()))
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
    // Os filtros combinam-se entre si; para o PROFESSIONAL o professionalId é ignorado.
    @Transactional(readOnly = true)
    public Page<ServiceResponse> findAll(ServiceFilter filter, Pageable pageable) {
        List<Specification<Service>> specs = new ArrayList<>();

        if (currentUserService.hasRole(Role.PROFESSIONAL)) {
            specs.add(ServiceSpecifications.ofProfessional(currentUserService.getUser().getId()));
        } else {
            if (!currentUserService.hasRole(Role.ADMIN)) {
                specs.add(ServiceSpecifications.bookable());
            }
            if (filter.professionalId() != null) {
                specs.add(ServiceSpecifications.ofProfessional(filter.professionalId()));
            }
        }
        if (filter.categoryId() != null) {
            specs.add(ServiceSpecifications.inCategory(filter.categoryId()));
        }
        if (filter.name() != null && !filter.name().isBlank()) {
            specs.add(ServiceSpecifications.nameContains(filter.name()));
        }

        return serviceRepository.findAll(Specification.allOf(specs), pageable)
                .map(ServiceResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public ServiceResponse findById(Long id) {
        return ServiceResponse.fromEntity(getVisibleService(id));
    }

    // Serviços fora do catálogo respondem como inexistentes para quem não os pode ver
    @Transactional(readOnly = true)
    public Service getVisibleService(Long id) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Serviço não encontrado com o ID: " + id));

        if (!canView(service)) {
            throw new NotFoundException("Serviço não encontrado com o ID: " + id);
        }

        return service;
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
        service.setCategory(resolveCategory(request.categoryId()));

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

    private ServiceCategory resolveCategory(Long categoryId) {
        return categoryId == null ? null : categoryService.getCategory(categoryId);
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

        return service.isBookable();
    }

    private void checkOwnership(Service service) {
        if (currentUserService.hasRole(Role.PROFESSIONAL)
                && !service.getProfessional().getEmail().equals(currentUserService.getEmail())) {
            throw new AccessDeniedException("Não tem acesso a este serviço.");
        }
    }
}
