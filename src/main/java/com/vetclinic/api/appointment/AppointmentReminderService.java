package com.vetclinic.api.appointment;

import com.vetclinic.api.client.Client;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Envia um lembrete por email na véspera de cada consulta ativa (agendada ou
 * confirmada). Desligado por padrão — só existe como bean quando
 * app.reminders.enabled=true, e nesse caso exige um servidor SMTP configurado
 * (spring.mail.*), senão o boot falha por falta do bean JavaMailSender — de
 * propósito, para não mascarar reminders "ligados" que na prática não enviam nada.
 */
@Component
@ConditionalOnProperty(prefix = "app.reminders", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class AppointmentReminderService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final AppointmentRepository appointmentRepository;
    private final JavaMailSender mailSender;

    @Value("${app.reminders.from}")
    private String fromAddress;

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional(readOnly = true)
    public void sendTomorrowReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDateTime start = tomorrow.atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        List<Appointment> appointments = appointmentRepository
                .findByStatusNotAndScheduledAtBetweenOrderByScheduledAt(AppointmentStatus.CANCELED, start, end)
                .stream()
                .filter(a -> a.getStatus() == AppointmentStatus.SCHEDULED || a.getStatus() == AppointmentStatus.CONFIRMED)
                .toList();

        int sent = 0;
        for (Appointment appointment : appointments) {
            Client client = appointment.getPet().getClient();
            if (!StringUtils.hasText(client.getEmail())) {
                continue;
            }
            try {
                sendReminder(appointment, client);
                sent++;
            } catch (Exception ex) {
                log.warn("Falha ao enviar lembrete de consulta {} para {}: {}",
                        appointment.getId(), client.getEmail(), ex.getMessage());
            }
        }
        log.info("Lembretes de consulta: {} enviado(s) de {} candidato(s) para amanhã.", sent, appointments.size());
    }

    private void sendReminder(Appointment appointment, Client client) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(client.getEmail());
        message.setSubject("Lembrete: consulta de " + appointment.getPet().getName() + " amanhã");
        message.setText(
                "Olá, " + client.getName() + "!\n\n"
                        + "Este é um lembrete de que " + appointment.getPet().getName()
                        + " tem consulta agendada para amanhã às " + TIME_FORMAT.format(appointment.getScheduledAt())
                        + " com " + appointment.getVet().getName() + ".\n\n"
                        + "Qualquer dúvida, entre em contato com a clínica."
        );
        mailSender.send(message);
    }
}
