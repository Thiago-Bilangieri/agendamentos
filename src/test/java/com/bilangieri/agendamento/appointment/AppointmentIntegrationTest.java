package com.bilangieri.agendamento.appointment;

import com.bilangieri.agendamento.IntegrationTest;
import com.bilangieri.agendamento.appointment.entity.Appointment;
import com.bilangieri.agendamento.appointment.entity.AppointmentStatus;
import com.bilangieri.agendamento.service.entity.Service;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AppointmentIntegrationTest extends IntegrationTest {

    private ResultActions book(String asEmail, Long customerId, Long serviceId, LocalDateTime startAt) throws Exception {
        return mockMvc.perform(post("/api/appointments").header("Authorization", bearer(asEmail))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"customerId": %d, "serviceId": %d, "startAt": "%s"}
                        """.formatted(customerId, serviceId, startAt)));
    }

    @Test
    void customerBooksAppointmentWithEndCalculatedFromDuration() throws Exception {
        Service corte = service(ANA, "Corte Feminino"); // 60 minutos
        LocalDateTime start = nextYearAt(10, 0);

        book(JOAO, customer(JOAO).getId(), corte.getId(), start)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.professionalId").value(user(ANA).getId()))
                .andExpect(jsonPath("$.endAt", containsString("T11:00")));
    }

    @Test
    void overlappingBookingForTheSameProfessionalIsRejected() throws Exception {
        Service corte = service(ANA, "Corte Feminino");
        book(JOAO, customer(JOAO).getId(), corte.getId(), nextYearAt(10, 0)).andExpect(status().isCreated());

        book(MARIA, customer(MARIA).getId(), corte.getId(), nextYearAt(10, 30))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("conflituoso")));
    }

    @Test
    void bookingRightAfterThePreviousOneEndsIsAllowed() throws Exception {
        Service corte = service(ANA, "Corte Feminino");
        book(JOAO, customer(JOAO).getId(), corte.getId(), nextYearAt(10, 0)).andExpect(status().isCreated());

        book(MARIA, customer(MARIA).getId(), corte.getId(), nextYearAt(11, 0)).andExpect(status().isCreated());
    }

    @Test
    void cancelledAppointmentFreesTheSlot() throws Exception {
        Service corte = service(ANA, "Corte Feminino");
        saveAppointment(customer(JOAO), corte, nextYearAt(10, 0), AppointmentStatus.CANCELLED);

        book(MARIA, customer(MARIA).getId(), corte.getId(), nextYearAt(10, 0)).andExpect(status().isCreated());
    }

    @Test
    void sameTimeWithAnotherProfessionalIsAllowed() throws Exception {
        book(JOAO, customer(JOAO).getId(), service(ANA, "Corte Feminino").getId(), nextYearAt(10, 0))
                .andExpect(status().isCreated());

        book(MARIA, customer(MARIA).getId(), service(BRUNO, "Barba").getId(), nextYearAt(10, 0))
                .andExpect(status().isCreated());
    }

    @Test
    void databaseRejectsOverlappingAppointmentsEvenWithoutTheServiceCheck() {
        Service corte = service(ANA, "Corte Feminino");
        saveAppointment(customer(JOAO), corte, nextYearAt(10, 0), AppointmentStatus.SCHEDULED);

        // Grava diretamente pelo repositório, sem passar pela validação do AppointmentService
        assertThatThrownBy(() -> {
            saveAppointment(customer(MARIA), corte, nextYearAt(10, 30), AppointmentStatus.SCHEDULED);
            appointmentRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("appointments_no_overlap");
    }

    @Test
    void customerCanBookWithoutSendingTheirOwnId() throws Exception {
        mockMvc.perform(post("/api/appointments").header("Authorization", bearer(JOAO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"serviceId": %d, "startAt": "%s"}
                                """.formatted(service(ANA, "Corte Feminino").getId(), nextYearAt(10, 0))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(customer(JOAO).getId()));
    }

    @Test
    void adminMustSendTheCustomerId() throws Exception {
        mockMvc.perform(post("/api/appointments").header("Authorization", bearer(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"serviceId": %d, "startAt": "%s"}
                                """.formatted(service(ANA, "Corte Feminino").getId(), nextYearAt(10, 0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("ID do cliente é obrigatório")));
    }

    @Test
    void professionalMarksNoShowAfterTheAppointmentStarted() throws Exception {
        Appointment appointment = saveAppointment(customer(JOAO), service(ANA, "Brushing"),
                LocalDateTime.now().minusHours(2).withNano(0), AppointmentStatus.CONFIRMED);

        mockMvc.perform(patch("/api/appointments/{id}/no-show", appointment.getId()).header("Authorization", bearer(ANA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NO_SHOW"));
    }

    @Test
    void noShowCannotBeMarkedBeforeTheAppointmentStarts() throws Exception {
        Appointment appointment = saveAppointment(customer(JOAO), service(ANA, "Brushing"), nextYearAt(15, 0), AppointmentStatus.SCHEDULED);

        mockMvc.perform(patch("/api/appointments/{id}/no-show", appointment.getId()).header("Authorization", bearer(ANA)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("ainda não começou")));
    }

    @Test
    void customerCannotMarkNoShow() throws Exception {
        Appointment appointment = saveAppointment(customer(JOAO), service(ANA, "Brushing"),
                LocalDateTime.now().minusHours(2).withNano(0), AppointmentStatus.CONFIRMED);

        mockMvc.perform(patch("/api/appointments/{id}/no-show", appointment.getId()).header("Authorization", bearer(JOAO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotBookForAnotherCustomer() throws Exception {
        book(JOAO, customer(MARIA).getId(), service(ANA, "Corte Feminino").getId(), nextYearAt(10, 0))
                .andExpect(status().isForbidden());
    }

    @Test
    void serviceFromProfessionalNotApprovedCannotBeBooked() throws Exception {
        book(ADMIN, customer(JOAO).getId(), service(DIOGO_PENDING, "Massagem Relaxante").getId(), nextYearAt(10, 0))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bookingInThePastFailsValidation() throws Exception {
        book(JOAO, customer(JOAO).getId(), service(ANA, "Corte Feminino").getId(), LocalDateTime.now().minusDays(1).withNano(0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.startAt").exists());
    }

    @Test
    void professionalCannotBookAppointments() throws Exception {
        book(ANA, customer(JOAO).getId(), service(ANA, "Corte Feminino").getId(), nextYearAt(10, 0))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerOnlyListsOwnAppointments() throws Exception {
        mockMvc.perform(get("/api/appointments").header("Authorization", bearer(JOAO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].customerName", everyItem(is("João Silva"))));
    }

    @Test
    void customerCannotSeeAnotherCustomersAppointment() throws Exception {
        Appointment marias = saveAppointment(customer(MARIA), service(ANA, "Brushing"), nextYearAt(15, 0), AppointmentStatus.SCHEDULED);

        mockMvc.perform(get("/api/appointments/{id}", marias.getId()).header("Authorization", bearer(JOAO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void professionalConfirmsOwnAppointment() throws Exception {
        Appointment appointment = saveAppointment(customer(JOAO), service(ANA, "Brushing"), nextYearAt(15, 0), AppointmentStatus.SCHEDULED);

        mockMvc.perform(patch("/api/appointments/{id}/confirm", appointment.getId()).header("Authorization", bearer(ANA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void professionalCannotTouchAnotherProfessionalsAppointment() throws Exception {
        Appointment appointment = saveAppointment(customer(JOAO), service(ANA, "Brushing"), nextYearAt(15, 0), AppointmentStatus.SCHEDULED);

        mockMvc.perform(patch("/api/appointments/{id}/confirm", appointment.getId()).header("Authorization", bearer(BRUNO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotConfirmAppointments() throws Exception {
        Appointment appointment = saveAppointment(customer(JOAO), service(ANA, "Brushing"), nextYearAt(15, 0), AppointmentStatus.SCHEDULED);

        mockMvc.perform(patch("/api/appointments/{id}/confirm", appointment.getId()).header("Authorization", bearer(JOAO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCancelsOwnAppointment() throws Exception {
        Appointment appointment = saveAppointment(customer(JOAO), service(ANA, "Brushing"), nextYearAt(15, 0), AppointmentStatus.SCHEDULED);

        mockMvc.perform(patch("/api/appointments/{id}/cancel", appointment.getId()).header("Authorization", bearer(JOAO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void completedAppointmentCannotBeCancelled() throws Exception {
        Appointment appointment = saveAppointment(customer(JOAO), service(ANA, "Brushing"),
                LocalDateTime.now().minusDays(2).withNano(0), AppointmentStatus.COMPLETED);

        mockMvc.perform(patch("/api/appointments/{id}/cancel", appointment.getId()).header("Authorization", bearer(ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("COMPLETED para CANCELLED")));
    }

    @Test
    void customerCannotCancelAppointmentThatAlreadyStarted() throws Exception {
        Appointment appointment = saveAppointment(customer(JOAO), service(ANA, "Brushing"),
                LocalDateTime.now().minusDays(2).withNano(0), AppointmentStatus.SCHEDULED);

        mockMvc.perform(patch("/api/appointments/{id}/cancel", appointment.getId()).header("Authorization", bearer(JOAO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("já começou")));
    }

    @Test
    void adminUpdatesNotesOfAppointmentThatAlreadyStarted() throws Exception {
        LocalDateTime startAt = LocalDateTime.now().minusHours(1).withNano(0);
        Appointment appointment = saveAppointment(customer(JOAO), service(ANA, "Brushing"), startAt, AppointmentStatus.CONFIRMED);

        mockMvc.perform(put("/api/appointments/{id}", appointment.getId())
                        .header("Authorization", bearer(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startAt":"%s","status":"COMPLETED","notes":"Correu bem"}""".formatted(startAt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.notes").value("Correu bem"));
    }

    @Test
    void adminCannotRescheduleToThePast() throws Exception {
        Appointment appointment = saveAppointment(customer(JOAO), service(ANA, "Brushing"), nextYearAt(15, 0), AppointmentStatus.SCHEDULED);

        mockMvc.perform(put("/api/appointments/{id}", appointment.getId())
                        .header("Authorization", bearer(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startAt":"%s","status":"SCHEDULED"}""".formatted(LocalDateTime.now().minusDays(1).withNano(0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("no futuro")));
    }

    @Test
    void futureAppointmentCannotBeCompleted() throws Exception {
        Appointment appointment = saveAppointment(customer(JOAO), service(ANA, "Brushing"), nextYearAt(15, 0), AppointmentStatus.CONFIRMED);

        mockMvc.perform(patch("/api/appointments/{id}/complete", appointment.getId()).header("Authorization", bearer(ANA)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("ainda não começou")));
    }
}
