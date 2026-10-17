package com.bilangieri.agendamento.appointment;

import com.bilangieri.agendamento.IntegrationTest;
import com.bilangieri.agendamento.appointment.entity.Appointment;
import com.bilangieri.agendamento.appointment.entity.AppointmentStatus;
import com.bilangieri.agendamento.appointment.entity.AppointmentStatusHistory;
import com.bilangieri.agendamento.appointment.repository.AppointmentStatusHistoryRepository;
import com.bilangieri.agendamento.service.entity.Service;
import com.bilangieri.agendamento.user.entity.Role;
import com.bilangieri.agendamento.user.entity.User;
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

    @Autowired
    private AppointmentStatusHistoryRepository statusHistoryRepository;

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

        // 1 do filtro JWT (carrega o utilizador) + 1 dos agendamentos com os JOINs
        // + 1 das entradas de cancelamento da página (os dados de teste têm agendamentos CANCELLED).
        // Sem count: os 17 agendamentos cabem na primeira página (20), por isso o Spring Data já sabe o total
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
    }

    @Test
    void cancellationDetailsCostOneQueryPerPageWhateverTheNumberOfCancelledAppointments() throws Exception {
        Service brushing = service(ANA, "Brushing");
        User ana = user(ANA);
        // Só 2 a mais: com 19 agendamentos a página (20) continua incompleta e não há consulta de count
        for (int hour = 10; hour < 12; hour++) {
            Appointment cancelled = saveAppointment(customer(JOAO), brushing, nextYearAt(hour, 0), AppointmentStatus.CANCELLED);
            statusHistoryRepository.save(AppointmentStatusHistory.builder()
                    .appointment(cancelled).fromStatus(AppointmentStatus.SCHEDULED).toStatus(AppointmentStatus.CANCELLED)
                    .changedBy(ana).changedByRole(Role.PROFESSIONAL).reason("Prestador doente")
                    .changedAt(now()).build());
        }
        entityManager.flush();
        entityManager.clear();
        statistics.clear();

        mockMvc.perform(get("/api/appointments").header("Authorization", bearer(ADMIN)))
                .andExpect(status().isOk());

        // Os mesmos 3 do teste acima: as entradas de cancelamento vêm todas na mesma consulta (sem N+1)
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
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
