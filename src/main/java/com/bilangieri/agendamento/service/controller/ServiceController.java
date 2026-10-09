package com.bilangieri.agendamento.service.controller;

import com.bilangieri.agendamento.service.dto.ServiceCreateRequest;
import com.bilangieri.agendamento.service.dto.ServiceResponse;
import com.bilangieri.agendamento.service.dto.ServiceUpdateRequest;
import com.bilangieri.agendamento.service.service.ServiceService;
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
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceService serviceService;

    @PostMapping
    public ResponseEntity<ServiceResponse> create(@RequestBody @Valid ServiceCreateRequest request) {
        ServiceResponse response = serviceService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<ServiceResponse>> findAll(
            @RequestParam(required = false) Long professionalId,
            @ParameterObject @PageableDefault(sort = "name") Pageable pageable) {
        return ResponseEntity.ok(serviceService.findAll(professionalId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceResponse> findById(@PathVariable Long id) {
        ServiceResponse response = serviceService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid ServiceUpdateRequest request) {
        ServiceResponse response = serviceService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        serviceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}