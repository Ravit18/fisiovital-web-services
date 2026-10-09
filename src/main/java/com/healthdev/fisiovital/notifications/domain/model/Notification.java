package com.healthdev.fisiovital.notifications.domain.model;

import com.healthdev.fisiovital.iam.domain.model.User;
import com.healthdev.fisiovital.treatment.domain.model.Session;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

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

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Notification(User user, Session session, NotificationType type, String message) {
        this.user = user;
        this.session = session;
        this.type = type;
        this.message = message;
        this.read = false;
        this.createdAt = LocalDateTime.now();
    }

    public void markAsRead() {
        this.read = true;
    }
}
