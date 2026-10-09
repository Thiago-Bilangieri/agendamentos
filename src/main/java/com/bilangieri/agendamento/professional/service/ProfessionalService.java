package com.bilangieri.agendamento.professional.service;

import com.bilangieri.agendamento.exception.NotFoundException;
import com.bilangieri.agendamento.professional.dto.ProfessionalResponse;
import com.bilangieri.agendamento.user.entity.ApprovalStatus;
import com.bilangieri.agendamento.user.entity.Role;
import com.bilangieri.agendamento.user.entity.User;
import com.bilangieri.agendamento.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfessionalService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<ProfessionalResponse> findByStatus(ApprovalStatus status, Pageable pageable) {
        return userRepository.findByRoleAndApprovalStatus(Role.PROFESSIONAL, status, pageable)
                .map(ProfessionalResponse::fromEntity);
    }

    @Transactional
    public ProfessionalResponse updateApprovalStatus(Long id, ApprovalStatus status) {
        User professional = userRepository.findById(id)
                .filter(user -> user.getRole() == Role.PROFESSIONAL)
                .orElseThrow(() -> new NotFoundException("Prestador não encontrado com o ID: " + id));

        professional.setApprovalStatus(status);
        User updated = userRepository.save(professional);
        return ProfessionalResponse.fromEntity(updated);
    }
}
