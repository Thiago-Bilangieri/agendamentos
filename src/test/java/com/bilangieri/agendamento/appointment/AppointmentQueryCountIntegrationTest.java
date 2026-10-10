package com.bilangieri.agendamento.appointment;

import com.bilangieri.agendamento.IntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Garante que as listagens não fazem N+1: cliente, serviço e prestador vêm no mesmo SELECT dos agendamentos.
// Sem o @EntityGraph, cada cliente, serviço e prestador diferente da página custava uma consulta extra.
class AppointmentQueryCountIntegrationTest extends IntegrationTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @PersistenceContext
    private EntityManager entityManager;

    private Statistics statistics;

    @BeforeEach
    void enableStatistics() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
    }

    @Test
    void adminListLoadsAppointmentsAndRelationsInOneQuery() throws Exception {
        mockMvc.perform(get("/api/appointments").header("Authorization", bearer(ADMIN)))
                .andExpect(status().isOk());

        // 1 do filtro JWT (carrega o utilizador) + 1 dos agendamentos com os JOINs.
        // Sem count: os 17 agendamentos cabem na primeira página (20), por isso o Spring Data já sabe o total
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
    }

    @Test
    void detailLoadsAppointmentAndRelationsInOneQuery() throws Exception {
        Long id = appointmentRepository.findAll().getFirst().getId();
        // Esvazia a sessão para o pedido não aproveitar entidades já carregadas acima
        entityManager.clear();
        statistics.clear();

        mockMvc.perform(get("/api/appointments/" + id).header("Authorization", bearer(ADMIN)))
                .andExpect(status().isOk());

        // 1 do filtro JWT + 1 do agendamento com os JOINs
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
    }
}
