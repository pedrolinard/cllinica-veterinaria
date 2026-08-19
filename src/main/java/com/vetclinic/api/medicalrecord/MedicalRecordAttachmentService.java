package com.vetclinic.api.medicalrecord;

import com.vetclinic.api.audit.AuditAction;
import com.vetclinic.api.audit.AuditService;
import com.vetclinic.api.common.exception.ResourceNotFoundException;
import com.vetclinic.api.medicalrecord.dto.MedicalRecordAttachmentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MedicalRecordAttachmentService {

    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif", "application/pdf"
    );

    private final MedicalRecordAttachmentRepository attachmentRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final AuditService auditService;

    @Transactional
    public MedicalRecordAttachmentResponse upload(UUID medicalRecordId, MultipartFile file) {
        MedicalRecord record = medicalRecordRepository.findById(medicalRecordId)
                .orElseThrow(() -> ResourceNotFoundException.of("Prontuário", medicalRecordId));

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo vazio.");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("Arquivo maior que o limite de 5MB.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                    "Tipo de arquivo não suportado. Envie imagem (JPEG/PNG/WEBP/GIF) ou PDF."
            );
        }

        String filename = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "arquivo";

        try {
            MedicalRecordAttachment attachment = MedicalRecordAttachment.builder()
                    .medicalRecord(record)
                    .filename(filename)
                    .contentType(contentType)
                    .sizeBytes(file.getSize())
                    .content(file.getBytes())
                    .build();
            return MedicalRecordAttachmentResponse.from(attachmentRepository.saveAndFlush(attachment));
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler o arquivo enviado.", e);
        }
    }

    public List<MedicalRecordAttachmentResponse> findByRecord(UUID medicalRecordId) {
        return attachmentRepository.findByMedicalRecordIdOrderByCreatedAtDesc(medicalRecordId).stream()
                .map(MedicalRecordAttachmentResponse::from)
                .toList();
    }

    public MedicalRecordAttachment getOrThrow(UUID id) {
        return attachmentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Anexo", id));
    }

    @Transactional
    public void delete(UUID id) {
        MedicalRecordAttachment attachment = getOrThrow(id);
        attachmentRepository.delete(attachment);
        auditService.record("MedicalRecordAttachment", id, AuditAction.DELETE,
                "Anexo removido: " + attachment.getFilename());
    }
}
