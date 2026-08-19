package com.vetclinic.api.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Registro de um token JWT invalidado antes de sua expiração natural (logout).
 * {@link JwtAuthenticationFilter} rejeita qualquer token cujo jti apareça aqui.
 */
@Entity
@Table(name = "revoked_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RevokedToken {

    @Id
    private String jti;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
