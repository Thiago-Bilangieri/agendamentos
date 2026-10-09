package com.bilangieri.agendamento.appointment.service;

import com.bilangieri.agendamento.appointment.dto.AppointmentCreateRequest;
import com.bilangieri.agendamento.appointment.dto.AppointmentResponse;
import com.bilangieri.agendamento.appointment.entity.Appointment;
import com.bilangieri.agendamento.appointment.entity.AppointmentStatus;
import com.bilangieri.agendamento.appointment.repository.AppointmentRepository;
import com.bilangieri.agendamento.customer.entity.Customer;
import com.bilangieri.agendamento.customer.repository.CustomerRepository;
import com.bilangieri.agendamento.exception.BusinessException;
import com.bilangieri.agendamento.professional.service.WorkingHoursService;
import com.bilangieri.agendamento.security.CurrentUserService;
import com.bilangieri.agendamento.service.entity.Service;
import com.bilangieri.agendamento.service.repository.ServiceRepository;
import com.bilangieri.agendamento.user.entity.ApprovalStatus;
import com.bilangieri.agendamento.user.entity.Role;
import com.bilangieri.agendamento.user.entity.User;
import com.bilangieri.agendamento.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkingHoursService workingHoursService;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private AppointmentService appointmentService;

    private User professional;
    private Customer customer;
    private Service service;

    @BeforeEach
    void setUp() {
        professional = User.builder().id(10L).name("Ana").email("ana@test.com").role(Role.PROFESSIONAL).build();
        customer = Customer.builder().id(1L).name("João").email("joao@test.com").build();
        service = Service.builder().id(100L).name("Corte").durationMinutes(45).professional(professional).build();
        // Por omissão o prestador está a trabalhar; o teste do horário de trabalho muda isto
        lenient().when(workingHoursService.isWithinWorkingHours(any(), any(), any())).thenReturn(true);
    }

    private void loggedInAs(Role role) {
        lenient().when(currentUserService.hasRole(any())).thenAnswer(invocation -> invocation.getArgument(0) == role);
    }

    private void bookingFixtures() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(serviceRepository.findById(100L)).thenReturn(Optional.of(service));
    }

    @Test
    void createCalculatesEndFromServiceDurationAndStartsAsScheduled() {
        loggedInAs(Role.ADMIN);
        bookingFixtures();
        when(appointmentRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);

        AppointmentResponse response = appointmentService.create(new AppointmentCreateRequest(1L, 100L, start, null));

        assertThat(response.endAt()).isEqualTo(start.plusMinutes(45));
        assertThat(response.status()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(response.professionalId()).isEqualTo(professional.getId());
    }

    @Test
    void createRejectsOverlappingAppointmentOfTheSameProfessional() {
        loggedInAs(Role.ADMIN);
        bookingFixtures();
        when(appointmentRepository.findConflictingAppointments(any(), any(), any()))
                .thenReturn(List.of(new Appointment()));

        assertThatThrownBy(() -> appointmentService.create(
                new AppointmentCreateRequest(1L, 100L, LocalDateTime.now().plusDays(1), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("conflituoso");

        verify(appointmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void createTranslatesDatabaseOverlapViolationIntoConflictError() {
        loggedInAs(Role.ADMIN);
        bookingFixtures();
        // Pedido simultâneo gravou primeiro: a verificação passou, mas a restrição da base de dados trava
        when(appointmentRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException(
                "insert failed", new RuntimeException("violates exclusion constraint \"appointments_no_overlap\"")));

        assertThatThrownBy(() -> appointmentService.create(
                new AppointmentCreateRequest(1L, 100L, LocalDateTime.now().plusDays(1), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("conflituoso");
    }

    @Test
    void createDoesNotHideOtherDatabaseErrors() {
        loggedInAs(Role.ADMIN);
        bookingFixtures();
        when(appointmentRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException(
                "insert failed", new RuntimeException("violates foreign key constraint \"fk_appointment_customer\"")));

        assertThatThrownBy(() -> appointmentService.create(
                new AppointmentCreateRequest(1L, 100L, LocalDateTime.now().plusDays(1), null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void createRejectsTimeOutsideTheProfessionalsWorkingHours() {
        loggedInAs(Role.ADMIN);
        bookingFixtures();
        when(workingHoursService.isWithinWorkingHours(any(), any(), any())).thenReturn(false);

        assertThatThrownBy(() -> appointmentService.create(
                new AppointmentCreateRequest(1L, 100L, LocalDateTime.now().plusDays(1), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("fora do horário de trabalho");

        verify(appointmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsInactiveService() {
        loggedInAs(Role.ADMIN);
        bookingFixtures();
        service.setActive(false);

        assertThatThrownBy(() -> appointmentService.create(
                new AppointmentCreateRequest(1L, 100L, LocalDateTime.now().plusDays(1), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não está disponível");
    }

    @Test
    void createRejectsProfessionalNotYetApproved() {
        loggedInAs(Role.ADMIN);
        bookingFixtures();
        professional.setApprovalStatus(ApprovalStatus.PENDING);

        assertThatThrownBy(() -> appointmentService.create(
                new AppointmentCreateRequest(1L, 100L, LocalDateTime.now().plusDays(1), null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void customerCannotBookForAnotherCustomer() {
        loggedInAs(Role.CUSTOMER);
        when(currentUserService.getEmail()).thenReturn("joao@test.com");
        when(customerRepository.findByUserEmail("joao@test.com")).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> appointmentService.create(
                new AppointmentCreateRequest(2L, 100L, LocalDateTime.now().plusDays(1), null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void customerBooksForThemselvesWithoutSendingTheirId() {
        loggedInAs(Role.CUSTOMER);
        when(currentUserService.getEmail()).thenReturn("joao@test.com");
        when(customerRepository.findByUserEmail("joao@test.com")).thenReturn(Optional.of(customer));
        when(serviceRepository.findById(100L)).thenReturn(Optional.of(service));
        when(appointmentRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse response = appointmentService.create(
                new AppointmentCreateRequest(null, 100L, LocalDateTime.now().plusDays(1), null));

        assertThat(response.customerId()).isEqualTo(customer.getId());
        verify(customerRepository, never()).findById(any());
    }

    @Test
    void adminMustSayWhichCustomerTheAppointmentIsFor() {
        loggedInAs(Role.ADMIN);

        assertThatThrownBy(() -> appointmentService.create(
                new AppointmentCreateRequest(null, 100L, LocalDateTime.now().plusDays(1), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ID do cliente é obrigatório");
    }

    private Appointment existingAppointment(AppointmentStatus status, LocalDateTime startAt) {
        Appointment appointment = Appointment.builder()
                .id(50L).customer(customer).service(service).professional(professional)
                .startAt(startAt).endAt(startAt.plusMinutes(45)).status(status)
                .build();
        when(appointmentRepository.findById(50L)).thenReturn(Optional.of(appointment));
        return appointment;
    }

    @Test
    void cannotCancelCompletedAppointment() {
        loggedInAs(Role.ADMIN);
        existingAppointment(AppointmentStatus.COMPLETED, LocalDateTime.now().minusDays(1));

        assertThatThrownBy(() -> appointmentService.updateStatus(50L, AppointmentStatus.CANCELLED))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("COMPLETED para CANCELLED");

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void cannotCompleteAppointmentThatHasNotStarted() {
        loggedInAs(Role.ADMIN);
        existingAppointment(AppointmentStatus.CONFIRMED, LocalDateTime.now().plusHours(2));

        assertThatThrownBy(() -> appointmentService.updateStatus(50L, AppointmentStatus.COMPLETED))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ainda não começou");
    }

    @Test
    void completesAppointmentThatAlreadyStarted() {
        loggedInAs(Role.ADMIN);
        existingAppointment(AppointmentStatus.CONFIRMED, LocalDateTime.now().minusHours(1));
        when(appointmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse response = appointmentService.updateStatus(50L, AppointmentStatus.COMPLETED);

        assertThat(response.status()).isEqualTo(AppointmentStatus.COMPLETED);
    }
}
