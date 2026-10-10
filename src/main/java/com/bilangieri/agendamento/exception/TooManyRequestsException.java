package com.bilangieri.agendamento.exception;

import lombok.Getter;

import java.time.Duration;

// Pedido recusado por excesso de tentativas; retryAfter indica quando voltar a tentar (header Retry-After)
@Getter
public class TooManyRequestsException extends RuntimeException {

    private final Duration retryAfter;

    public TooManyRequestsException(String message, Duration retryAfter) {
        super(message);
        this.retryAfter = retryAfter;
    }
}
