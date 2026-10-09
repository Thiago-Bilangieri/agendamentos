package com.bilangieri.agendamento.professional.entity;

import com.bilangieri.agendamento.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

// Um bloco de trabalho semanal de um prestador (ex.: segunda, 09:00-13:00)
@Entity
@Table(name = "working_hours")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class WorkingHours {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professional_id", nullable = false)
    private User professional;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    // O intervalo [start, end) cabe inteiramente neste bloco
    public boolean contains(LocalTime start, LocalTime end) {
        return !start.isBefore(startTime) && !end.isAfter(endTime);
    }
}
