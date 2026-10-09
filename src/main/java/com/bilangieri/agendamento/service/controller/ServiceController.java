package com.bilangieri.agendamento.service.controller;

import com.bilangieri.agendamento.appointment.dto.TimeSlot;
import com.bilangieri.agendamento.appointment.service.AvailabilityService;
import com.bilangieri.agendamento.service.dto.ServiceCreateRequest;
import com.bilangieri.agendamento.service.dto.ServiceFilter;
import com.bilangieri.agendamento.service.dto.ServiceResponse;
import com.bilangieri.agendamento.service.dto.ServiceUpdateRequest;
import com.bilangieri.agendamento.service.service.ServiceService;
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
public class ServiceController {

    private final ServiceService serviceService;
    private final AvailabilityService availabilityService;

    @PostMapping
    public ResponseEntity<ServiceResponse> create(@RequestBody @Valid ServiceCreateRequest request) {
        ServiceResponse response = serviceService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<ServiceResponse>> findAll(
            @ParameterObject ServiceFilter filter,
            @ParameterObject @PageableDefault(sort = "name") Pageable pageable) {
        return ResponseEntity.ok(serviceService.findAll(filter, pageable));
    }

    @GetMapping("/{id}/availability")
    public ResponseEntity<List<TimeSlot>> findAvailability(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(availabilityService.findAvailableSlots(id, date));
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
