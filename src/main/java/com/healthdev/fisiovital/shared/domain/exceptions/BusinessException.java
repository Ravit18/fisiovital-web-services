package com.healthdev.fisiovital.shared.domain.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Excepcion de negocio. Lleva el codigo HTTP y la clave del mensaje i18n (messages_*.properties).
 */
@Getter
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String messageKey;
    private final transient Object[] args;

    public BusinessException(HttpStatus status, String messageKey, Object... args) {
        super(messageKey);
        this.status = status;
        this.messageKey = messageKey;
        this.args = args;
    }

    public static BusinessException badRequest(String key, Object... args) {
        return new BusinessException(HttpStatus.BAD_REQUEST, key, args);
    }

    public static BusinessException unauthorized(String key, Object... args) {
        return new BusinessException(HttpStatus.UNAUTHORIZED, key, args);
    }

    public static BusinessException forbidden(String key, Object... args) {
        return new BusinessException(HttpStatus.FORBIDDEN, key, args);
    }

    public static BusinessException notFound(String key, Object... args) {
        return new BusinessException(HttpStatus.NOT_FOUND, key, args);
    }

    public static BusinessException conflict(String key, Object... args) {
        return new BusinessException(HttpStatus.CONFLICT, key, args);
    }

    public static BusinessException locked(String key, Object... args) {
        return new BusinessException(HttpStatus.LOCKED, key, args);
    }

    public static BusinessException unprocessable(String key, Object... args) {
        return new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, key, args);
    }
}
