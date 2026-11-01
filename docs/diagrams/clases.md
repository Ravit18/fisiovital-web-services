# Diagrama de clases del dominio (Sprint 1)

Entidades JPA reales de `model/`, sin `ClinicalNote` (no corresponde a ninguna historia).
US14–US16 (historial de citas) se resuelven sobre `Session`, sin entidad propia.

```mermaid
classDiagram
    direction LR

    class User {
        -Long id
        -String email
        -String passwordHash
        -Role role
        -int failedAttempts
        -LocalDateTime lockedUntil
        -LocalDateTime createdAt
        +registerFailedAttempt() void
        +resetFailedAttempts() void
        +isLocked() boolean
    }
    class Role {
        <<enumeration>>
        PATIENT
        PHYSIOTHERAPIST
    }

    class Patient {
        -Long id
        -String fullName
        -String phone
    }
    class Physiotherapist {
        -Long id
        -String fullName
        -String phone
        -String licenseNumber
        -Specialty specialty
        -int yearsOfExperience
        -String description
        -boolean active
    }
    class Specialty {
        <<enumeration>>
        DEPORTIVA
        COLUMNA_POSTURA
        GERIATRICA
        PEDIATRICA
        POSTQUIRURGICA
        NEUROLOGICA
    }

    class AvailabilitySlot {
        -Long id
        -LocalDateTime startTime
        -LocalDateTime endTime
        -SlotStatus status
        -Long version
        +isFree() boolean
        +belongsTo(physiotherapistId) boolean
        +book() void
        +release() void
    }
    class SlotStatus {
        <<enumeration>>
        FREE
        BOOKED
    }

    class TreatmentPlan {
        -Long id
        -String diagnosis
        -int totalSessions
        -int frequencyPerWeek
        -PlanStatus status
        -LocalDateTime createdAt
        +isActive() boolean
        +belongsToPatient(patientId) boolean
        +canBookMore(activeSessions) boolean
        +finish() void
    }
    class PlanStatus {
        <<enumeration>>
        ACTIVE
        FINISHED
    }

    class Session {
        -Long id
        -int sessionNumber
        -SessionStatus status
        -boolean reminderSent
        +isReserved() boolean
        +isLateChange(now) boolean
        +reschedule(newSlot, now) void
        +cancel(now) void
        +statusAt(now) SessionStatus
        +markReminderSent() void
    }
    class SessionStatus {
        <<enumeration>>
        RESERVED
        COMPLETED
        CANCELLED
        LATE_CANCELLED
    }

    class Notification {
        -Long id
        -NotificationType type
        -String message
        -String messageKey
        -String messageArgs
        -boolean read
        -LocalDateTime createdAt
        +isTranslatable() boolean
        +getArgs() Object[]
        +markAsRead() void
    }
    class NotificationType {
        <<enumeration>>
        SESSION_REMINDER
        SESSION_RESCHEDULED
        SESSION_CANCELLED
    }

    class Conversation {
        -Long id
        -LocalDateTime startedAt
        -LocalDateTime lastActivityAt
        +belongsTo(userId) boolean
        +isExpired(now) boolean
        +addMessage(sender, content, now) ChatMessage
    }
    class ChatMessage {
        -Long id
        -MessageSender sender
        -String content
        -LocalDateTime createdAt
    }
    class MessageSender {
        <<enumeration>>
        USER
        ASSISTANT
    }

    User "1" -- "0..1" Patient : cuenta
    User "1" -- "0..1" Physiotherapist : cuenta
    Physiotherapist "1" -- "0..*" AvailabilitySlot : publica
    Patient "1" -- "0..*" TreatmentPlan : sigue
    Physiotherapist "1" -- "0..*" TreatmentPlan : dirige
    TreatmentPlan "1" *-- "1..*" Session : contiene
    Session "0..*" --> "0..1" AvailabilitySlot : ocupa
    User "1" -- "0..*" Notification : recibe
    Notification "0..*" --> "0..1" Session : sobre
    User "1" -- "0..*" Conversation : conversa
    Conversation "1" *-- "0..*" ChatMessage : contiene

    User ..> Role
    Physiotherapist ..> Specialty
    AvailabilitySlot ..> SlotStatus
    TreatmentPlan ..> PlanStatus
    Session ..> SessionStatus
    Notification ..> NotificationType
    ChatMessage ..> MessageSender
```

## Explicación

El dominio tiene nueve entidades. `User` guarda la cuenta y el rol, y se asocia a un perfil de
`Patient` o de `Physiotherapist`. El fisioterapeuta publica `AvailabilitySlot` (bloques de 60 minutos con
`@Version` para evitar dobles reservas), y paciente y fisioterapeuta se vinculan mediante un `TreatmentPlan`
que contiene sus `Session`; cada sesión ocupa un bloque. `Notification` registra recordatorios y avisos de
cambios, y `Conversation` con sus `ChatMessage` guarda el contexto del asistente con IA (US17–US20). Las reglas de negocio están dentro de las entidades (por ejemplo, `Session.isLateChange` aplica la
regla de 24 horas).

## Trazabilidad con las historias

| Clase | Historias | Regla de negocio |
|---|---|---|
| `User` | US01–US03 | Registro, login con JWT y bloqueo tras 5 intentos fallidos |
| `Physiotherapist` | US02, US04–US05 | Búsqueda por especialidad, ordenada por años de experiencia |
| `AvailabilitySlot` | US06–US07, US10 | Bloques de 60 min sin cruces; `@Version` evita la doble reserva |
| `TreatmentPlan` | US09–US10 | Un solo plan activo por especialista; no admite 0 sesiones |
| `Session` | US08, US10–US12, US14–US16 | Reservar, reprogramar y cancelar; cambio tardío si faltan menos de 24 h |
| `Notification` | US13 | Recordatorio automático (`@Scheduled`), por correo y en la plataforma, y avisos de cambios |
| `Conversation` | US17–US20 | Contexto del asistente; vence tras 30 minutos sin actividad (`isExpired`) |
| `ChatMessage` | US17–US20 | Mensajes del usuario y del asistente; permiten responder "el segundo" sin repetir la fecha (US19) |
