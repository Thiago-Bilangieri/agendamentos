package com.bilangieri.agendamento;

import com.bilangieri.agendamento.appointment.entity.Appointment;
import com.bilangieri.agendamento.appointment.entity.AppointmentStatus;
import com.bilangieri.agendamento.appointment.repository.AppointmentRepository;
import com.bilangieri.agendamento.auth.service.JwtService;
import com.bilangieri.agendamento.customer.entity.Customer;
import com.bilangieri.agendamento.customer.repository.CustomerRepository;
import com.bilangieri.agendamento.service.entity.Service;
import com.bilangieri.agendamento.service.repository.ServiceRepository;
import com.bilangieri.agendamento.user.entity.User;
import com.bilangieri.agendamento.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Base dos testes de integração: aplicação completa (segurança incluída) sobre um PostgreSQL
 * em Testcontainers, com as migrações e os dados de teste (db/testdata) do perfil dev.
 * Cada teste corre numa transação revertida no fim, por isso os testes não interferem entre si.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
public abstract class IntegrationTest {

    protected static final String ADMIN = "admin@agendamento.com";
    protected static final String ANA = "ana.ribeiro@agendamento.com";
    protected static final String BRUNO = "bruno.matos@agendamento.com";
    protected static final String DIOGO_PENDING = "diogo.ferreira@agendamento.com";
    protected static final String JOAO = "joao.silva@email.com";
    protected static final String MARIA = "maria.santos@email.com";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JwtService jwtService;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected ServiceRepository serviceRepository;

    @Autowired
    protected CustomerRepository customerRepository;

    @Autowired
    protected AppointmentRepository appointmentRepository;

    protected String bearer(String email) {
        return "Bearer " + jwtService.generateToken(email);
    }

    protected User user(String email) {
        return userRepository.findByEmail(email).orElseThrow();
    }

    protected Customer customer(String email) {
        return customerRepository.findByEmail(email).orElseThrow();
    }

    protected Service service(String professionalEmail, String name) {
        return serviceRepository.findByNameAndProfessionalId(name, user(professionalEmail).getId()).orElseThrow();
    }

    // Data futura sem agendamentos nos dados de teste
    protected LocalDateTime nextYearAt(int hour, int minute) {
        return LocalDate.now().plusYears(1).atTime(hour, minute);
    }

    protected Appointment saveAppointment(Customer customer, Service service, LocalDateTime startAt, AppointmentStatus status) {
        return appointmentRepository.save(Appointment.builder()
                .customer(customer)
                .service(service)
                .professional(service.getProfessional())
                .startAt(startAt)
                .endAt(startAt.plusMinutes(service.getDurationMinutes()))
                .status(status)
                .build());
    }
}
