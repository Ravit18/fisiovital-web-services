package com.healthdev.fisiovital.notifications.application;

import com.healthdev.fisiovital.iam.domain.model.User;
import com.healthdev.fisiovital.notifications.domain.model.Notification;
import com.healthdev.fisiovital.notifications.domain.model.NotificationType;
import com.healthdev.fisiovital.notifications.infrastructure.persistence.NotificationRepository;
import com.healthdev.fisiovital.shared.domain.exceptions.BusinessException;
import com.healthdev.fisiovital.shared.infrastructure.config.I18nConfig;
import com.healthdev.fisiovital.treatment.domain.model.Session;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final MessageSource messageSource;

    /** Crea una notificacion dentro de la plataforma (el texto se guarda en es-419). */
    @Transactional
    public Notification notify(User user, Session session, NotificationType type, String messageKey, Object... args) {
        String text = messageSource.getMessage(messageKey, args, messageKey, I18nConfig.ES_419);
        return notificationRepository.save(new Notification(user, session, type, text));
    }

    @Transactional(readOnly = true)
    public List<Notification> findByUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public Notification markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .filter(n -> n.getUser().getId().equals(userId))
                .orElseThrow(() -> BusinessException.notFound("notification.not.found"));
        notification.markAsRead();
        return notification;
    }
}
