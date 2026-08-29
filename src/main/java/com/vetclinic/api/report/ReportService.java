package com.vetclinic.api.report;

import com.vetclinic.api.appointment.Appointment;
import com.vetclinic.api.appointment.AppointmentRepository;
import com.vetclinic.api.appointment.AppointmentStatus;
import com.vetclinic.api.service.ClinicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gera relatórios a partir dos dados já existentes — sem depender de
 * ferramenta externa de BI, suficiente para o volume de uma clínica.
 *
 * <p>Os dados são montados uma única vez ({@link #dailyAgenda} / {@link #billing})
 * e depois serializados para o formato pedido: CSV (planilha) aqui mesmo,
 * ou PDF (documento para impressão) em {@link ReportPdfWriter}.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final AppointmentRepository appointmentRepository;

    /** Uma linha da agenda do dia, já com os campos resolvidos para texto. */
    public record AgendaEntry(String time, String pet, String tutor, String vet,
                              String status, String service, String reason) {
    }

    /** Uma linha do faturamento: total por serviço num período. */
    public record BillingLine(String service, long quantity, long unitPriceCents, long subtotalCents) {
    }

    /** Faturamento consolidado de um período. */
    public record BillingReport(LocalDate from, LocalDate to, List<BillingLine> lines, long totalCents) {
    }

    // ------------------------------------------------------------------ dados

    public List<AgendaEntry> dailyAgenda(LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);

        List<Appointment> appointments = appointmentRepository
                .findByStatusNotAndScheduledAtBetweenOrderByScheduledAt(AppointmentStatus.CANCELED, dayStart, dayEnd);

        List<AgendaEntry> entries = new ArrayList<>(appointments.size());
        for (Appointment a : appointments) {
            entries.add(new AgendaEntry(
                    TIME_FORMAT.format(a.getScheduledAt()),
                    a.getPet().getName(),
                    a.getPet().getClient().getName(),
                    a.getVet().getName(),
                    a.getStatus().name(),
                    a.getService() != null ? a.getService().getName() : "",
                    a.getReason() != null ? a.getReason() : ""
            ));
        }
        return entries;
    }

    public BillingReport billing(LocalDate from, LocalDate to) {
        LocalDateTime rangeStart = from.atStartOfDay();
        LocalDateTime rangeEnd = to.plusDays(1).atStartOfDay();

        List<Appointment> completed = appointmentRepository
                .findByStatusAndScheduledAtBetween(AppointmentStatus.COMPLETED, rangeStart, rangeEnd);

        Map<String, long[]> totalsByService = new LinkedHashMap<>(); // nome -> [quantidade, precoUnitarioCentavos, subtotalCentavos]
        for (Appointment a : completed) {
            ClinicService service = a.getService();
            String key = service != null ? service.getName() : "Sem serviço vinculado";
            long unitPrice = service != null ? service.getPriceCents() : 0;

            long[] totals = totalsByService.computeIfAbsent(key, k -> new long[]{0, unitPrice, 0});
            totals[0] += 1;
            totals[2] += unitPrice;
        }

        List<BillingLine> lines = totalsByService.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().toLowerCase()))
                .map(e -> new BillingLine(e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2]))
                .toList();
        long grandTotal = lines.stream().mapToLong(BillingLine::subtotalCents).sum();

        return new BillingReport(from, to, lines, grandTotal);
    }

    // -------------------------------------------------------------------- CSV

    public String dailyAgendaCsv(LocalDate date) {
        StringBuilder csv = new StringBuilder();
        csv.append("Hora,Pet,Tutor,Veterinario,Status,Servico,Motivo\n");
        for (AgendaEntry e : dailyAgenda(date)) {
            csv.append(csvField(e.time())).append(',')
                    .append(csvField(e.pet())).append(',')
                    .append(csvField(e.tutor())).append(',')
                    .append(csvField(e.vet())).append(',')
                    .append(csvField(e.status())).append(',')
                    .append(csvField(e.service())).append(',')
                    .append(csvField(e.reason())).append('\n');
        }
        return csv.toString();
    }

    public String billingCsv(LocalDate from, LocalDate to) {
        BillingReport report = billing(from, to);

        StringBuilder csv = new StringBuilder();
        csv.append("Servico,Quantidade,PrecoUnitario,Subtotal\n");
        for (BillingLine line : report.lines()) {
            csv.append(csvField(line.service())).append(',')
                    .append(line.quantity()).append(',')
                    .append(formatCents(line.unitPriceCents())).append(',')
                    .append(formatCents(line.subtotalCents())).append('\n');
        }
        csv.append(csvField("TOTAL")).append(",,,").append(formatCents(report.totalCents())).append('\n');
        return csv.toString();
    }

    static String formatCents(long cents) {
        return String.format("%d.%02d", cents / 100, Math.abs(cents % 100));
    }

    private String csvField(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
