package com.healthdev.fisiovital.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Conversacion de un usuario con el asistente (US17-US20). Guarda el contexto para que el asistente
 * recuerde lo que ofrecio (por ejemplo, "el segundo" horario en US19). El usuario puede retomarla desde su
 * historial de chats; el titulo sale de su primer mensaje.
 */
@Entity
@Table(name = "conversations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Conversation {

    public static final int TITLE_LENGTH = 60;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = TITLE_LENGTH + 1)
    private String title;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "last_activity_at", nullable = false)
    private LocalDateTime lastActivityAt;

    public Conversation(User user, String firstMessage, LocalDateTime now) {
        this.user = user;
        this.title = titleOf(firstMessage);
        this.startedAt = now;
        this.lastActivityAt = now;
    }

    public boolean belongsTo(Long userId) {
        return user.getId().equals(userId);
    }

    /** Primer mensaje en una linea, cortado en la ultima palabra completa antes de {@value #TITLE_LENGTH}. */
    public static String titleOf(String message) {
        String line = message == null ? "" : message.strip().replaceAll("\\s+", " ");
        if (line.length() <= TITLE_LENGTH) {
            return line;
        }
        int cut = line.lastIndexOf(' ', TITLE_LENGTH);
        return line.substring(0, cut > TITLE_LENGTH / 2 ? cut : TITLE_LENGTH).stripTrailing() + "…";
    }

    /** Registra un mensaje y actualiza la ultima actividad de la conversacion. */
    public ChatMessage addMessage(MessageSender sender, String content, LocalDateTime now) {
        this.lastActivityAt = now;
        return new ChatMessage(this, sender, content, now);
    }
}
