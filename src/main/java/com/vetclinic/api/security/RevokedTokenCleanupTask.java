package com.vetclinic.api.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Remove periodicamente da tabela de revogação os tokens que já expiraram
 * naturalmente — depois de expirados, o filtro de autenticação já os rejeitaria
 * de qualquer forma, então mantê-los é só custo de armazenamento.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RevokedTokenCleanupTask {

    private final RevokedTokenRepository revokedTokenRepository;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void purgeExpired() {
        long removed = revokedTokenRepository.deleteByExpiresAtBefore(Instant.now());
        if (removed > 0) {
            log.info("Limpeza de tokens revogados: {} registro(s) expirado(s) removido(s).", removed);
        }
    }
}
