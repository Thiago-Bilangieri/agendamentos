package com.bilangieri.agendamento.appointment.entity;

public enum AppointmentStatus {
    SCHEDULED,
    CONFIRMED,
    COMPLETED,
    CANCELLED,
    NO_SHOW;

    // COMPLETED, CANCELLED e NO_SHOW são estados finais: o agendamento já não pode ser alterado
    public boolean isFinal() {
        return this == COMPLETED || this == CANCELLED || this == NO_SHOW;
    }

    public boolean canTransitionTo(AppointmentStatus target) {
        return switch (this) {
            case SCHEDULED -> target == CONFIRMED || target == COMPLETED || target == CANCELLED || target == NO_SHOW;
            case CONFIRMED -> target == COMPLETED || target == CANCELLED || target == NO_SHOW;
            case COMPLETED, CANCELLED, NO_SHOW -> false;
        };
    }

    // Só faz sentido marcar como concluído ou como falta depois de o agendamento começar
    public boolean requiresStarted() {
        return this == COMPLETED || this == NO_SHOW;
    }

    // Depois de começar, o desfecho é COMPLETED ou NO_SHOW: cancelar só antes da hora de início
    public boolean requiresNotStarted() {
        return this == CANCELLED;
    }
}
