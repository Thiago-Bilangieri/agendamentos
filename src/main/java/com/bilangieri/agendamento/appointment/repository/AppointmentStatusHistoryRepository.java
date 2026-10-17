package com.bilangieri.agendamento.appointment.repository;

import com.bilangieri.agendamento.appointment.entity.AppointmentStatusHistory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppointmentStatusHistoryRepository extends JpaRepository<AppointmentStatusHistory, Long> {

    // Ordem cronológica; o id desempata mudanças registadas no mesmo instante
    @EntityGraph(attributePaths = "changedBy")
    List<AppointmentStatusHistory> findByAppointmentIdOrderByChangedAtAscIdAsc(Long appointmentId);
}
