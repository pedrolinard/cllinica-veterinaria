package com.vetclinic.api.report;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.vetclinic.api.report.ReportService.AgendaEntry;
import com.vetclinic.api.report.ReportService.BillingLine;
import com.vetclinic.api.report.ReportService.BillingReport;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Serializa os relatórios do {@link ReportService} em PDF: um documento com
 * cabeçalho, tabela e totais, pronto para impressão ou anexo de email.
 */
@Component
class ReportPdfWriter {

    private static final DateTimeFormatter DATE_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Color HEADER_BG = new Color(0x33, 0x3D, 0x4E);
    private static final Color ZEBRA_BG = new Color(0xF2, 0xF4, 0xF7);

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLACK);
    private static final Font META_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
    private static final Font TH_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
    private static final Font TD_FONT = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
    private static final Font TD_BOLD_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
    private static final Font FOOTER_FONT = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);

    byte[] dailyAgenda(LocalDate date, List<AgendaEntry> entries) {
        return render(PageSize.A4.rotate(), doc -> {
            addHeader(doc, "Agenda do dia",
                    "Data: " + DATE_BR.format(date) + "   •   " + entries.size() + " consulta(s)");

            if (entries.isEmpty()) {
                doc.add(new Paragraph("Nenhuma consulta para esta data.", TD_FONT));
                return;
            }

            PdfPTable table = new PdfPTable(new float[]{1.1f, 2.4f, 2.4f, 2.4f, 1.9f, 2.4f, 3.2f});
            table.setWidthPercentage(100);
            table.setSpacingBefore(14);
            headerRow(table, "Hora", "Pet", "Tutor", "Veterinário", "Status", "Serviço", "Motivo");

            boolean zebra = false;
            for (AgendaEntry e : entries) {
                Color bg = zebra ? ZEBRA_BG : Color.WHITE;
                bodyCell(table, e.time(), bg, false);
                bodyCell(table, e.pet(), bg, false);
                bodyCell(table, e.tutor(), bg, false);
                bodyCell(table, e.vet(), bg, false);
                bodyCell(table, e.status(), bg, false);
                bodyCell(table, e.service(), bg, false);
                bodyCell(table, e.reason(), bg, false);
                zebra = !zebra;
            }
            doc.add(table);
        });
    }

    byte[] billing(BillingReport report) {
        return render(PageSize.A4, doc -> {
            addHeader(doc, "Faturamento por serviço",
                    "Período: " + DATE_BR.format(report.from()) + " a " + DATE_BR.format(report.to())
                            + "   •   consultas concluídas");

            PdfPTable table = new PdfPTable(new float[]{4f, 1.5f, 2f, 2f});
            table.setWidthPercentage(100);
            table.setSpacingBefore(14);
            headerRow(table, "Serviço", "Qtd.", "Preço unitário", "Subtotal");

            boolean zebra = false;
            for (BillingLine line : report.lines()) {
                Color bg = zebra ? ZEBRA_BG : Color.WHITE;
                bodyCell(table, line.service(), bg, false);
                numberCell(table, Long.toString(line.quantity()), bg, false);
                numberCell(table, money(line.unitPriceCents()), bg, false);
                numberCell(table, money(line.subtotalCents()), bg, false);
                zebra = !zebra;
            }

            bodyCell(table, "TOTAL", ZEBRA_BG, true);
            numberCell(table, "", ZEBRA_BG, true);
            numberCell(table, "", ZEBRA_BG, true);
            numberCell(table, money(report.totalCents()), ZEBRA_BG, true);

            doc.add(table);
        });
    }

    // ---------------------------------------------------------------- helpers

    @FunctionalInterface
    private interface DocumentBody {
        void write(Document doc) throws DocumentException;
    }

    private byte[] render(Rectangle pageSize, DocumentBody body) {
        Document doc = new Document(pageSize, 36, 36, 42, 42);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();
            body.write(doc);
            addFooter(doc);
        } catch (DocumentException e) {
            throw new IllegalStateException("Falha ao gerar o PDF do relatório", e);
        } finally {
            doc.close();
        }
        return out.toByteArray();
    }

    private void addHeader(Document doc, String title, String meta) throws DocumentException {
        doc.add(new Paragraph("Vet Clinic", META_FONT));
        Paragraph titleP = new Paragraph(title, TITLE_FONT);
        titleP.setSpacingBefore(2);
        doc.add(titleP);
        doc.add(new Paragraph(meta, META_FONT));
    }

    private void addFooter(Document doc) throws DocumentException {
        Paragraph footer = new Paragraph(
                "Gerado em " + DATE_TIME_BR.format(LocalDateTime.now()), FOOTER_FONT);
        footer.setSpacingBefore(16);
        doc.add(footer);
    }

    private void headerRow(PdfPTable table, String... labels) {
        for (String label : labels) {
            PdfPCell cell = new PdfPCell(new Phrase(label, TH_FONT));
            cell.setBackgroundColor(HEADER_BG);
            cell.setPadding(5);
            cell.setBorderColor(HEADER_BG);
            table.addCell(cell);
        }
        table.setHeaderRows(1);
    }

    private void bodyCell(PdfPTable table, String text, Color background, boolean bold) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, bold ? TD_BOLD_FONT : TD_FONT));
        cell.setBackgroundColor(background);
        cell.setPadding(4);
        cell.setBorderColor(new Color(0xDD, 0xDD, 0xDD));
        table.addCell(cell);
    }

    private void numberCell(PdfPTable table, String text, Color background, boolean bold) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, bold ? TD_BOLD_FONT : TD_FONT));
        cell.setBackgroundColor(background);
        cell.setPadding(4);
        cell.setBorderColor(new Color(0xDD, 0xDD, 0xDD));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cell);
    }

    private String money(long cents) {
        return "R$ " + ReportService.formatCents(cents).replace('.', ',');
    }
}
