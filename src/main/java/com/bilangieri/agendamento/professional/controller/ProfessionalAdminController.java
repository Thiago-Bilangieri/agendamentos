package com.bilangieri.agendamento.professional.controller;

import com.bilangieri.agendamento.config.OpenApiConfig;
import com.bilangieri.agendamento.professional.dto.ProfessionalResponse;
import com.bilangieri.agendamento.professional.service.ProfessionalService;
import com.bilangieri.agendamento.user.entity.ApprovalStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/professionals")
@RequiredArgsConstructor
@Tag(name = OpenApiConfig.TAG_ADMIN)
public class ProfessionalAdminController {

    private final ProfessionalService professionalService;

    @GetMapping
    @Operation(summary = "Listar prestadores por estado", description = "**ADMIN.** Ordenação por omissão: `name`.")
    public ResponseEntity<Page<ProfessionalResponse>> findByStatus(
            @Parameter(description = "Estado da aprovação") @RequestParam(defaultValue = "PENDING") ApprovalStatus status,
            @ParameterObject @SortDefault(sort = "name") Pageable pageable) {
        return ResponseEntity.ok(professionalService.findByStatus(status, pageable));
    }

    @PatchMapping("/{id}/approve")
    @Operation(summary = "Aprovar prestador", description = "**ADMIN.** O prestador passa a poder fazer login.")
    @ApiResponse(responseCode = "404", description = "Prestador inexistente")
    public ResponseEntity<ProfessionalResponse> approve(@PathVariable Long id) {
        ProfessionalResponse response = professionalService.updateApprovalStatus(id, ApprovalStatus.APPROVED);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/reject")
    @Operation(summary = "Rejeitar prestador", description = "**ADMIN.**")
    @ApiResponse(responseCode = "404", description = "Prestador inexistente")
    public ResponseEntity<ProfessionalResponse> reject(@PathVariable Long id) {
        ProfessionalResponse response = professionalService.updateApprovalStatus(id, ApprovalStatus.REJECTED);
        return ResponseEntity.ok(response);
    }
}
