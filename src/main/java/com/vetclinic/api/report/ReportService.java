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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gera relatórios em CSV a partir dos dados já existentes — sem depender de
 * ferramenta externa de BI, suficiente para o volume de uma clínica.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final AppointmentRepository appointmentRepository;

    public String dailyAgendaCsv(LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);

        List<Appointment> appointments = appointmentRepository
                .findByStatusNotAndScheduledAtBetweenOrderByScheduledAt(AppointmentStatus.CANCELED, dayStart, dayEnd);

        StringBuilder csv = new StringBuilder();
        csv.append("Hora,Pet,Tutor,Veterinario,Status,Servico,Motivo\n");
        for (Appointment a : appointments) {
            csv.append(TIME_FORMAT.format(a.getScheduledAt())).append(',')
                    .append(csvField(a.getPet().getName())).append(',')
                    .append(csvField(a.getPet().getClient().getName())).append(',')
                    .append(csvField(a.getVet().getName())).append(',')
                    .append(csvField(a.getStatus().name())).append(',')
                    .append(csvField(a.getService() != null ? a.getService().getName() : "")).append(',')
                    .append(csvField(a.getReason())).append('\n');
        }
        return csv.toString();
    }

    public String billingCsv(LocalDate from, LocalDate to) {
        LocalDateTime rangeStart = from.atStartOfDay();
        LocalDateTime rangeEnd = to.plusDays(1).atStartOfDay();

        List<Appointment> completed = appointmentRepository
                .findByStatusAndScheduledAtBetween(AppointmentStatus.COMPLETED, rangeStart, rangeEnd);

        Map<String, long[]> totalsByService = new LinkedHashMap<>(); // [quantidade, precoCentavosUnitario, subtotalCentavos]
        for (Appointment a : completed) {
            ClinicService service = a.getService();
            String key = service != null ? service.getName() : "Sem serviço vinculado";
            long unitPrice = service != null ? service.getPriceCents() : 0;

            long[] totals = totalsByService.computeIfAbsent(key, k -> new long[]{0, unitPrice, 0});
            totals[0] += 1;
            totals[2] += unitPrice;
        }

        StringBuilder csv = new StringBuilder();
        csv.append("Servico,Quantidade,PrecoUnitario,Subtotal\n");
        long grandTotal = 0;
        List<Map.Entry<String, long[]>> rows = totalsByService.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().toLowerCase()))
                .toList();
        for (Map.Entry<String, long[]> entry : rows) {
            long[] totals = entry.getValue();
            csv.append(csvField(entry.getKey())).append(',')
                    .append(totals[0]).append(',')
                    .append(formatCents(totals[1])).append(',')
                    .append(formatCents(totals[2])).append('\n');
            grandTotal += totals[2];
        }
        csv.append(csvField("TOTAL")).append(",,,").append(formatCents(grandTotal)).append('\n');
        return csv.toString();
    }

    private String formatCents(long cents) {
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
