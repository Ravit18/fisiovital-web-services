package com.healthdev.fisiovital.service;

import com.healthdev.fisiovital.config.I18nConfig;
import com.healthdev.fisiovital.dto.NotificationResponseDTO;
import com.healthdev.fisiovital.exception.BusinessException;
import com.healthdev.fisiovital.mapper.NotificationMapper;
import com.healthdev.fisiovital.model.Notification;
import com.healthdev.fisiovital.model.NotificationType;
import com.healthdev.fisiovital.model.Session;
import com.healthdev.fisiovital.model.User;
import com.healthdev.fisiovital.repository.NotificationRepository;
import com.healthdev.fisiovital.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final CurrentUserProvider currentUser;
    private final MessageSource messageSource;

    /**
     * Crea una notificacion dentro de la plataforma. Guarda la clave y los argumentos para traducirla al
     * leer; los argumentos deben ser independientes del idioma (nombres, numeros, fechas dd/MM/yyyy).
     */
    @Transactional
    public Notification notify(User user, Session session, NotificationType type, String messageKey, Object... args) {
        String text = messageSource.getMessage(messageKey, args, messageKey, I18nConfig.ES_419);
        return notificationRepository.save(new Notification(user, session, type, messageKey, args, text));
    }

    /** US13 - Notificaciones del usuario autenticado, en el idioma de la peticion. */
    @Transactional(readOnly = true)
    public List<NotificationResponseDTO> findMine() {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(currentUser.get().userId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public NotificationResponseDTO markAsRead(Long notificationId) {
        Long userId = currentUser.get().userId();
        Notification notification = notificationRepository.findById(notificationId)
                .filter(n -> n.getUser().getId().equals(userId))
                .orElseThrow(() -> BusinessException.notFound("notification.not.found"));
        notification.markAsRead();
        return toResponse(notification);
    }

    private NotificationResponseDTO toResponse(Notification n) {
        String text = n.isTranslatable()
                ? messageSource.getMessage(n.getMessageKey(), n.getArgs(), n.getMessage(), LocaleContextHolder.getLocale())
                : n.getMessage();
        return notificationMapper.toResponse(n, text);
    }
}
