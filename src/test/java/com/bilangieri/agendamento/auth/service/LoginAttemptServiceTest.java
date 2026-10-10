package com.bilangieri.agendamento.auth.service;

import com.bilangieri.agendamento.exception.TooManyRequestsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginAttemptServiceTest {

    private static final String EMAIL = "joao@email.com";
    private static final Duration LOCK = Duration.ofMinutes(15);

    // Relógio controlado pelo teste, para avançar o tempo sem esperar
    private static class MutableClock extends Clock {
        private Instant now = Instant.parse("2030-03-13T12:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    private MutableClock clock;
    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock();
        service = new LoginAttemptService(3, LOCK, clock);
    }

    private void fail(int times) {
        for (int i = 0; i < times; i++) {
            service.recordFailure(EMAIL);
        }
    }

    @Test
    void allowsLoginBelowTheLimit() {
        fail(2);

        assertThatCode(() -> service.checkAllowed(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void blocksAfterTheLimitAndSaysWhenToRetry() {
        fail(3);
        clock.advance(Duration.ofMinutes(5));

        assertThatThrownBy(() -> service.checkAllowed(EMAIL))
                .isInstanceOfSatisfying(TooManyRequestsException.class,
                        ex -> assertThat(ex.getRetryAfter()).isEqualTo(Duration.ofMinutes(10)));
    }

    @Test
    void unblocksWhenTheLockExpires() {
        fail(3);
        clock.advance(LOCK);

        assertThatCode(() -> service.checkAllowed(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void failuresOutsideTheWindowStartANewCount() {
        fail(2);
        clock.advance(LOCK);
        fail(2);

        assertThatCode(() -> service.checkAllowed(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void successfulLoginResetsTheCount() {
        fail(2);
        service.recordSuccess(EMAIL);
        fail(2);

        assertThatCode(() -> service.checkAllowed(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void otherEmailsAreNotAffected() {
        fail(3);

        assertThatCode(() -> service.checkAllowed("maria@email.com")).doesNotThrowAnyException();
    }
}
