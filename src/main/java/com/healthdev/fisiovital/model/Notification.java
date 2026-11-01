package com.healthdev.fisiovital.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * Aviso dentro de la plataforma. Guarda la clave i18n y sus argumentos para traducirlo al idioma de quien
 * lo lee; message conserva el texto en es-419 (y es el unico dato de las notificaciones anteriores a la clave).
 */
@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    /** Separador de argumentos (Unit Separator): no aparece en nombres ni fechas. */
    private static final String ARGS_SEPARATOR = "\u001F";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private Session session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false)
    private String message;

    @Column(name = "message_key", length = 100)
    private String messageKey;

    @Column(name = "message_args", length = 500)
    private String messageArgs;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Notification(User user, Session session, NotificationType type, String messageKey, Object[] args,
                        String message) {
        this.user = user;
        this.session = session;
        this.type = type;
        this.messageKey = messageKey;
        this.messageArgs = String.join(ARGS_SEPARATOR, Arrays.stream(args).map(String::valueOf).toList());
        this.message = message;
        this.read = false;
        this.createdAt = LocalDateTime.now();
    }

    public boolean isTranslatable() {
        return messageKey != null;
    }

    public Object[] getArgs() {
        return messageArgs == null || messageArgs.isEmpty() ? new Object[0] : messageArgs.split(ARGS_SEPARATOR, -1);
    }

    public void markAsRead() {
        this.read = true;
    }
}
