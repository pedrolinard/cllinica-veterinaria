package com.vetclinic.api.medicalrecord.dto;

import com.vetclinic.api.medicalrecord.MedicalRecordAttachment;

import java.time.Instant;
import java.util.UUID;

public record MedicalRecordAttachmentResponse(
        UUID id,
        String filename,
        String contentType,
        long sizeBytes,
        Instant createdAt
) {
    public static MedicalRecordAttachmentResponse from(MedicalRecordAttachment attachment) {
        return new MedicalRecordAttachmentResponse(
                attachment.getId(),
                attachment.getFilename(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                attachment.getCreatedAt()
        );
    }
}
