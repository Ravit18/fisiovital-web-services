package com.healthdev.fisiovital.mapper;

import com.healthdev.fisiovital.dto.response.NotificationResponseDTO;
import com.healthdev.fisiovital.model.Notification;
import java.time.LocalDateTime;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-11-22T12:47:07-0500",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.12.1 (Eclipse Adoptium)"
)
@Component
public class NotificationMapperImpl implements NotificationMapper {

    @Override
    public NotificationResponseDTO toResponse(Notification notification, String translatedMessage) {
        if ( notification == null && translatedMessage == null ) {
            return null;
        }

        Long id = null;
        String type = null;
        boolean read = false;
        LocalDateTime createdAt = null;
        if ( notification != null ) {
            id = notification.getId();
            if ( notification.getType() != null ) {
                type = notification.getType().name();
            }
            read = notification.isRead();
            createdAt = notification.getCreatedAt();
        }
        String message = null;
        message = translatedMessage;

        NotificationResponseDTO notificationResponseDTO = new NotificationResponseDTO( id, type, message, read, createdAt );

        return notificationResponseDTO;
    }
}
