package com.bilangieri.agendamento.auth.service;

import com.bilangieri.agendamento.exception.TooManyRequestsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Proteção contra força bruta no login: após N passwords erradas para o mesmo email, esse email fica
 * bloqueado durante um período. Conta por email (não por IP, que atrás de um proxy não identifica ninguém)
 * e também conta emails inexistentes, para não revelar quais existem.
 * Estado em memória: chega para uma instância; com várias réplicas passaria para Redis.
 */
@Service
public class LoginAttemptService {

    // Acima deste tamanho, as entradas já expiradas são removidas para a memória não crescer sem limite
    private static final int CLEANUP_THRESHOLD = 10_000;

    private record Failures(int count, Instant windowStart) {
    }

    private final Map<String, Failures> failuresByEmail = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final Duration lockDuration;
    private final Clock clock;

    public LoginAttemptService(
            @Value("${api.security.login.max-attempts}") int maxAttempts,
            @Value("${api.security.login.lock-duration}") Duration lockDuration,
            Clock clock
    ) {
        this.maxAttempts = maxAttempts;
        this.lockDuration = lockDuration;
        this.clock = clock;
    }

    public void checkAllowed(String email) {
        Failures failures = activeFailures(email);
        if (failures != null && failures.count() >= maxAttempts) {
            Duration retryAfter = Duration.between(clock.instant(), failures.windowStart().plus(lockDuration));
            throw new TooManyRequestsException(
                    "Demasiadas tentativas de login falhadas. Tente novamente mais tarde.", retryAfter);
        }
    }

    public void recordFailure(String email) {
        Instant now = clock.instant();
        failuresByEmail.compute(email, (key, current) ->
                current == null || isExpired(current, now)
                        ? new Failures(1, now)
                        : new Failures(current.count() + 1, current.windowStart()));

        if (failuresByEmail.size() > CLEANUP_THRESHOLD) {
            failuresByEmail.values().removeIf(failures -> isExpired(failures, now));
        }
    }

    public void recordSuccess(String email) {
        failuresByEmail.remove(email);
    }

    private Failures activeFailures(String email) {
        Failures failures = failuresByEmail.get(email);
        return failures == null || isExpired(failures, clock.instant()) ? null : failures;
    }

    private boolean isExpired(Failures failures, Instant now) {
        return !now.isBefore(failures.windowStart().plus(lockDuration));
    }
}
