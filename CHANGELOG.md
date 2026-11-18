# Changelog

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y [SemVer](https://semver.org/lang/es/).

## [Unreleased]

### Changed
- Arquitectura en capas según el curso: una carpeta por capa (`controller/`, `service/`, `mapper/`, `repository/`,
  `model/`, `dto/`, además de `security/`, `config/` y `exception/`). Los controllers reciben `*RequestDTO` y
  devuelven `*ResponseDTO`; los services convierten con mappers de MapStruct y la API nunca expone entidades.
- `src/test` sigue la misma estructura: `controller/<Módulo>ControllerTest` y `exception/GlobalExceptionHandlerTest`
  (85 tests).
- CORS: en local se acepta cualquier puerto de `localhost`; en la nube se fijan los dominios con
  `CORS_ALLOWED_ORIGINS`.
- Diagrama C4 de componentes y documentación actualizados a la arquitectura en capas.

### Fixed
- US16: buscar un paciente por nombre devolvía también sus sesiones con otros fisioterapeutas. Ahora solo muestra
  las del fisioterapeuta que consulta (test de regresión incluido).

### Added
- Ver, editar y eliminar un plan de tratamiento: `GET`, `PUT` y `DELETE /api/v1/treatment-plans/{planId}`
  (9 tests).
- `scripts/seed-demo.ps1`: carga por la API fisioterapeutas, pacientes, horarios y citas de demostración para la
  Web App.
- Asistente conversacional con IA para US17–US20 (`POST /api/v1/assistant/messages`) con Spring AI 1.1 y
  Google Gemini (`gemini-3.5-flash-lite`, plan gratuito): resuelve dudas, recomienda especialistas, reserva y
  reprograma sesiones mediante tool calling sobre los services existentes.
- Entidades `Conversation` y `ChatMessage` (tablas `conversations` y `chat_messages`) para el contexto del chat.
- Respuestas fijas de seguridad: emergencias, sin indicaciones médicas y molestias sin detalle.
- Envío de recordatorios por Gmail configurado con `MAIL_USERNAME` y `MAIL_PASSWORD` (US13).
- 17 tests del asistente (73 en total) y 5 escenarios nuevos en Postman (53 en total).
- Postman: US17 (error: conversación ajena), US18 (exitoso), US19 y US20 completas contra Gemini real (71 aserciones,
  101 peticiones). Se verifican en la agenda del fisioterapeuta, no en el texto de Gemini. La variable `aiDelayMs`
  (20 s) respeta el límite de 15 peticiones por minuto del plan gratuito.
- Maven Wrapper (`mvnw`, Maven 3.9.16): basta con tener JDK 21 o superior para compilar y probar.

### Changed
- Spring Boot 3.3.5 → 3.5.15 y springdoc 2.6.0 → 2.8.17 (requisito de Spring AI 1.1).
- US14/US15: el mensaje de carga lenta es "Por favor inténtalo más tarde", como pide el informe.
- Los tests ya no dependen del `.env`: el profile `test` fija una key de Gemini ficticia y no envía correos.

### Fixed
- US13: las notificaciones se muestran en el idioma de quien las lee (`Accept-Language`); antes se guardaban
  siempre en es-419. `notifications` guarda `message_key` y `message_args`; las filas antiguas usan `message`.
  El recordatorio separa fecha y hora para que el inglés diga "on 21/11/2026 at 10:00". 2 tests nuevos (75 en total).
- US11/US12 (test y Postman): los escenarios de cambio tardío fallaban entre las 19:00 y las 22:00 porque los
  bloques cruzaban la medianoche; ahora empiezan a las 00:00 del día siguiente.

## [1.0.0] - 2026-10-21

Cierre del Sprint 1 (US01–US16) con las correcciones de la retroalimentación del TB2.

### Added
- Historial de citas paginado del paciente (US14) y del fisioterapeuta, con búsqueda por paciente (US15–US16)
  en `/api/v1/appointments/me` y `/api/v1/appointments/physiotherapist/me`.
- Mensaje "No hay especialistas disponibles" en la búsqueda vacía (US04) y "Sin disponibilidad por ahora"
  en el perfil (US05).
- Timeout de consulta configurable (`QUERY_TIMEOUT_MS`): una carga de más de 2 minutos responde 503 (US14).
- Suite de 56 tests de integración (H2) y colección de Postman con 48 escenarios y aserciones.
- CORS configurable para Angular (`CORS_ALLOWED_ORIGINS`) e intervalo del recordatorio configurable
  (`REMINDER_FIXED_DELAY_MS`).
- Ejemplos de request/response en Swagger y carga de secretos locales desde `.env`.
- Diagramas Mermaid en `docs/diagrams/`: modelo C4 (contexto, contenedores, componentes y código), clases y base de datos.

### Changed
- Fecha pasada al publicar bloques responde 422 (US07).
- La búsqueda de especialistas pasa del controller a `PhysiotherapistSearchService`.

### Fixed
- Rutas inexistentes, métodos no permitidos y conflictos de datos responden 404, 405 y 409 en lugar de 500.

### Removed
- Notas clínicas y progreso del plan (no corresponden a ninguna historia de usuario).
