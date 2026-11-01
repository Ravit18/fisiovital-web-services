package com.healthdev.fisiovital.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Conversacion de un usuario con el asistente (US17-US20). Guarda el contexto para que el asistente
 * recuerde lo que ofrecio (por ejemplo, "el segundo" horario en US19). Tras 30 minutos sin actividad
 * se considera vencida y se inicia una nueva.
 */
@Entity
@Table(name = "conversations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Conversation {

    public static final long EXPIRATION_MINUTES = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "last_activity_at", nullable = false)
    private LocalDateTime lastActivityAt;

    public Conversation(User user, LocalDateTime now) {
        this.user = user;
        this.startedAt = now;
        this.lastActivityAt = now;
    }

    public boolean belongsTo(Long userId) {
        return user.getId().equals(userId);
    }

    public boolean isExpired(LocalDateTime now) {
        return lastActivityAt.plusMinutes(EXPIRATION_MINUTES).isBefore(now);
    }

    /** Registra un mensaje y actualiza la ultima actividad de la conversacion. */
    public ChatMessage addMessage(MessageSender sender, String content, LocalDateTime now) {
        this.lastActivityAt = now;
        return new ChatMessage(this, sender, content, now);
    }
}
