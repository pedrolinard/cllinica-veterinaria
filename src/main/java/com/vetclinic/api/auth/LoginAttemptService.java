package com.vetclinic.api.auth;

import com.vetclinic.api.common.exception.TooManyAttemptsException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limita tentativas de login por email para dificultar força bruta contra uma
 * conta específica. Em memória: suficiente para uma única instância; se a API
 * rodar com múltiplas réplicas, isso precisa migrar para um contador
 * compartilhado (ex: Redis).
 */
@Component
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration BLOCK_DURATION = Duration.ofMinutes(15);

    private final ConcurrentHashMap<String, Attempt> attemptsByEmail = new ConcurrentHashMap<>();

    public void checkAllowed(String email) {
        Attempt attempt = attemptsByEmail.get(email);
        if (attempt != null && attempt.count.get() >= MAX_ATTEMPTS && Instant.now().isBefore(attempt.blockedUntil)) {
            throw new TooManyAttemptsException(
                    "Muitas tentativas de login para este email. Tente novamente em alguns minutos."
            );
        }
    }

    public void registerFailure(String email) {
        attemptsByEmail.compute(email, (key, existing) -> {
            Instant now = Instant.now();
            if (existing == null || now.isAfter(existing.blockedUntil)) {
                return new Attempt(new AtomicInteger(1), now.plus(BLOCK_DURATION));
            }
            existing.count.incrementAndGet();
            return existing;
        });
    }

    public void registerSuccess(String email) {
        attemptsByEmail.remove(email);
    }

    private static final class Attempt {
        private final AtomicInteger count;
        private final Instant blockedUntil;

        private Attempt(AtomicInteger count, Instant blockedUntil) {
            this.count = count;
            this.blockedUntil = blockedUntil;
        }
    }
}
