package com.vetclinic.api.service;

import com.vetclinic.api.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Item do catálogo de serviços/produtos oferecidos pela clínica
 * (ex: consulta de rotina, vacinação, banho e tosa, exames).
 *
 * Nomeada "ClinicService" (em vez de "Service") para não colidir com a
 * anotação {@code org.springframework.stereotype.Service}.
 */
@Entity
@Table(name = "services")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ClinicService extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "price_cents", nullable = false)
    private Integer priceCents;

    @Column(name = "duration_min", nullable = false)
    @Builder.Default
    private Integer durationMin = 30;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
