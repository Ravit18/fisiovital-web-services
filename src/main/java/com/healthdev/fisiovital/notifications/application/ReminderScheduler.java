package com.healthdev.fisiovital.notifications.application;

import com.healthdev.fisiovital.notifications.domain.model.NotificationType;
import com.healthdev.fisiovital.treatment.domain.model.Session;
import com.healthdev.fisiovital.treatment.infrastructure.persistence.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.healthdev.fisiovital.shared.infrastructure.config.I18nConfig.ES_419;

/**
 * US13 - Cada 10 minutos busca sesiones que empiezan dentro de las proximas 24 horas
 * y envia el recordatorio por correo (si esta configurado) y como notificacion en la plataforma.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm");

    private final SessionRepository sessionRepository;
    private final NotificationService notificationService;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final MessageSource messageSource;

    @Value("${app.reminders.hours-before:24}")
    private long hoursBefore;

    @Value("${app.mail.from}")
    private String mailFrom;

    @Scheduled(fixedDelay = 600_000, initialDelay = 30_000)
    @Transactional
    public void sendReminders() {
        LocalDateTime now = LocalDateTime.now();
        List<Session> pending = sessionRepository.findPendingReminders(now, now.plusHours(hoursBefore));
        for (Session session : pending) {
            String physioName = session.getPlan().getPhysiotherapist().getFullName();
            String when = session.getSlot().getStartTime().format(FORMAT);
            var patientUser = session.getPlan().getPatient().getUser();

            notificationService.notify(patientUser, session, NotificationType.SESSION_REMINDER,
                    "notification.reminder", physioName, when);
            sendEmail(patientUser.getEmail(), messageSource.getMessage("notification.reminder",
                    new Object[]{physioName, when}, ES_419));
            session.markReminderSent();
        }
        if (!pending.isEmpty()) {
            log.info("Recordatorios procesados: {}", pending.size());
        }
    }

    private void sendEmail(String to, String text) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            return; // Correo no configurado: queda solo la notificacion en la plataforma.
        }
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(mailFrom);
            mail.setTo(to);
            mail.setSubject(messageSource.getMessage("mail.reminder.subject", null, ES_419));
            mail.setText(text);
            sender.send(mail);
        } catch (Exception e) {
            log.warn("No se pudo enviar el recordatorio a {}: {}", to, e.getMessage());
        }
    }
}
