package com.bilangieri.agendamento.professional.controller;

import com.bilangieri.agendamento.professional.dto.WeeklyScheduleRequest;
import com.bilangieri.agendamento.professional.dto.WorkingHoursResponse;
import com.bilangieri.agendamento.professional.service.WorkingHoursService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/professionals")
@RequiredArgsConstructor
public class ProfessionalController {

    private final WorkingHoursService workingHoursService;

    @GetMapping("/{id}/working-hours")
    public ResponseEntity<List<WorkingHoursResponse>> findWorkingHours(@PathVariable Long id) {
        return ResponseEntity.ok(workingHoursService.findByProfessional(id));
    }

    @PutMapping("/me/working-hours")
    public ResponseEntity<List<WorkingHoursResponse>> replaceMyWorkingHours(
            @RequestBody @Valid WeeklyScheduleRequest request) {
        return ResponseEntity.ok(workingHoursService.replaceMine(request));
    }
}
