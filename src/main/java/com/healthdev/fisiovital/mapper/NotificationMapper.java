package com.healthdev.fisiovital.mapper;

import com.healthdev.fisiovital.dto.NotificationResponseDTO;
import com.healthdev.fisiovital.model.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapStructConfig.class)
public interface NotificationMapper {

    /** translatedMessage es el texto ya traducido al idioma de la peticion (US13). */
    @Mapping(target = "message", source = "translatedMessage")
    NotificationResponseDTO toResponse(Notification notification, String translatedMessage);
}
