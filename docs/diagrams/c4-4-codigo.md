# C4 · Nivel 4 — Diagrama de Código (componente Sesiones, US11–US12)

Detalle de clases de un componente: **reprogramar y cancelar una sesión**. Muestra cómo cada capa
(controller → service → repository, con records `*RequestDTO` / `*ResponseDTO` y un Mapper de MapStruct) se implementa en el código real.
El modelo de dominio completo está en [clases.md](clases.md).

```mermaid
classDiagram
    direction TB

    namespace controller_dto {
        class SessionController {
            <<RestController /api/v1/sessions>>
            -SessionService sessionService
            +reschedule(sessionId, RescheduleSessionRequestDTO) ResponseEntity~SessionResponseDTO~
            +cancel(sessionId) ResponseEntity~SessionResponseDTO~
        }
        class RescheduleSessionRequestDTO {
            <<record · request>>
            +Long newSlotId
        }
        class SessionResponseDTO {
            <<record · response>>
            +Long id
            +Long planId
            +int sessionNumber
            +int totalSessions
            +String status
            +Long slotId
            +LocalDateTime startTime
            +LocalDateTime endTime
            +String patientName
            +String physiotherapistName
            +boolean lateChange
        }
    }

    namespace service {
        class SessionService {
            <<Service · Transactional>>
            -SessionRepository sessionRepository
            -AvailabilityService availabilityService
            -ProfileLookupService profileLookupService
            -NotificationService notificationService
            -SessionMapper sessionMapper
            +reschedule(sessionId, RescheduleSessionRequestDTO) SessionResponseDTO
            +cancel(sessionId) SessionResponseDTO
            +agenda(LocalDate) AgendaResponseDTO
        }
        class AvailabilityService {
            <<Service>>
            +getSlot(slotId) AvailabilitySlot
        }
        class NotificationService {
            <<Service>>
            +notify(User, Session, NotificationType, messageKey, args) Notification
        }
    }

    namespace mapper {
        class SessionMapper {
            <<interface · MapStruct Mapper>>
            +toResponse(Session, boolean lateChange) SessionResponseDTO
            +toBookedAgendaItem(AvailabilitySlot, Session) AgendaItemResponseDTO
        }
    }

    namespace repository {
        class SessionRepository {
            <<interface · JpaRepository>>
            +findFirstBySlotIdAndStatusInOrderByIdDesc(slotId, statuses) Optional~Session~
            +findPendingReminders(from, to) List~Session~
        }
    }

    namespace model {
        class Session {
            <<Entity>>
            -int sessionNumber
            -SessionStatus status
            +isLateChange(now) boolean
            +reschedule(newSlot, now) void
            +cancel(now) void
        }
        class AvailabilitySlot {
            <<Entity>>
            -Long version
            +belongsTo(physiotherapistId) boolean
            +book() void
            +release() void
        }
    }

    namespace shared {
        class BusinessException {
            <<RuntimeException>>
            +conflict(key, args)$ BusinessException
            +notFound(key, args)$ BusinessException
        }
        class GlobalExceptionHandler {
            <<RestControllerAdvice>>
        }
    }

    SessionController --> SessionService : delega
    SessionController ..> RescheduleSessionRequestDTO : recibe
    SessionController ..> SessionResponseDTO : responde
    SessionService --> SessionRepository : saveAndFlush
    SessionService --> AvailabilityService : nuevo bloque
    SessionService --> NotificationService : avisa al fisioterapeuta
    SessionService --> SessionMapper : Entity a ResponseDTO
    SessionMapper ..> SessionResponseDTO : genera
    SessionService ..> BusinessException : lanza
    SessionRepository ..> Session : persiste
    Session --> AvailabilitySlot : ocupa
    GlobalExceptionHandler ..> BusinessException : traduce a HTTP + i18n
```

## Secuencia de US11 (reprogramar)

1. `SessionController.reschedule` recibe `RescheduleSessionRequestDTO` y delega en `SessionService`.
2. `SessionService` verifica que la sesión sea del paciente autenticado y obtiene el nuevo bloque con
   `AvailabilityService.getSlot`. Si no es del mismo fisioterapeuta, no está libre o ya pasó, lanza
   `BusinessException.conflict` → **409** con mensaje i18n.
3. `Session.reschedule(newSlot, now)` valida que la sesión siga reservada y sea futura (si no, **422**),
   reserva el nuevo bloque y libera el anterior; `isLateChange(now)` marca el cambio tardío si faltan
   menos de 24 h.
4. `SessionRepository.saveAndFlush` guarda; si otro paciente tomó el bloque, `@Version` provoca
   un conflicto de concurrencia → **409**.
5. `NotificationService.notify` avisa al fisioterapeuta y `SessionMapper` (generado por MapStruct) convierte la
   entidad `Session` en `SessionResponseDTO` con `lateChange`.
