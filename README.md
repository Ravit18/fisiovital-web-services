# FisioVital Web Services (Sprint 1)

API RESTful de FisioVital, construida con Spring Boot 3.3, Java 21, Spring Data JPA, Spring Security + JWT,
PostgreSQL y Swagger (OpenAPI). Cubre las User Stories US01 a US16.

## Requisitos

- JDK 21
- IntelliJ IDEA (Community o Ultimate)
- PostgreSQL 14 o superior (instalado localmente o con Docker)
- Postman

## 1. Crear la base de datos

**Opción A — PostgreSQL instalado (pgAdmin):** crea una base de datos llamada `fisiovital`.
El usuario por defecto es `postgres` con contraseña `postgres`. Si tu contraseña es otra, cámbiala en
`src/main/resources/application.properties` (`spring.datasource.password`) o define la variable de entorno `DB_PASSWORD`.

**Opción B — Docker:**

```bash
docker compose up -d postgres
```

Las tablas se crean solas al iniciar la aplicación (`spring.jpa.hibernate.ddl-auto=update`).

## 2. Abrir y ejecutar en IntelliJ IDEA

1. *File → Open* y selecciona la carpeta `fisiovital-web-services` (la que contiene `pom.xml`).
2. Espera a que IntelliJ descargue las dependencias de Maven (barra inferior).
3. Verifica el JDK: *File → Project Structure → Project SDK = 21*.
4. Si IntelliJ lo sugiere, habilita *Annotation Processing* (necesario para Lombok).
5. Ejecuta `FisioVitalApplication` (botón ▶ junto al método `main`).
6. La API queda en `http://localhost:8080` y la documentación en **http://localhost:8080/swagger-ui.html**.

## 3. Probar con Postman

1. *Import* → selecciona `postman/FisioVital.postman_collection.json`.
2. Abre la colección → *Run* (Collection Runner) → *Run FisioVital API - Sprint 1*.
3. Las peticiones se ejecutan en orden y guardan solas los tokens y los IDs (paciente, fisioterapeuta, bloques, plan, sesión).
4. Cada corrida genera usuarios nuevos, así que puedes repetirla las veces que quieras.

Para cambiar el idioma de las respuestas, cambia la variable `lang` a `en-US` (por defecto `es-419`).

## Endpoints

| US | Método | Endpoint | Rol |
|---|---|---|---|
| US01 | POST | `/api/v1/auth/sign-up/patient` | Público |
| US02 | POST | `/api/v1/auth/sign-up/physiotherapist` | Público |
| US03 | POST | `/api/v1/auth/sign-in` | Público |
| US04 | GET | `/api/v1/physiotherapists?specialty=DEPORTIVA` | Autenticado |
| US05 | GET | `/api/v1/physiotherapists/{id}` | Autenticado |
| US06 | GET | `/api/v1/physiotherapists/{id}/slots?from=&to=` | Autenticado |
| US07 | POST | `/api/v1/physiotherapists/me/slots` | Fisioterapeuta |
| US08 | GET | `/api/v1/physiotherapists/me/agenda?date=` | Fisioterapeuta |
| US09 | POST | `/api/v1/treatment-plans` | Paciente |
| — | GET | `/api/v1/treatment-plans/me` | Paciente |
| US10 | POST | `/api/v1/treatment-plans/{planId}/sessions` | Paciente |
| US11 | PATCH | `/api/v1/sessions/{sessionId}/reschedule` | Paciente |
| US12 | PATCH | `/api/v1/sessions/{sessionId}/cancel` | Paciente |
| US13 | GET | `/api/v1/notifications` | Autenticado |
| US14 | GET | `/api/v1/patients/me/appointments` | Paciente |
| US15 | GET | `/api/v1/physiotherapists/me/appointments` | Fisioterapeuta |
| US16 | GET | `/api/v1/patients/appointments?fullName=` | Fisioterapeuta |

Especialidades válidas: `DEPORTIVA`, `COLUMNA_POSTURA`, `GERIATRICA`, `PEDIATRICA`, `POSTQUIRURGICA`, `NEUROLOGICA`.

## Arquitectura (bounded contexts, DDD)

```
com.healthdev.fisiovital
├── iam            Usuarios, registro, inicio de sesión, JWT y Spring Security (US01-US03)
├── profiles       Pacientes y fisioterapeutas, búsqueda de especialistas (US04-US05)
├── scheduling     Bloques de disponibilidad (US06-US07)
├── treatment      Planes, sesiones y agenda (US08-US12)
├── appointments   Historial de citas del paciente y del fisioterapeuta (US14-US16)
├── notifications  Notificaciones y recordatorios programados (US13)
└── shared         Excepciones, manejo global de errores, i18n y Swagger
```

Cada contexto se organiza en capas: `domain/model` (entidades con reglas de negocio),
`infrastructure/persistence` (repositorios JPA), `application` (servicios) e `interfaces/rest` (controladores y resources).

## Recordatorios por correo (opcional)

Cada 10 minutos se envían recordatorios de las sesiones que empiezan en las próximas 24 horas. Sin configuración
de correo, quedan como notificación en la plataforma (`GET /api/v1/notifications`). Para enviarlos también por
e-mail, descomenta las líneas `spring.mail.*` en `application.properties` y usa una contraseña de aplicación de Gmail.

## Docker

```bash
docker compose --profile full up --build
```

Levanta PostgreSQL y la API juntas.
