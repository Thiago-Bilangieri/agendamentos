package com.bilangieri.agendamento.appointment.controller;

import com.bilangieri.agendamento.appointment.dto.AppointmentCreateRequest;
import com.bilangieri.agendamento.appointment.dto.AppointmentResponse;
import com.bilangieri.agendamento.appointment.dto.AppointmentUpdateRequest;
import com.bilangieri.agendamento.appointment.entity.AppointmentStatus;
import com.bilangieri.agendamento.appointment.service.AppointmentService;
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
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    public ResponseEntity<AppointmentResponse> create(@RequestBody @Valid AppointmentCreateRequest request) {
        AppointmentResponse response = appointmentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<AppointmentResponse>> findAll(
            @ParameterObject @PageableDefault(sort = "startAt") Pageable pageable) {
        return ResponseEntity.ok(appointmentService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponse> findById(@PathVariable Long id) {
        AppointmentResponse response = appointmentService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AppointmentResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid AppointmentUpdateRequest request) {
        AppointmentResponse response = appointmentService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<AppointmentResponse> cancel(@PathVariable Long id) {
        AppointmentResponse response = appointmentService.updateStatus(id, AppointmentStatus.CANCELLED);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/confirm")
    public ResponseEntity<AppointmentResponse> confirm(@PathVariable Long id) {
        AppointmentResponse response = appointmentService.updateStatus(id, AppointmentStatus.CONFIRMED);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<AppointmentResponse> complete(@PathVariable Long id) {
        AppointmentResponse response = appointmentService.updateStatus(id, AppointmentStatus.COMPLETED);
        return ResponseEntity.ok(response);
    }
}
