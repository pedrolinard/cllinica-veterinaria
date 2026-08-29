package com.vetclinic.api.report;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Locale;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Reports", description = "Exportação de relatórios em CSV ou PDF (parâmetro format)")
public class ReportController {

    private static final MediaType CSV_UTF8 = new MediaType("text", "csv", StandardCharsets.UTF_8);
    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private final ReportService reportService;
    private final ReportPdfWriter reportPdfWriter;

    @GetMapping("/daily-agenda")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    @Operation(summary = "Exporta a agenda de um dia (format=csv|pdf, padrão csv)")
    public ResponseEntity<byte[]> dailyAgenda(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "csv") String format
    ) {
        String basename = "agenda-" + date;
        if (isPdf(format)) {
            return pdfResponse(reportPdfWriter.dailyAgenda(date, reportService.dailyAgenda(date)), basename + ".pdf");
        }
        return csvResponse(reportService.dailyAgendaCsv(date), basename + ".csv");
    }

    @GetMapping("/billing")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Exporta o faturamento por serviço num período (format=csv|pdf, padrão csv)")
    public ResponseEntity<byte[]> billing(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "csv") String format
    ) {
        String basename = "faturamento-" + from + "_a_" + to;
        if (isPdf(format)) {
            return pdfResponse(reportPdfWriter.billing(reportService.billing(from, to)), basename + ".pdf");
        }
        return csvResponse(reportService.billingCsv(from, to), basename + ".csv");
    }

    private boolean isPdf(String format) {
        String normalized = format == null ? "csv" : format.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "pdf" -> true;
            case "csv", "" -> false;
            default -> throw new IllegalArgumentException("Formato inválido: use 'csv' ou 'pdf'.");
        };
    }

    private ResponseEntity<byte[]> csvResponse(String csv, String filename) {
        // BOM UTF-8 para o Excel reconhecer acentuação corretamente ao abrir o CSV.
        byte[] body = csv.getBytes(StandardCharsets.UTF_8);
        byte[] withBom = new byte[UTF8_BOM.length + body.length];
        System.arraycopy(UTF8_BOM, 0, withBom, 0, UTF8_BOM.length);
        System.arraycopy(body, 0, withBom, UTF8_BOM.length, body.length);

        return fileResponse(withBom, CSV_UTF8, filename);
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filename) {
        return fileResponse(pdf, MediaType.APPLICATION_PDF, filename);
    }

    private ResponseEntity<byte[]> fileResponse(byte[] body, MediaType contentType, String filename) {
        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .body(body);
    }
}
