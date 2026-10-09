package com.bilangieri.agendamento.appointment.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static com.bilangieri.agendamento.appointment.entity.AppointmentStatus.*;
import static org.assertj.core.api.Assertions.assertThat;

class AppointmentStatusTest {

    @ParameterizedTest
    @CsvSource({
            "SCHEDULED, CONFIRMED",
            "SCHEDULED, COMPLETED",
            "SCHEDULED, CANCELLED",
            "SCHEDULED, NO_SHOW",
            "CONFIRMED, COMPLETED",
            "CONFIRMED, CANCELLED",
            "CONFIRMED, NO_SHOW"
    })
    void allowsValidTransitions(AppointmentStatus from, AppointmentStatus to) {
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "CONFIRMED, SCHEDULED",
            "SCHEDULED, SCHEDULED",
            "CONFIRMED, CONFIRMED"
    })
    void rejectsGoingBackOrStayingInTheSameStatus(AppointmentStatus from, AppointmentStatus to) {
        assertThat(from.canTransitionTo(to)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"COMPLETED", "CANCELLED", "NO_SHOW"})
    void finalStatusesCannotChange(AppointmentStatus finalStatus) {
        assertThat(finalStatus.isFinal()).isTrue();
        for (AppointmentStatus target : AppointmentStatus.values()) {
            assertThat(finalStatus.canTransitionTo(target)).isFalse();
        }
    }

    @Test
    void onlyCompletedAndNoShowRequireTheAppointmentToHaveStarted() {
        assertThat(COMPLETED.requiresStarted()).isTrue();
        assertThat(NO_SHOW.requiresStarted()).isTrue();
        assertThat(SCHEDULED.requiresStarted()).isFalse();
        assertThat(CONFIRMED.requiresStarted()).isFalse();
        assertThat(CANCELLED.requiresStarted()).isFalse();
    }
}
