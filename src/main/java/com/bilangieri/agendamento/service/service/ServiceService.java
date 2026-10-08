package com.bilangieri.agendamento.service.service;

import com.bilangieri.agendamento.exception.BusinessException;
import com.bilangieri.agendamento.exception.NotFoundException;
import com.bilangieri.agendamento.service.dto.ServiceCreateRequest;
import com.bilangieri.agendamento.service.dto.ServiceResponse;
import com.bilangieri.agendamento.service.dto.ServiceUpdateRequest;
import com.bilangieri.agendamento.service.entity.Service;
import com.bilangieri.agendamento.service.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@org.springframework.stereotype.Service // Evita conflito com o nome da classe Service
@RequiredArgsConstructor
public class ServiceService {

    private final ServiceRepository serviceRepository;

    @Transactional
    public ServiceResponse create(ServiceCreateRequest request) {
        if (serviceRepository.findByName(request.name()).isPresent()) {
            throw new BusinessException("Já existe um serviço registado com este nome.");
        }

        Service service = Service.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .durationMinutes(request.durationMinutes())
                .active(true)
                .build();

        Service savedService = serviceRepository.save(service);
        return ServiceResponse.fromEntity(savedService);
    }

    @Transactional(readOnly = true)
    public List<ServiceResponse> findAll() {
        return serviceRepository.findAll().stream()
                .map(ServiceResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ServiceResponse findById(Long id) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Serviço não encontrado com o ID: " + id));
        return ServiceResponse.fromEntity(service);
    }

    @Transactional
    public ServiceResponse update(Long id, ServiceUpdateRequest request) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Serviço não encontrado com o ID: " + id));

        serviceRepository.findByName(request.name()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new BusinessException("Já existe outro serviço com este nome.");
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
        serviceRepository.delete(service);
    }
}