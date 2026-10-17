package com.bilangieri.agendamento.user.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AccountUserDetailsTest {

    private static final Instant CHANGED_AT = Instant.parse("2030-03-13T12:00:00.750Z");

    private static AccountUserDetails account(Instant credentialsChangedAt) {
        return new AccountUserDetails("joao@email.com", "hash", true, List.of(), credentialsChangedAt);
    }

    @Test
    void acceptsAnyTokenWhenTheCredentialsNeverChanged() {
        assertThat(account(null).acceptsTokenIssuedAt(Date.from(Instant.parse("2020-01-01T00:00:00Z")))).isTrue();
    }

    @Test
    void rejectsTokensIssuedBeforeTheChange() {
        assertThat(account(CHANGED_AT).acceptsTokenIssuedAt(Date.from(Instant.parse("2030-03-13T11:59:59Z")))).isFalse();
    }

    // O iat do JWT é em segundos: o token emitido no mesmo segundo da mudança tem iat 12:00:00, antes das 12:00:00.750
    @Test
    void acceptsTheTokenIssuedInTheSameSecondAsTheChange() {
        assertThat(account(CHANGED_AT).acceptsTokenIssuedAt(Date.from(Instant.parse("2030-03-13T12:00:00Z")))).isTrue();
        assertThat(account(CHANGED_AT).acceptsTokenIssuedAt(Date.from(Instant.parse("2030-03-13T12:00:01Z")))).isTrue();
    }

    @Test
    void rejectsTokensWithoutIssuedAtOnceTheCredentialsChanged() {
        assertThat(account(CHANGED_AT).acceptsTokenIssuedAt(null)).isFalse();
    }
}
