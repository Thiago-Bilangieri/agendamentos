package com.bilangieri.agendamento.user.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class StrongPasswordValidatorTest {

    private final StrongPasswordValidator validator = new StrongPasswordValidator();

    @ParameterizedTest
    @ValueSource(strings = {"secret12", "Password1", "pâssword9", "a1234567", "12345678a"})
    void acceptsPasswordsWithLettersAndDigits(String password) {
        assertThat(validator.isValid(password, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"password", "12345678", "abc123", "        ", "!!!!!!!!1"})
    void rejectsWeakPasswords(String password) {
        // "        " é branco: fica para o @NotBlank
        assertThat(validator.isValid(password, null)).isEqualTo(password.isBlank());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void leavesMissingPasswordsToNotBlank(String password) {
        assertThat(validator.isValid(password, null)).isTrue();
    }

    @Test
    void enforcesTheLengthLimits() {
        assertThat(validator.isValid("abcdef1", null)).isFalse();
        assertThat(validator.isValid("abcdefg1", null)).isTrue();
        assertThat(validator.isValid("a1" + "x".repeat(70), null)).isTrue();
        assertThat(validator.isValid("a1" + "x".repeat(71), null)).isFalse();
    }

    // O limite do BCrypt é em bytes: 36 caracteres de 2 bytes já chegam aos 72
    @Test
    void countsTheBcryptLimitInBytes() {
        assertThat(validator.isValid("1" + "é".repeat(35), null)).isTrue();
        assertThat(validator.isValid("1" + "é".repeat(36), null)).isFalse();
    }
}
