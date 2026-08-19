package com.vetclinic.api.medicalrecord;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MedicalRecordAttachmentRepository extends JpaRepository<MedicalRecordAttachment, UUID> {

    List<MedicalRecordAttachment> findByMedicalRecordIdOrderByCreatedAtDesc(UUID medicalRecordId);
}
