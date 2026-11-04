package com.healthdev.fisiovital.exception;

import com.healthdev.fisiovital.dto.response.ErrorResponseDTO;
import jakarta.persistence.QueryTimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    private String msg(String key, Object... args) {
        return messageSource.getMessage(key, args, key, LocaleContextHolder.getLocale());
    }

    private ResponseEntity<ErrorResponseDTO> build(HttpStatus status, String message, List<String> details) {
        return ResponseEntity.status(status)
                .body(ErrorResponseDTO.of(status.value(), status.getReasonPhrase(), message, details));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponseDTO> handleBusiness(BusinessException ex) {
        return build(ex.getStatus(), msg(ex.getMessageKey(), ex.getArgs()), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        String message = details.size() == 1
                ? ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage()
                : msg("validation.failed");
        return build(HttpStatus.BAD_REQUEST, message, details);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorResponseDTO> handleMalformed(Exception ex) {
        return build(HttpStatus.BAD_REQUEST, msg("request.malformed"), List.of(ex.getMessage()));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponseDTO> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        return build(HttpStatus.CONFLICT, msg("slot.just.booked"), List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, msg("auth.forbidden"), List.of());
    }

    /** Ruta inexistente -> 404 (antes caia al catch-all y respondia 500). */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNoResource(NoResourceFoundException ex) {
        return build(HttpStatus.NOT_FOUND, msg("error.not.found"), List.of());
    }

    /** Metodo HTTP no soportado por la ruta -> 405. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, msg("error.method.not.allowed"), List.of(ex.getMethod()));
    }

    /** Restriccion de integridad (p. ej. unico duplicado no cubierto por regla de negocio) -> 409. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleDataIntegrity(DataIntegrityViolationException ex) {
        return build(HttpStatus.CONFLICT, msg("error.conflict"), List.of());
    }

    /** US14 - Consulta que supera el timeout configurado -> 503 con mensaje i18n. */
    @ExceptionHandler({QueryTimeoutException.class, TransactionTimedOutException.class})
    public ResponseEntity<ErrorResponseDTO> handleTimeout(Exception ex) {
        log.warn("Consulta tardia / timeout: {}", ex.getMessage());
        return build(HttpStatus.SERVICE_UNAVAILABLE, msg("appointments.slow"), List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, msg("error.internal"), List.of());
    }
}
