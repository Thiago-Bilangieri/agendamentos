package com.bilangieri.agendamento.appointment.repository;

import com.bilangieri.agendamento.appointment.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // Query customizada para verificar conflitos de horário para o mesmo profissional.
    // O conflito ocorre se já existir um agendamento para o mesmo profissional onde:
    // O início do agendamento existente é anterior ao fim do novo E o fim do existente é posterior ao início do novo.
    // Excluímos agendamentos cancelados da validação, se pretendido, mas por segurança validamos todos os ativos.
    @Query("""
        SELECT a FROM Appointment a 
        WHERE a.professional.id = :professionalId 
        AND a.status <> 'CANCELLED'
        AND (a.startAt < :endAt AND a.endAt > :startAt)
    """)
    List<Appointment> findConflictingAppointments(
            @Param("professionalId") Long professionalId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );
}