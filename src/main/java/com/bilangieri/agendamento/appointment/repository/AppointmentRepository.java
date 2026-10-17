package com.bilangieri.agendamento.appointment.repository;

import com.bilangieri.agendamento.appointment.entity.Appointment;
import com.bilangieri.agendamento.appointment.entity.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // As consultas que montam AppointmentResponse trazem cliente, serviço e prestador no mesmo SELECT (JOIN),
    // em vez de uma consulta extra por relação LAZY (N+1)
    @Override
    @EntityGraph(attributePaths = {"customer", "service", "professional"})
    Page<Appointment> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "service", "professional"})
    Optional<Appointment> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"customer", "service", "professional"})
    Page<Appointment> findByCustomerId(Long customerId, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "service", "professional"})
    Page<Appointment> findByProfessionalId(Long professionalId, Pageable pageable);

    List<Appointment> findByProfessionalId(Long professionalId);

    boolean existsByServiceId(Long serviceId);

    boolean existsByCustomerId(Long customerId);

    List<Appointment> findByCustomerIdAndStatusInAndStartAtAfter(
            Long customerId, Collection<AppointmentStatus> statuses, LocalDateTime startAt);

    boolean existsByProfessionalIdAndStatusInAndStartAtAfter(
            Long professionalId, Collection<AppointmentStatus> statuses, LocalDateTime startAt);

    // Agendamentos do prestador que se sobrepõem ao intervalo [startAt, endAt): o existente começa antes do fim
    // do novo e acaba depois do início do novo. Os CANCELLED não ocupam horário; todos os outros estados ocupam.
    // É a mesma regra da restrição appointments_no_overlap (V7).
    @Query("""
        SELECT a FROM Appointment a
        WHERE a.professional.id = :professionalId
        AND a.status <> com.bilangieri.agendamento.appointment.entity.AppointmentStatus.CANCELLED
        AND (a.startAt < :endAt AND a.endAt > :startAt)
    """)
    List<Appointment> findConflictingAppointments(
            @Param("professionalId") Long professionalId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );
}