package com.vetclinic.api.auth;

import com.vetclinic.api.common.exception.TooManyAttemptsException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limita tentativas de login em duas dimensões:
 *
 * <ul>
 *   <li><b>por email</b> — dificulta força bruta contra uma conta específica;</li>
 *   <li><b>por IP</b> — dificulta <i>password spraying</i> (rotacionar emails
 *       a partir de um mesmo cliente), com um teto mais alto.</li>
 * </ul>
 *
 * Em memória: suficiente para uma única instância. Com múltiplas réplicas isso
 * precisa migrar para um contador compartilhado (ex: Redis). O IP real depende
 * de {@code server.forward-headers-strategy} estar configurado atrás do proxy.
 */
@Component
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS_PER_EMAIL = 5;
    private static final int MAX_ATTEMPTS_PER_IP = 20;
    private static final Duration BLOCK_DURATION = Duration.ofMinutes(15);

    private final ConcurrentHashMap<String, Attempt> byEmail = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Attempt> byIp = new ConcurrentHashMap<>();

    public void checkAllowed(String email, String ip) {
        if (isBlocked(byEmail.get(email), MAX_ATTEMPTS_PER_EMAIL)) {
            throw new TooManyAttemptsException(
                    "Muitas tentativas de login para este email. Tente novamente em alguns minutos.");
        }
        if (isBlocked(byIp.get(ip), MAX_ATTEMPTS_PER_IP)) {
            throw new TooManyAttemptsException(
                    "Muitas tentativas de login a partir deste endereço. Tente novamente em alguns minutos.");
        }
    }

    public void registerFailure(String email, String ip) {
        bumpFailure(byEmail, email);
        bumpFailure(byIp, ip);
    }

    public void registerSuccess(String email, String ip) {
        byEmail.remove(email);
        byIp.remove(ip);
    }

    private boolean isBlocked(Attempt attempt, int max) {
        return attempt != null
                && attempt.count.get() >= max
                && Instant.now().isBefore(attempt.blockedUntil);
    }

    private void bumpFailure(ConcurrentHashMap<String, Attempt> map, String key) {
        map.compute(key, (k, existing) -> {
            Instant now = Instant.now();
            if (existing == null || now.isAfter(existing.blockedUntil)) {
                return new Attempt(new AtomicInteger(1), now.plus(BLOCK_DURATION));
            }
            existing.count.incrementAndGet();
            return existing;
        });
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
