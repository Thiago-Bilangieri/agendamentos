package com.bilangieri.agendamento.appointment.controller;

import com.bilangieri.agendamento.appointment.dto.AppointmentCreateRequest;
import com.bilangieri.agendamento.appointment.dto.AppointmentResponse;
import com.bilangieri.agendamento.appointment.dto.AppointmentUpdateRequest;
import com.bilangieri.agendamento.appointment.entity.AppointmentStatus;
import com.bilangieri.agendamento.appointment.service.AppointmentService;
import com.bilangieri.agendamento.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@Tag(name = OpenApiConfig.TAG_APPOINTMENTS)
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    @Operation(summary = "Marcar agendamento", description = """
            **ADMIN, CUSTOMER** (o cliente só marca para si próprio). O `endAt` é calculado pela duração do serviço.
            O horário tem de estar dentro do horário de trabalho do prestador e não pode sobrepor outro agendamento \
            ativo dele (garantido também com pedidos simultâneos). Use `GET /api/services/{id}/availability` para \
            saber os horários livres.""")
    @ApiResponse(responseCode = "201", description = "Agendamento criado como `SCHEDULED`")
    @ApiResponse(responseCode = "400", description = "Dados inválidos, horário ocupado, fora do horário de trabalho ou serviço indisponível")
    @ApiResponse(responseCode = "404", description = "Cliente ou serviço inexistente")
    public ResponseEntity<AppointmentResponse> create(@RequestBody @Valid AppointmentCreateRequest request) {
        AppointmentResponse response = appointmentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar agendamentos", description = """
            **Todos os perfis.** CUSTOMER vê os seus, PROFESSIONAL os que recebeu, ADMIN todos. \
            Ordenação por omissão: `startAt`.""")
    public ResponseEntity<Page<AppointmentResponse>> findAll(
            @ParameterObject @PageableDefault(sort = "startAt") Pageable pageable) {
        return ResponseEntity.ok(appointmentService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe de um agendamento", description = "**Todos os perfis** (só agendamentos próprios).")
    @ApiResponse(responseCode = "404", description = "Agendamento inexistente")
    public ResponseEntity<AppointmentResponse> findById(@PathVariable Long id) {
        AppointmentResponse response = appointmentService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Reagendar / alterar estado e notas",
            description = "**ADMIN.** Aplica as mesmas regras da marcação e das transições de estado.")
    @ApiResponse(responseCode = "400", description = "Horário inválido ou transição de estado não permitida")
    @ApiResponse(responseCode = "404", description = "Agendamento inexistente")
    public ResponseEntity<AppointmentResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid AppointmentUpdateRequest request) {
        AppointmentResponse response = appointmentService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancelar", description = "**Todos os perfis** (só agendamentos próprios). Liberta o horário.")
    @ApiResponse(responseCode = "400", description = "O agendamento já está num estado final")
    @ApiResponse(responseCode = "404", description = "Agendamento inexistente")
    public ResponseEntity<AppointmentResponse> cancel(@PathVariable Long id) {
        AppointmentResponse response = appointmentService.updateStatus(id, AppointmentStatus.CANCELLED);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/confirm")
    @Operation(summary = "Confirmar", description = "**ADMIN, PROFESSIONAL** (só os que recebeu).")
    @ApiResponse(responseCode = "400", description = "Transição de estado não permitida")
    @ApiResponse(responseCode = "404", description = "Agendamento inexistente")
    public ResponseEntity<AppointmentResponse> confirm(@PathVariable Long id) {
        AppointmentResponse response = appointmentService.updateStatus(id, AppointmentStatus.CONFIRMED);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/complete")
    @Operation(summary = "Marcar como concluído",
            description = "**ADMIN, PROFESSIONAL** (só os que recebeu). Só depois da hora de início.")
    @ApiResponse(responseCode = "400", description = "Transição não permitida ou o agendamento ainda não começou")
    @ApiResponse(responseCode = "404", description = "Agendamento inexistente")
    public ResponseEntity<AppointmentResponse> complete(@PathVariable Long id) {
        AppointmentResponse response = appointmentService.updateStatus(id, AppointmentStatus.COMPLETED);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/no-show")
    @Operation(summary = "Marcar falta do cliente",
            description = "**ADMIN, PROFESSIONAL** (só os que recebeu). Só depois da hora de início.")
    @ApiResponse(responseCode = "400", description = "Transição não permitida ou o agendamento ainda não começou")
    @ApiResponse(responseCode = "404", description = "Agendamento inexistente")
    public ResponseEntity<AppointmentResponse> noShow(@PathVariable Long id) {
        AppointmentResponse response = appointmentService.updateStatus(id, AppointmentStatus.NO_SHOW);
        return ResponseEntity.ok(response);
    }
}
