package com.bilangieri.agendamento.professional;

import com.bilangieri.agendamento.IntegrationTest;
import com.bilangieri.agendamento.appointment.entity.AppointmentStatus;
import com.bilangieri.agendamento.service.entity.Service;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WorkingHoursIntegrationTest extends IntegrationTest {

    // ===================== Horário de trabalho =====================

    @Test
    void anyoneCanSeeAProfessionalsWeeklySchedule() throws Exception {
        // Ana: terça a sábado, dois blocos por dia
        mockMvc.perform(get("/api/professionals/{id}/working-hours", user(ANA).getId())
                        .header("Authorization", bearer(JOAO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)))
                .andExpect(jsonPath("$[0].dayOfWeek").value("TUESDAY"))
                .andExpect(jsonPath("$[0].startTime").value("09:00:00"))
                .andExpect(jsonPath("$[1].startTime").value("14:00:00"));
    }

    @Test
    void workingHoursOfSomeoneWhoIsNotAProfessionalIsNotFound() throws Exception {
        mockMvc.perform(get("/api/professionals/{id}/working-hours", user(JOAO).getId())
                        .header("Authorization", bearer(ADMIN)))
                .andExpect(status().isNotFound());
    }

    @Test
    void professionalReplacesOwnSchedule() throws Exception {
        mockMvc.perform(put("/api/professionals/me/working-hours").header("Authorization", bearer(ANA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"hours": [
                                  {"dayOfWeek": "MONDAY", "startTime": "08:00", "endTime": "12:00"},
                                  {"dayOfWeek": "MONDAY", "startTime": "12:00", "endTime": "16:00"}
                                ]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get("/api/professionals/{id}/working-hours", user(ANA).getId())
                        .header("Authorization", bearer(ANA)))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].dayOfWeek", not(hasItem("TUESDAY"))));
    }

    @Test
    void overlappingBlocksAreRejected() throws Exception {
        mockMvc.perform(put("/api/professionals/me/working-hours").header("Authorization", bearer(ANA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"hours": [
                                  {"dayOfWeek": "MONDAY", "startTime": "09:00", "endTime": "13:00"},
                                  {"dayOfWeek": "MONDAY", "startTime": "12:00", "endTime": "18:00"}
                                ]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("sobrepõem")));
    }

    @Test
    void blockMustEndAfterItStarts() throws Exception {
        mockMvc.perform(put("/api/professionals/me/working-hours").header("Authorization", bearer(ANA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"hours": [{"dayOfWeek": "MONDAY", "startTime": "18:00", "endTime": "09:00"}]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingFieldsFailValidation() throws Exception {
        mockMvc.perform(put("/api/professionals/me/working-hours").header("Authorization", bearer(ANA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"hours": [{"dayOfWeek": "MONDAY"}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['hours[0].startTime']").exists());
    }

    @Test
    void onlyProfessionalsEditTheirSchedule() throws Exception {
        mockMvc.perform(put("/api/professionals/me/working-hours").header("Authorization", bearer(JOAO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"hours": []}
                                """))
                .andExpect(status().isForbidden());
    }

    // ===================== Disponibilidade =====================

    @Test
    void availabilityListsEveryFreeSlotWithinWorkingHours() throws Exception {
        // Corte Feminino (60 min), quarta: 09:00-13:00 -> 09:00..12:00 (7) e 14:00-19:00 -> 14:00..18:00 (9)
        Service corte = service(ANA, "Corte Feminino");

        mockMvc.perform(get("/api/services/{id}/availability", corte.getId())
                        .param("date", nextYearWednesday().toString())
                        .header("Authorization", bearer(JOAO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(16)))
                .andExpect(jsonPath("$[0].startAt", containsString("T09:00")))
                .andExpect(jsonPath("$[0].endAt", containsString("T10:00")))
                .andExpect(jsonPath("$[*].startAt", not(hasItem(containsString("T12:30")))))
                .andExpect(jsonPath("$[*].startAt", not(hasItem(containsString("T13:00")))));
    }

    @Test
    void bookedTimeIsNoLongerAvailable() throws Exception {
        Service corte = service(ANA, "Corte Feminino");
        saveAppointment(customer(MARIA), corte, nextYearAt(10, 0), AppointmentStatus.SCHEDULED);

        // 09:30, 10:00 e 10:30 sobrepõem o agendamento das 10:00-11:00
        mockMvc.perform(get("/api/services/{id}/availability", corte.getId())
                        .param("date", nextYearWednesday().toString())
                        .header("Authorization", bearer(JOAO)))
                .andExpect(jsonPath("$", hasSize(13)))
                .andExpect(jsonPath("$[*].startAt", not(hasItem(containsString("T10:00")))))
                .andExpect(jsonPath("$[*].startAt", hasItem(containsString("T11:00"))));
    }

    @Test
    void noSlotsOnADayOff() throws Exception {
        // Ana não trabalha à segunda-feira
        mockMvc.perform(get("/api/services/{id}/availability", service(ANA, "Corte Feminino").getId())
                        .param("date", nextYearWednesday().minusDays(2).toString())
                        .header("Authorization", bearer(JOAO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void availabilityOfHiddenServiceIsNotFound() throws Exception {
        mockMvc.perform(get("/api/services/{id}/availability", service(BRUNO, "Pintura de Barba").getId())
                        .param("date", nextYearWednesday().toString())
                        .header("Authorization", bearer(JOAO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void availabilityRequiresAValidDate() throws Exception {
        mockMvc.perform(get("/api/services/{id}/availability", service(ANA, "Corte Feminino").getId())
                        .param("date", "amanha")
                        .header("Authorization", bearer(JOAO)))
                .andExpect(status().isBadRequest());
    }

    // ===================== Marcação dentro do horário =====================

    @Test
    void bookingOutsideWorkingHoursIsRejected() throws Exception {
        Service corte = service(ANA, "Corte Feminino");

        // 12:30-13:30 atravessa a pausa de almoço
        mockMvc.perform(post("/api/appointments").header("Authorization", bearer(JOAO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId": %d, "serviceId": %d, "startAt": "%s"}
                                """.formatted(customer(JOAO).getId(), corte.getId(), nextYearAt(12, 30))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("fora do horário de trabalho")));
    }

    @Test
    void everyAvailableSlotCanActuallyBeBooked() throws Exception {
        Service corte = service(ANA, "Corte Feminino");

        // O último horário livre do dia (18:00-19:00) é aceite pelo agendamento
        mockMvc.perform(post("/api/appointments").header("Authorization", bearer(JOAO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId": %d, "serviceId": %d, "startAt": "%s"}
                                """.formatted(customer(JOAO).getId(), corte.getId(), nextYearAt(18, 0))))
                .andExpect(status().isCreated());
    }
}
