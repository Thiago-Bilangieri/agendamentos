package com.bilangieri.agendamento.professional.service;

import com.bilangieri.agendamento.exception.NotFoundException;
import com.bilangieri.agendamento.professional.dto.ProfessionalResponse;
import com.bilangieri.agendamento.user.entity.ApprovalStatus;
import com.bilangieri.agendamento.user.entity.Role;
import com.bilangieri.agendamento.user.entity.User;
import com.bilangieri.agendamento.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfessionalService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ProfessionalResponse> findByStatus(ApprovalStatus status) {
        return userRepository.findByRoleAndApprovalStatus(Role.PROFESSIONAL, status).stream()
                .map(ProfessionalResponse::fromEntity)
                .toList();
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
