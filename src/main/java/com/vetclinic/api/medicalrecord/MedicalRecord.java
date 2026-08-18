package com.vetclinic.api.medicalrecord;

import com.vetclinic.api.appointment.Appointment;
import com.vetclinic.api.common.BaseEntity;
import com.vetclinic.api.pet.Pet;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Prontuário médico gerado a partir de uma consulta concluída.
 */
@Entity
@Table(name = "medical_records")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class MedicalRecord extends BaseEntity {

    @Column(columnDefinition = "TEXT")
    private String diagnosis;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "weight_kg")
    private Double weightKg;

    @Column(columnDefinition = "TEXT")
    private String vaccines;

    @Column(columnDefinition = "TEXT")
    private String prescriptions;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;
}
