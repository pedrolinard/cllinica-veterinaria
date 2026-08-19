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

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Reports", description = "Exportação de relatórios em CSV")
public class ReportController {

    private static final MediaType CSV_UTF8 = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final ReportService reportService;

    @GetMapping("/daily-agenda")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    @Operation(summary = "Exporta a agenda de um dia (CSV)")
    public ResponseEntity<byte[]> dailyAgenda(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return csvResponse(reportService.dailyAgendaCsv(date), "agenda-" + date + ".csv");
    }

    @GetMapping("/billing")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Exporta o faturamento por serviço num período (CSV)")
    public ResponseEntity<byte[]> billing(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return csvResponse(reportService.billingCsv(from, to), "faturamento-" + from + "_a_" + to + ".csv");
    }

    private ResponseEntity<byte[]> csvResponse(String csv, String filename) {
        // BOM UTF-8 para o Excel reconhecer acentuação corretamente ao abrir o CSV.
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] body = csv.getBytes(StandardCharsets.UTF_8);
        byte[] withBom = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, withBom, 0, bom.length);
        System.arraycopy(body, 0, withBom, bom.length, body.length);

        return ResponseEntity.ok()
                .contentType(CSV_UTF8)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .body(withBom);
    }
}
