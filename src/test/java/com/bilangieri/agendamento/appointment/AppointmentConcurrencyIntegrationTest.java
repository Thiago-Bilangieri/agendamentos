package com.bilangieri.agendamento.appointment;

import com.bilangieri.agendamento.IntegrationTest;
import com.bilangieri.agendamento.customer.entity.Customer;
import com.bilangieri.agendamento.service.entity.Service;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

// Sem transação de teste: cada pedido tem de fazer commit para que a corrida aconteça de verdade
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AppointmentConcurrencyIntegrationTest extends IntegrationTest {

    private static final String CARLA = "carla.nunes@agendamento.com";
    private static final int ATTEMPTS = 8;

    @AfterEach
    void cleanUp() {
        appointmentRepository.findByProfessionalId(user(CARLA).getId()).stream()
                .filter(appointment -> appointment.getStartAt().isAfter(LocalDateTime.now().plusMonths(6)))
                .forEach(appointmentRepository::delete);
    }

    @RepeatedTest(5)
    void simultaneousBookingsForTheSameSlotOnlyOneSucceeds() throws Exception {
        Service limpeza = service(CARLA, "Limpeza de Pele");
        List<String> customerEmails = List.of(JOAO, MARIA, "pedro.costa@email.com", "sofia.almeida@email.com");
        LocalDateTime start = nextYearAt(9, 0);

        CountDownLatch startSignal = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(ATTEMPTS)) {
            for (int i = 0; i < ATTEMPTS; i++) {
                String email = customerEmails.get(i % customerEmails.size());
                Customer customer = customer(email);
                String token = bearer(email);
                results.add(executor.submit(() -> {
                    startSignal.await();
                    return mockMvc.perform(post("/api/appointments").header("Authorization", token)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                                            {"customerId": %d, "serviceId": %d, "startAt": "%s"}
                                            """.formatted(customer.getId(), limpeza.getId(), start)))
                            .andReturn().getResponse().getStatus();
                }));
            }
            startSignal.countDown();
        }

        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> result : results) {
            statuses.add(result.get());
        }

        assertThat(statuses).filteredOn(status -> status == 201).hasSize(1);
        assertThat(statuses).filteredOn(status -> status != 201).allMatch(status -> status == 400);
    }
}
