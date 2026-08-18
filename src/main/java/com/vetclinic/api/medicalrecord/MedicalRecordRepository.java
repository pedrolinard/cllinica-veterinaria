package com.vetclinic.api.medicalrecord;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, UUID> {

    Page<MedicalRecord> findByPetId(UUID petId, Pageable pageable);

    Optional<MedicalRecord> findByAppointmentId(UUID appointmentId);

    boolean existsByAppointmentId(UUID appointmentId);
}
