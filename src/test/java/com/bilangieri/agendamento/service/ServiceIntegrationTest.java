package com.bilangieri.agendamento.service;

import com.bilangieri.agendamento.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ServiceIntegrationTest extends IntegrationTest {

    @Test
    void customerSeesOnlyTheAvailableCatalog() throws Exception {
        mockMvc.perform(get("/api/services").header("Authorization", bearer(JOAO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].active", everyItem(is(true))))
                .andExpect(jsonPath("$.content[*].name", not(hasItem("Pintura de Barba"))))
                .andExpect(jsonPath("$.content[*].name", not(hasItem("Massagem Relaxante"))))
                .andExpect(jsonPath("$.content[*].name", not(hasItem("Treino Personalizado"))));
    }

    @Test
    void professionalSeesOnlyOwnServicesIncludingInactive() throws Exception {
        mockMvc.perform(get("/api/services").header("Authorization", bearer(BRUNO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(4)))
                .andExpect(jsonPath("$.content[*].professionalName", everyItem(is("Bruno Matos"))))
                .andExpect(jsonPath("$.content[*].name", hasItem("Pintura de Barba")));
    }

    @Test
    void filterByProfessionalRespectsTheCallersRole() throws Exception {
        Long brunoId = user(BRUNO).getId();

        mockMvc.perform(get("/api/services").param("professionalId", brunoId.toString())
                        .header("Authorization", bearer(JOAO)))
                .andExpect(jsonPath("$.content", hasSize(3)));

        mockMvc.perform(get("/api/services").param("professionalId", brunoId.toString())
                        .header("Authorization", bearer(ADMIN)))
                .andExpect(jsonPath("$.content", hasSize(4)));
    }

    @Test
    void customerGetsEmptyListForProfessionalNotApproved() throws Exception {
        mockMvc.perform(get("/api/services").param("professionalId", user(DIOGO_PENDING).getId().toString())
                        .header("Authorization", bearer(JOAO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void hiddenServicesReturnNotFoundToCustomers() throws Exception {
        Long inactive = service(BRUNO, "Pintura de Barba").getId();
        Long fromPendingProfessional = service(DIOGO_PENDING, "Massagem Relaxante").getId();

        mockMvc.perform(get("/api/services/{id}", inactive).header("Authorization", bearer(JOAO)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/services/{id}", fromPendingProfessional).header("Authorization", bearer(JOAO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownerAndAdminCanSeeHiddenServices() throws Exception {
        Long inactive = service(BRUNO, "Pintura de Barba").getId();

        mockMvc.perform(get("/api/services/{id}", inactive).header("Authorization", bearer(BRUNO)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/services/{id}", inactive).header("Authorization", bearer(ADMIN)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/services/{id}", inactive).header("Authorization", bearer(ANA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void professionalCreatesServiceForThemselves() throws Exception {
        mockMvc.perform(post("/api/services").header("Authorization", bearer(ANA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Penteado", "price": 30.00, "durationMinutes": 50, "professionalId": 999}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.professionalId").value(user(ANA).getId()));
    }

    @Test
    void customerCannotCreateServices() throws Exception {
        mockMvc.perform(post("/api/services").header("Authorization", bearer(JOAO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Penteado", "price": 30.00, "durationMinutes": 50}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void professionalCannotEditAnotherProfessionalsService() throws Exception {
        Long anasService = service(ANA, "Brushing").getId();

        mockMvc.perform(put("/api/services/{id}", anasService).header("Authorization", bearer(BRUNO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Brushing", "price": 1.00, "durationMinutes": 45, "active": true}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void deletingServiceWithAppointmentsIsAConflict() throws Exception {
        Long withAppointments = service(BRUNO, "Corte Masculino").getId();

        mockMvc.perform(delete("/api/services/{id}", withAppointments).header("Authorization", bearer(BRUNO)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void deletingServiceWithoutAppointmentsWorks() throws Exception {
        Long withoutAppointments = service(BRUNO, "Pintura de Barba").getId();

        mockMvc.perform(delete("/api/services/{id}", withoutAppointments).header("Authorization", bearer(BRUNO)))
                .andExpect(status().isNoContent());
    }
}
