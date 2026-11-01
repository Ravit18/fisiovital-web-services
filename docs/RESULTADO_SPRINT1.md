# Resultado US01–US20 — checklist final

Verificado el 2026-11-01 sobre la arquitectura en capas (Controller → Service → Repository con DTOs y MapStruct).

## 1. Resultado de las pruebas

| Prueba | Resultado |
|---|---|
| `./mvnw test` (integración, H2 en modo PostgreSQL; Gemini simulado) | **85 tests, 0 fallos** |
| Postman / Newman (`postman/FisioVital.postman_collection.json`) contra la app en ejecución | **71/71 aserciones OK en es-419 y en en-US**, 101 peticiones, 0 fallos (US17–US20 contra Gemini real) |
| Rutas inexistentes (notas clínicas, progreso del plan) | 401 sin token / 404 con token. **Nunca 500** |

## 2. US01–US20 × 3 escenarios

Cada escenario tiene su petición en Postman (código HTTP + mensaje i18n) y su test de integración. Excepción: el
error de US17 "IA caída → 503" no se puede provocar contra Gemini real; Postman prueba en su lugar la conversación
de otro usuario (404) y el 503 queda cubierto por `AssistantControllerTest` y `AssistantControllerWithoutKeyTest`.

| US | Exitoso | Error | Alternativo | Test de integración | Estado |
|---|---|---|---|---|---|
| US01 Registro paciente | ✅ | ✅ | ✅ | `controller/AuthControllerTest` | Done |
| US02 Registro fisioterapeuta | ✅ | ✅ | ✅ | `controller/AuthControllerTest` | Done |
| US03 Inicio de sesión (bloqueo 5 intentos) | ✅ | ✅ | ✅ | `controller/AuthControllerTest` | Done |
| US04 Buscar por especialidad ("No hay especialistas disponibles") | ✅ | ✅ | ✅ | `controller/PhysiotherapistControllerTest` | Done (corregida) |
| US05 Perfil ("Sin disponibilidad por ahora") | ✅ | ✅ | ✅ | `controller/PhysiotherapistControllerTest` | Done (corregida) |
| US06 Horarios libres / siguiente semana | ✅ | ✅ | ✅ | `controller/AvailabilityControllerTest` | Done |
| US07 Publicar bloques (fecha pasada → 422) | ✅ | ✅ | ✅ | `controller/AvailabilityControllerTest` | Done |
| US08 Agenda diaria | ✅ | ✅ | ✅ | `controller/AgendaControllerTest` | Done |
| US09 Plan de tratamiento | ✅ | ✅ | ✅ | `controller/TreatmentPlanControllerTest` | Done |
| US10 Reserva con concurrencia (`@Version`) | ✅ | ✅ | ✅ | `controller/TreatmentPlanControllerTest` | Done |
| US11 Reprogramar (cambio tardío < 24 h) | ✅ | ✅ | ✅ | `controller/SessionControllerTest` | Done |
| US12 Cancelar (`LATE_CANCELLED` < 24 h) | ✅ | ✅ | ✅ | `controller/SessionControllerTest` | Done |
| US13 Recordatorio automático (correo Gmail + notificación) | ✅ | ✅ | ✅ | `controller/NotificationControllerTest` | Done (correo listo; faltan credenciales en `.env`) |
| US14 Historial paciente paginado (carga > 2 min → 503 "Por favor inténtalo más tarde") | ✅ | ✅ | ✅ | `controller/AppointmentHistoryControllerTest`, `exception/GlobalExceptionHandlerTest` | Done (corregida; T23 ahora sí implementada) |
| US15 Historial fisioterapeuta | ✅ | ✅ | ✅ | `controller/AppointmentHistoryControllerTest` | Done |
| US16 Historial de un paciente | ✅ | ✅ | ✅ | `controller/AppointmentHistoryControllerTest` | Done |
| US17 Consultar dudas al asistente (política de cancelación / IA caída → 503 / sin indicaciones médicas) | ✅ | ✅ | ✅ | `controller/AssistantControllerTest`, `controller/AssistantControllerWithoutKeyTest` | Done |
| US18 Recomendación de especialista (3 de columna / "Me duele" → pregunta / pecho → emergencias) | ✅ | ✅ | ✅ | `controller/AssistantControllerTest` | Done |
| US19 Reservar mediante el asistente (confirmar / día lleno → propone otro / "el segundo") | ✅ | ✅ | ✅ | `controller/AssistantControllerTest` | Done |
| US20 Reprogramar mediante el asistente (confirmar / ocupado → 2 cercanos / sin sesiones) | ✅ | ✅ | ✅ | `controller/AssistantControllerTest` | Done |

**20/20 historias completas.** US17–US20 además se probaron en vivo con Gemini real (`gemini-3.5-flash-lite`):
una conversación completa reservó el jueves a las 15:00 tras responder "el segundo" y luego la movió al viernes
a la misma hora, verificado en el historial (US14).

## 3. Tabla de endpoints real (reemplaza la Tabla 8 del informe)

| US | Método | Ruta | Rol |
|---|---|---|---|
| US01 | POST | `/api/v1/auth/sign-up/patient` | Público |
| US02 | POST | `/api/v1/auth/sign-up/physiotherapist` | Público |
| US03 | POST | `/api/v1/auth/sign-in` | Público |
| US04 | GET | `/api/v1/physiotherapists?specialty=` | Autenticado |
| US05 | GET | `/api/v1/physiotherapists/{id}` | Autenticado |
| US06 | GET | `/api/v1/physiotherapists/{id}/slots?from=&to=` | Autenticado |
| US07 | POST | `/api/v1/physiotherapists/me/slots` | PHYSIOTHERAPIST |
| US08 | GET | `/api/v1/physiotherapists/me/agenda?date=` | PHYSIOTHERAPIST |
| US09 | POST | `/api/v1/treatment-plans` | PATIENT |
| — | GET | `/api/v1/treatment-plans/me` | PATIENT |
| — | GET | `/api/v1/treatment-plans/{planId}` | PATIENT |
| — | PUT | `/api/v1/treatment-plans/{planId}` | PATIENT |
| — | DELETE | `/api/v1/treatment-plans/{planId}` | PATIENT |
| US10 | POST | `/api/v1/treatment-plans/{planId}/sessions` | PATIENT |
| US11 | PATCH | `/api/v1/sessions/{sessionId}/reschedule` | PATIENT |
| US12 | PATCH | `/api/v1/sessions/{sessionId}/cancel` | PATIENT |
| US13 | GET | `/api/v1/notifications` | Autenticado |
| — | PATCH | `/api/v1/notifications/{id}/read` | Autenticado |
| US14 | GET | `/api/v1/appointments/me?page=&size=` | PATIENT |
| US15 | GET | `/api/v1/appointments/physiotherapist/me` | PHYSIOTHERAPIST |
| US16 | GET | `/api/v1/appointments/physiotherapist/me?patientName=` | PHYSIOTHERAPIST |
| US17–US20 | POST | `/api/v1/assistant/messages` | Autenticado (reservar y reprogramar: PATIENT) |
