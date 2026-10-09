package com.bilangieri.agendamento.exception;

// Operação incompatível com o estado atual dos dados (ex.: remover um registo ainda referenciado)
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
