package com.bilangieri.agendamento.professional.controller;

import com.bilangieri.agendamento.professional.dto.ProfessionalResponse;
import com.bilangieri.agendamento.professional.service.ProfessionalService;
import com.bilangieri.agendamento.user.entity.ApprovalStatus;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/professionals")
@RequiredArgsConstructor
public class ProfessionalAdminController {

    private final ProfessionalService professionalService;

    @GetMapping
    public ResponseEntity<Page<ProfessionalResponse>> findByStatus(
            @RequestParam(defaultValue = "PENDING") ApprovalStatus status,
            @ParameterObject @PageableDefault(sort = "name") Pageable pageable) {
        return ResponseEntity.ok(professionalService.findByStatus(status, pageable));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<ProfessionalResponse> approve(@PathVariable Long id) {
        ProfessionalResponse response = professionalService.updateApprovalStatus(id, ApprovalStatus.APPROVED);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<ProfessionalResponse> reject(@PathVariable Long id) {
        ProfessionalResponse response = professionalService.updateApprovalStatus(id, ApprovalStatus.REJECTED);
        return ResponseEntity.ok(response);
    }
}
