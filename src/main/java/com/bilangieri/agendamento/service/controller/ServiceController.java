package com.bilangieri.agendamento.service.controller;

import com.bilangieri.agendamento.appointment.dto.TimeSlot;
import com.bilangieri.agendamento.appointment.service.AvailabilityService;
import com.bilangieri.agendamento.config.OpenApiConfig;
import com.bilangieri.agendamento.service.dto.ServiceCreateRequest;
import com.bilangieri.agendamento.service.dto.ServiceFilter;
import com.bilangieri.agendamento.service.dto.ServiceResponse;
import com.bilangieri.agendamento.service.dto.ServiceUpdateRequest;
import com.bilangieri.agendamento.service.service.ServiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
@Tag(name = OpenApiConfig.TAG_SERVICES)
public class ServiceController {

    private final ServiceService serviceService;
    private final AvailabilityService availabilityService;

    @PostMapping
    @Operation(summary = "Criar serviço",
            description = "**ADMIN, PROFESSIONAL.** O prestador cria sempre para si próprio; o ADMIN indica o `professionalId`.")
    @ApiResponse(responseCode = "201", description = "Serviço criado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou nome repetido para o mesmo prestador")
    @ApiResponse(responseCode = "404", description = "Prestador ou categoria inexistente")
    public ResponseEntity<ServiceResponse> create(@RequestBody @Valid ServiceCreateRequest request) {
        ServiceResponse response = serviceService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar serviços", description = """
            **Todos os perfis.** O resultado depende de quem pede:
            - **CUSTOMER:** só o catálogo disponível (serviços ativos de prestadores ativos e aprovados)
            - **PROFESSIONAL:** só os seus serviços, incluindo inativos (`professionalId` é ignorado)
            - **ADMIN:** todos

            Os filtros são opcionais e combinam-se entre si. Ordenação por omissão: `name`.""")
    public ResponseEntity<Page<ServiceResponse>> findAll(
            @ParameterObject ServiceFilter filter,
            @ParameterObject @PageableDefault(sort = "name") Pageable pageable) {
        return ResponseEntity.ok(serviceService.findAll(filter, pageable));
    }

    @GetMapping("/{id}/availability")
    @Operation(summary = "Horários livres de um serviço num dia", description = """
            **Todos os perfis.** Inícios possíveis de 30 em 30 minutos, dentro do horário de trabalho do prestador, \
            sem sobrepor agendamentos ativos e ainda no futuro. Qualquer horário devolvido pode ser marcado em \
            `POST /api/appointments`. Lista vazia num dia de folga.""")
    @ApiResponse(responseCode = "200", description = "Horários livres, por ordem")
    @ApiResponse(responseCode = "400", description = "Data em formato inválido")
    @ApiResponse(responseCode = "404", description = "Serviço inexistente ou fora do catálogo")
    public ResponseEntity<List<TimeSlot>> findAvailability(
            @PathVariable Long id,
            @Parameter(description = "Dia a consultar (ISO, `yyyy-MM-dd`)", example = "2030-01-16")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(availabilityService.findAvailableSlots(id, date));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe de um serviço",
            description = "**Todos os perfis.** Um serviço fora do catálogo responde `404` a quem não o pode ver.")
    @ApiResponse(responseCode = "404", description = "Serviço inexistente ou fora do catálogo")
    public ResponseEntity<ServiceResponse> findById(@PathVariable Long id) {
        ServiceResponse response = serviceService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar serviço", description = "**ADMIN, PROFESSIONAL** (só os seus).")
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou nome repetido")
    @ApiResponse(responseCode = "404", description = "Serviço ou categoria inexistente")
    public ResponseEntity<ServiceResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid ServiceUpdateRequest request) {
        ServiceResponse response = serviceService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover serviço",
            description = "**ADMIN, PROFESSIONAL** (só os seus). Serviços com agendamentos não podem ser removidos: desative-os.")
    @ApiResponse(responseCode = "204", description = "Removido")
    @ApiResponse(responseCode = "404", description = "Serviço inexistente")
    @ApiResponse(responseCode = "409", description = "O serviço tem agendamentos associados")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        serviceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
