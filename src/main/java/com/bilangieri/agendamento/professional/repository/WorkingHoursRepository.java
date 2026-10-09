package com.bilangieri.agendamento.professional.repository;

import com.bilangieri.agendamento.professional.entity.WorkingHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.DayOfWeek;
import java.util.List;

public interface WorkingHoursRepository extends JpaRepository<WorkingHours, Long> {

    List<WorkingHours> findByProfessionalId(Long professionalId);

    List<WorkingHours> findByProfessionalIdAndDayOfWeekOrderByStartTime(Long professionalId, DayOfWeek dayOfWeek);

    @Modifying
    @Query("DELETE FROM WorkingHours w WHERE w.professional.id = :professionalId")
    void deleteByProfessionalId(@Param("professionalId") Long professionalId);
}
