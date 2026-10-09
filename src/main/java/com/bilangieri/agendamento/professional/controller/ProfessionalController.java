package com.bilangieri.agendamento.professional.controller;

import com.bilangieri.agendamento.config.OpenApiConfig;
import com.bilangieri.agendamento.professional.dto.WeeklyScheduleRequest;
import com.bilangieri.agendamento.professional.dto.WorkingHoursResponse;
import com.bilangieri.agendamento.professional.service.WorkingHoursService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/professionals")
@RequiredArgsConstructor
@Tag(name = OpenApiConfig.TAG_PROFESSIONALS)
public class ProfessionalController {

    private final WorkingHoursService workingHoursService;

    @GetMapping("/{id}/working-hours")
    @Operation(summary = "Horário de trabalho de um prestador",
            description = "**Todos os perfis.** Blocos semanais, por dia e hora de início.")
    @ApiResponse(responseCode = "404", description = "Prestador inexistente")
    public ResponseEntity<List<WorkingHoursResponse>> findWorkingHours(@PathVariable Long id) {
        return ResponseEntity.ok(workingHoursService.findByProfessional(id));
    }

    @PutMapping("/me/working-hours")
    @Operation(summary = "Definir o meu horário de trabalho", description = """
            **PROFESSIONAL.** Substitui o horário semanal completo. Um dia pode ter vários blocos \
            (ex.: pausa para almoço), desde que não se sobreponham. Os agendamentos já marcados não são alterados.""")
    @ApiResponse(responseCode = "400", description = "Bloco inválido ou blocos sobrepostos")
    public ResponseEntity<List<WorkingHoursResponse>> replaceMyWorkingHours(
            @RequestBody @Valid WeeklyScheduleRequest request) {
        return ResponseEntity.ok(workingHoursService.replaceMine(request));
    }
}
