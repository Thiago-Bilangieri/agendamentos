package com.bilangieri.agendamento.service;

import com.bilangieri.agendamento.IntegrationTest;
import com.bilangieri.agendamento.service.entity.Service;
import com.bilangieri.agendamento.service.repository.ServiceSpecifications;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ServiceIntegrationTest extends IntegrationTest {

    @PersistenceContext
    private EntityManager entityManager;

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
    void nameFilterTreatsLikeWildcardsAsPlainText() throws Exception {
        for (String wildcard : new String[]{"%", "_"}) {
            mockMvc.perform(get("/api/services").param("name", wildcard)
                            .header("Authorization", bearer(ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)));
        }
    }

    @Test
    void searchFindsServicesByProfessionalName() throws Exception {
        mockMvc.perform(get("/api/services").param("q", "ana").header("Authorization", bearer(JOAO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[*].professionalName", everyItem(is("Ana Ribeiro"))))
                .andExpect(jsonPath("$.content[*].name", containsInAnyOrder("Brushing", "Coloração", "Corte Feminino")));
    }

    @Test
    void searchIgnoresCase() throws Exception {
        for (String text : new String[]{"CORTE", "corte"}) {
            mockMvc.perform(get("/api/services").param("q", text).header("Authorization", bearer(JOAO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].name",
                            containsInAnyOrder("Corte Feminino", "Corte Masculino", "Corte + Barba")));
        }
    }

    @Test
    void searchIgnoresAccentsInTheTextAndInTheData() throws Exception {
        mockMvc.perform(get("/api/services").param("q", "depilacao").header("Authorization", bearer(JOAO)))
                .andExpect(jsonPath("$.content[*].name", contains("Depilação Pernas")));
        mockMvc.perform(get("/api/services").param("q", "COLORAÇÃO").header("Authorization", bearer(JOAO)))
                .andExpect(jsonPath("$.content[*].name", contains("Coloração")));
        // Também no nome do prestador, e com acentos a mais no texto pesquisado
        mockMvc.perform(get("/api/services").param("q", "ribéiro").header("Authorization", bearer(JOAO)))
                .andExpect(jsonPath("$.content", hasSize(3)));
    }

    @Test
    void searchCombinesWithCategory() throws Exception {
        Long cabelo = service(ANA, "Corte Feminino").getCategory().getId();

        mockMvc.perform(get("/api/services").param("q", "corte").param("categoryId", cabelo.toString())
                        .header("Authorization", bearer(JOAO)))
                .andExpect(jsonPath("$.content[*].name", contains("Corte Feminino")));
    }

    @Test
    void searchTreatsLikeWildcardsAsPlainText() throws Exception {
        serviceRepository.save(Service.builder().name("Corte 50% desconto").price(new BigDecimal("10.00"))
                .durationMinutes(30).professional(user(ANA)).build());

        mockMvc.perform(get("/api/services").param("q", "%").header("Authorization", bearer(ADMIN)))
                .andExpect(jsonPath("$.content[*].name", contains("Corte 50% desconto")));
        mockMvc.perform(get("/api/services").param("q", "_").header("Authorization", bearer(ADMIN)))
                .andExpect(jsonPath("$.content", hasSize(0)));
        mockMvc.perform(get("/api/services").param("q", "\\").header("Authorization", bearer(ADMIN)))
                .andExpect(jsonPath("$.content", hasSize(0)));
        mockMvc.perform(get("/api/services").param("q", "d'Ana").header("Authorization", bearer(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void searchKeepsHiddenServicesOutOfTheCustomerCatalog() throws Exception {
        // Inativo, prestador pendente e prestador inativo
        for (String text : new String[]{"pintura", "massagem", "diogo", "filipe"}) {
            mockMvc.perform(get("/api/services").param("q", text).header("Authorization", bearer(JOAO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.page.totalElements").value(0));
        }
        // O ADMIN continua a vê-los
        mockMvc.perform(get("/api/services").param("q", "massagem").header("Authorization", bearer(ADMIN)))
                .andExpect(jsonPath("$.content[*].name", contains("Massagem Relaxante")));
    }

    @Test
    void searchPaginationCountsEachServiceOnce() throws Exception {
        // Os 9 serviços do catálogo do cliente têm "a" no nome do serviço e/ou no do prestador:
        // os que têm nos dois contam uma só vez
        mockMvc.perform(get("/api/services").param("q", "a").param("size", "2").header("Authorization", bearer(JOAO)))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.page.totalElements").value(9));
    }

    @Test
    void catalogSearchUsesASingleJoinWithTheProfessional() {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Service> query = cb.createQuery(Service.class);
        Root<Service> root = query.from(Service.class);

        Specification.allOf(ServiceSpecifications.bookable(), ServiceSpecifications.serviceOrProfessionalNameContains("ana"))
                .toPredicate(root, query, cb);

        assertThat(root.getJoins()).hasSize(1);
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
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void deletingServiceWithoutAppointmentsWorks() throws Exception {
        Long withoutAppointments = service(BRUNO, "Pintura de Barba").getId();

        mockMvc.perform(delete("/api/services/{id}", withoutAppointments).header("Authorization", bearer(BRUNO)))
                .andExpect(status().isNoContent());
    }
}
