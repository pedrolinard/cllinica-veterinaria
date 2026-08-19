package com.vetclinic.api.medicalrecord;

import com.vetclinic.api.medicalrecord.dto.MedicalRecordAttachmentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Anexos (exames, fotos, laudos) de um prontuário médico.
 */
@RestController
@RequestMapping("/api/medical-records")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Medical Record Attachments", description = "Anexos dos prontuários médicos")
public class MedicalRecordAttachmentController {

    private final MedicalRecordAttachmentService attachmentService;

    @PostMapping(value = "/{recordId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('VET', 'ADMIN')")
    @Operation(summary = "Anexa um arquivo (imagem ou PDF, até 5MB) a um prontuário")
    public ResponseEntity<MedicalRecordAttachmentResponse> upload(
            @PathVariable UUID recordId, @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(attachmentService.upload(recordId, file));
    }

    @GetMapping("/{recordId}/attachments")
    @Operation(summary = "Lista os anexos de um prontuário")
    public ResponseEntity<List<MedicalRecordAttachmentResponse>> list(@PathVariable UUID recordId) {
        return ResponseEntity.ok(attachmentService.findByRecord(recordId));
    }

    @GetMapping("/attachments/{id}/download")
    @Operation(summary = "Baixa o conteúdo de um anexo")
    public ResponseEntity<byte[]> download(@PathVariable UUID id) {
        MedicalRecordAttachment attachment = attachmentService.getOrThrow(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(attachment.getFilename(), StandardCharsets.UTF_8).build().toString())
                .body(attachment.getContent());
    }

    @DeleteMapping("/attachments/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove um anexo")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        attachmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
