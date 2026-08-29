package com.vetclinic.api.report;

import com.vetclinic.api.AbstractIntegrationTest;
import com.vetclinic.api.user.Role;
import com.vetclinic.api.user.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cobre a exportação de relatórios: o padrão continua sendo CSV e o novo
 * parâmetro {@code format=pdf} devolve um PDF de verdade, respeitando a
 * autorização por papel já existente.
 */
class ReportExportTest extends AbstractIntegrationTest {

    private static final String PDF_MAGIC = "%PDF-";

    @Test
    void agendaDoDiaEmPdfRetornaUmPdf() throws Exception {
        String token = tokenFor(createUser(Role.RECEPTIONIST));

        mockMvc.perform(get("/api/reports/daily-agenda")
                        .param("date", LocalDate.now().toString())
                        .param("format", "pdf")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", startsWith("attachment; filename")))
                .andExpect(content().string(startsWith(PDF_MAGIC)));
    }

    @Test
    void agendaDoDiaSemFormatoContinuaEmCsv() throws Exception {
        String token = tokenFor(createUser(Role.RECEPTIONIST));

        mockMvc.perform(get("/api/reports/daily-agenda")
                        .param("date", LocalDate.now().toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"));
    }

    @Test
    void faturamentoEmPdfRetornaUmPdf() throws Exception {
        String token = tokenFor(createUser(Role.ADMIN));
        LocalDate today = LocalDate.now();

        mockMvc.perform(get("/api/reports/billing")
                        .param("from", today.minusDays(7).toString())
                        .param("to", today.toString())
                        .param("format", "pdf")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(content().string(startsWith(PDF_MAGIC)));
    }

    @Test
    void formatoDesconhecidoRetorna400() throws Exception {
        String token = tokenFor(createUser(Role.ADMIN));

        mockMvc.perform(get("/api/reports/daily-agenda")
                        .param("date", LocalDate.now().toString())
                        .param("format", "xml")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void faturamentoEmPdfNegadoParaRecepcionista() throws Exception {
        String token = tokenFor(createUser(Role.RECEPTIONIST));
        LocalDate today = LocalDate.now();

        mockMvc.perform(get("/api/reports/billing")
                        .param("from", today.minusDays(7).toString())
                        .param("to", today.toString())
                        .param("format", "pdf")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
