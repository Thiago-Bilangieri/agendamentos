package com.bilangieri.agendamento.user.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Política das passwords novas (registo e mudança de password): 8 a 72 caracteres, com pelo menos uma letra
 * e um número. As passwords já guardadas não são revalidadas, por isso o login não usa esta anotação.
 * Um valor vazio é aceite aqui: a obrigatoriedade fica com @NotBlank, para o campo ter um só erro.
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

    String message() default "A password deve ter entre 8 e 72 caracteres e incluir pelo menos uma letra e um número.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
