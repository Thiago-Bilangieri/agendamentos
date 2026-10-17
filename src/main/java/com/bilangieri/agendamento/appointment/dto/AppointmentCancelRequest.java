package com.bilangieri.agendamento.appointment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record AppointmentCancelRequest(
        // Obrigatório para PROFESSIONAL e ADMIN (validado no AppointmentService)
        @Size(max = 500, message = "O motivo do cancelamento não pode ter mais de 500 caracteres")
        @Schema(description = "Obrigatório para PROFESSIONAL e ADMIN; opcional para o CUSTOMER",
                example = "O prestador está doente")
        String reason
) {
}
