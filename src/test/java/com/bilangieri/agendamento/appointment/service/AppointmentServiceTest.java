package com.bilangieri.agendamento.appointment.service;

import com.bilangieri.agendamento.appointment.dto.AppointmentCreateRequest;
import com.bilangieri.agendamento.appointment.dto.AppointmentResponse;
import com.bilangieri.agendamento.appointment.dto.AppointmentUpdateRequest;
import com.bilangieri.agendamento.appointment.entity.Appointment;
import com.bilangieri.agendamento.appointment.entity.AppointmentStatus;
import com.bilangieri.agendamento.appointment.entity.AppointmentStatusHistory;
import com.bilangieri.agendamento.appointment.repository.AppointmentRepository;
import com.bilangieri.agendamento.appointment.repository.AppointmentStatusHistoryRepository;
import com.bilangieri.agendamento.customer.entity.Customer;
import com.bilangieri.agendamento.customer.repository.CustomerRepository;
import com.bilangieri.agendamento.exception.BusinessException;
import com.bilangieri.agendamento.exception.ConflictException;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
    private AppointmentStatusHistoryRepository statusHistoryRepository;

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

    // Tempo fixo: as regras "no futuro" / "já começou" não dependem da hora a que o teste corre
    private static final ZoneId ZONE = ZoneId.of("Europe/Lisbon");
    private static final LocalDateTime NOW = LocalDateTime.of(2030, 3, 13, 12, 0);

    private AppointmentService appointmentService;

    private User professional;
    private Customer customer;
    private Service service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE);
        appointmentService = new AppointmentService(appointmentRepository, statusHistoryRepository, customerRepository,
                serviceRepository, userRepository, workingHoursService, currentUserService, clock);

        professional = User.builder().id(10L).name("Ana").email("ana@test.com").role(Role.PROFESSIONAL).build();
        customer = Customer.builder().id(1L).name("João").email("joao@test.com").build();
        service = Service.builder().id(100L).name("Corte").durationMinutes(45).professional(professional).build();
        // Por omissão o prestador está a trabalhar; o teste do horário de trabalho muda isto
        lenient().when(workingHoursService.isWithinWorkingHours(any(), any(), any())).thenReturn(true);
    }

    private void loggedInAs(Role role) {
        lenient().when(currentUserService.hasRole(any())).thenAnswer(invocation -> invocation.getArgument(0) == role);
        // Quem fica registado no histórico de estados
        lenient().when(currentUserService.getUser())
                .thenReturn(User.builder().id(99L).name("Utilizador " + role).role(role).build());
    }

    private AppointmentStatusHistory savedHistoryEntry() {
        ArgumentCaptor<AppointmentStatusHistory> captor = ArgumentCaptor.forClass(AppointmentStatusHistory.class);
        verify(statusHistoryRepository).save(captor.capture());
        return captor.getValue();
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
        LocalDateTime start = NOW.plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);

        AppointmentResponse response = appointmentService.create(new AppointmentCreateRequest(1L, 100L, start, null));

        assertThat(response.endAt()).isEqualTo(start.plusMinutes(45));
        assertThat(response.status()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(response.professionalId()).isEqualTo(professional.getId());
        AppointmentStatusHistory entry = savedHistoryEntry();
        assertThat(entry.getFromStatus()).isNull();
        assertThat(entry.getToStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
    }

    @Test
    void createRejectsOverlappingAppointmentOfTheSameProfessional() {
        loggedInAs(Role.ADMIN);
        bookingFixtures();
        when(appointmentRepository.findConflictingAppointments(any(), any(), any()))
                .thenReturn(List.of(new Appointment()));

        assertThatThrownBy(() -> appointmentService.create(
                new AppointmentCreateRequest(1L, 100L, NOW.plusDays(1), null)))
                .isInstanceOf(ConflictException.class)
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
                new AppointmentCreateRequest(1L, 100L, NOW.plusDays(1), null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("conflituoso");
    }

    @Test
    void createDoesNotHideOtherDatabaseErrors() {
        loggedInAs(Role.ADMIN);
        bookingFixtures();
        when(appointmentRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException(
                "insert failed", new RuntimeException("violates foreign key constraint \"fk_appointment_customer\"")));

        assertThatThrownBy(() -> appointmentService.create(
                new AppointmentCreateRequest(1L, 100L, NOW.plusDays(1), null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void createRejectsTimeOutsideTheProfessionalsWorkingHours() {
        loggedInAs(Role.ADMIN);
        bookingFixtures();
        when(workingHoursService.isWithinWorkingHours(any(), any(), any())).thenReturn(false);

        assertThatThrownBy(() -> appointmentService.create(
                new AppointmentCreateRequest(1L, 100L, NOW.plusDays(1), null)))
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
                new AppointmentCreateRequest(1L, 100L, NOW.plusDays(1), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não está disponível");
    }

    @Test
    void createRejectsProfessionalNotYetApproved() {
        loggedInAs(Role.ADMIN);
        bookingFixtures();
        professional.setApprovalStatus(ApprovalStatus.PENDING);

        assertThatThrownBy(() -> appointmentService.create(
                new AppointmentCreateRequest(1L, 100L, NOW.plusDays(1), null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void customerCannotBookForAnotherCustomer() {
        loggedInAs(Role.CUSTOMER);
        when(currentUserService.getEmail()).thenReturn("joao@test.com");
        when(customerRepository.findByUserEmail("joao@test.com")).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> appointmentService.create(
                new AppointmentCreateRequest(2L, 100L, NOW.plusDays(1), null)))
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
                new AppointmentCreateRequest(null, 100L, NOW.plusDays(1), null));

        assertThat(response.customerId()).isEqualTo(customer.getId());
        verify(customerRepository, never()).findById(any());
    }

    @Test
    void adminMustSayWhichCustomerTheAppointmentIsFor() {
        loggedInAs(Role.ADMIN);

        assertThatThrownBy(() -> appointmentService.create(
                new AppointmentCreateRequest(null, 100L, NOW.plusDays(1), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ID do cliente é obrigatório");
    }

    private Appointment existingAppointment(AppointmentStatus status, LocalDateTime startAt) {
        Appointment appointment = Appointment.builder()
                .id(50L).customer(customer).service(service).professional(professional)
                .startAt(startAt).endAt(startAt.plusMinutes(45)).status(status)
                .build();
        when(appointmentRepository.findWithDetailsById(50L)).thenReturn(Optional.of(appointment));
        return appointment;
    }

    @Test
    void cannotCancelCompletedAppointment() {
        loggedInAs(Role.ADMIN);
        existingAppointment(AppointmentStatus.COMPLETED, NOW.minusDays(1));

        assertThatThrownBy(() -> appointmentService.cancel(50L, "Engano"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("COMPLETED para CANCELLED");

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void cannotCompleteAppointmentThatHasNotStarted() {
        loggedInAs(Role.ADMIN);
        existingAppointment(AppointmentStatus.CONFIRMED, NOW.plusHours(2));

        assertThatThrownBy(() -> appointmentService.updateStatus(50L, AppointmentStatus.COMPLETED))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ainda não começou");
    }

    @Test
    void completesAppointmentThatAlreadyStarted() {
        loggedInAs(Role.ADMIN);
        existingAppointment(AppointmentStatus.CONFIRMED, NOW.minusHours(1));
        when(appointmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse response = appointmentService.updateStatus(50L, AppointmentStatus.COMPLETED);

        assertThat(response.status()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    void cannotCancelAppointmentThatAlreadyStarted() {
        loggedInAs(Role.ADMIN);
        existingAppointment(AppointmentStatus.SCHEDULED, NOW.minusDays(1));

        assertThatThrownBy(() -> appointmentService.cancel(50L, "Engano"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("já começou");

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void cancelsFutureAppointment() {
        loggedInAs(Role.ADMIN);
        existingAppointment(AppointmentStatus.CONFIRMED, NOW.plusDays(1));
        when(appointmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse response = appointmentService.cancel(50L, "  Prestador doente  ");

        assertThat(response.status()).isEqualTo(AppointmentStatus.CANCELLED);
        AppointmentStatusHistory entry = savedHistoryEntry();
        assertThat(entry.getFromStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(entry.getToStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(entry.getChangedByRole()).isEqualTo(Role.ADMIN);
        assertThat(entry.getReason()).isEqualTo("Prestador doente");
        assertThat(entry.getChangedAt()).isEqualTo(NOW);
    }

    @Test
    void professionalMustGiveAReasonToCancel() {
        loggedInAs(Role.PROFESSIONAL);
        when(currentUserService.getEmail()).thenReturn("ana@test.com");
        existingAppointment(AppointmentStatus.SCHEDULED, NOW.plusDays(1));

        assertThatThrownBy(() -> appointmentService.cancel(50L, "   "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("motivo do cancelamento é obrigatório");

        verify(appointmentRepository, never()).save(any());
        verify(statusHistoryRepository, never()).save(any());
    }

    @Test
    void adminMustGiveAReasonToCancel() {
        loggedInAs(Role.ADMIN);
        existingAppointment(AppointmentStatus.SCHEDULED, NOW.plusDays(1));

        assertThatThrownBy(() -> appointmentService.cancel(50L, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("motivo do cancelamento é obrigatório");
    }

    @Test
    void customerCancelsWithoutAReason() {
        loggedInAs(Role.CUSTOMER);
        when(currentUserService.getEmail()).thenReturn("joao@test.com");
        when(customerRepository.findByUserEmail("joao@test.com")).thenReturn(Optional.of(customer));
        existingAppointment(AppointmentStatus.SCHEDULED, NOW.plusDays(1));
        when(appointmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse response = appointmentService.cancel(50L, null);

        assertThat(response.status()).isEqualTo(AppointmentStatus.CANCELLED);
        AppointmentStatusHistory entry = savedHistoryEntry();
        assertThat(entry.getChangedByRole()).isEqualTo(Role.CUSTOMER);
        assertThat(entry.getReason()).isNull();
    }

    @Test
    void confirmingRecordsTheStatusChange() {
        loggedInAs(Role.ADMIN);
        existingAppointment(AppointmentStatus.SCHEDULED, NOW.plusDays(1));
        when(appointmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        appointmentService.updateStatus(50L, AppointmentStatus.CONFIRMED);

        AppointmentStatusHistory entry = savedHistoryEntry();
        assertThat(entry.getFromStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(entry.getToStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
    }

    @Test
    void reschedulingWithoutChangingStatusRecordsNoHistory() {
        loggedInAs(Role.ADMIN);
        existingAppointment(AppointmentStatus.SCHEDULED, NOW.plusDays(1));
        when(appointmentRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        appointmentService.update(50L, new AppointmentUpdateRequest(NOW.plusDays(2), AppointmentStatus.SCHEDULED, null));

        verify(statusHistoryRepository, never()).save(any());
    }

    @Test
    void updatesNotesOfAppointmentThatAlreadyStartedWithoutRescheduling() {
        loggedInAs(Role.ADMIN);
        LocalDateTime startAt = NOW.minusHours(1);
        existingAppointment(AppointmentStatus.CONFIRMED, startAt);
        when(appointmentRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse response = appointmentService.update(50L,
                new AppointmentUpdateRequest(startAt, AppointmentStatus.COMPLETED, "Correu bem"));

        assertThat(response.notes()).isEqualTo("Correu bem");
        assertThat(response.status()).isEqualTo(AppointmentStatus.COMPLETED);
        // Sem reagendamento não há verificação de horário nem de conflitos
        verify(appointmentRepository, never()).findConflictingAppointments(any(), any(), any());
    }

    @Test
    void cannotRescheduleToThePast() {
        loggedInAs(Role.ADMIN);
        existingAppointment(AppointmentStatus.SCHEDULED, NOW.plusDays(1));

        assertThatThrownBy(() -> appointmentService.update(50L,
                new AppointmentUpdateRequest(NOW.minusDays(1), AppointmentStatus.SCHEDULED, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no futuro");

        verify(appointmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void reschedulingStillChecksConflicts() {
        loggedInAs(Role.ADMIN);
        existingAppointment(AppointmentStatus.SCHEDULED, NOW.plusDays(1));
        LocalDateTime newStart = NOW.plusDays(2);
        Appointment other = Appointment.builder().id(51L).build();
        when(appointmentRepository.findConflictingAppointments(10L, newStart, newStart.plusMinutes(45)))
                .thenReturn(List.of(other));

        assertThatThrownBy(() -> appointmentService.update(50L,
                new AppointmentUpdateRequest(newStart, AppointmentStatus.SCHEDULED, null)))
                .isInstanceOf(ConflictException.class);
    }
}
