package com.bilangieri.agendamento.exception;

import lombok.Getter;

// Erro de um campo validado no service (ex.: password atual errada): responde como os erros de @Valid,
// com "errors": { campo: mensagem }, para o front o mostrar junto do campo
@Getter
public class FieldValidationException extends RuntimeException {

    private final String field;

    public FieldValidationException(String field, String message) {
        super(message);
        this.field = field;
    }
}
