package com.bilangieri.agendamento.appointment.repository;

import com.bilangieri.agendamento.appointment.entity.AppointmentStatus;
import com.bilangieri.agendamento.appointment.entity.AppointmentStatusHistory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentStatusHistoryRepository extends JpaRepository<AppointmentStatusHistory, Long> {

    // Ordem cronológica; o id desempata mudanças registadas no mesmo instante
    @EntityGraph(attributePaths = "changedBy")
    List<AppointmentStatusHistory> findByAppointmentIdOrderByChangedAtAscIdAsc(Long appointmentId);

    // CANCELLED é final, por isso cada agendamento tem no máximo uma entrada de cancelamento
    Optional<AppointmentStatusHistory> findFirstByAppointmentIdAndToStatusOrderByChangedAtDesc(
            Long appointmentId, AppointmentStatus toStatus);

    // Uma só consulta para as entradas de cancelamento de uma página inteira de agendamentos (sem N+1)
    List<AppointmentStatusHistory> findByAppointmentIdInAndToStatus(Collection<Long> appointmentIds, AppointmentStatus toStatus);
}
