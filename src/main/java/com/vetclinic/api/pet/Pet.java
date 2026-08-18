package com.vetclinic.api.pet;

import com.vetclinic.api.client.Client;
import com.vetclinic.api.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * Animal atendido pela clínica, vinculado a um tutor (Client).
 */
@Entity
@Table(name = "pets")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Pet extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String species;

    private String breed;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "weight_kg")
    private Double weightKg;

    private String notes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;
}
