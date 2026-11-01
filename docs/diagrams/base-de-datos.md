# Modelo de base de datos (PostgreSQL, generado por JPA)

Tablas tal como las crea Hibernate a partir de las entidades. No existe la tabla `clinical_notes`:
el historial de citas (US14–US16) se consulta sobre `sessions` + `treatment_plans`.

```mermaid
erDiagram
    users {
        bigint id PK
        varchar email UK "120"
        varchar password_hash
        varchar role "20"
        integer failed_attempts
        timestamp locked_until "nullable"
        timestamp created_at
    }
    patients {
        bigint id PK
        bigint user_id FK,UK
        varchar full_name "100"
        varchar phone "15, nullable"
    }
    physiotherapists {
        bigint id PK
        bigint user_id FK,UK
        varchar full_name "100"
        varchar phone "15, nullable"
        varchar license_number UK "10"
        varchar specialty "30"
        integer years_of_experience
        text description "nullable"
        boolean active
    }
    availability_slots {
        bigint id PK
        bigint physiotherapist_id FK
        timestamp start_time
        timestamp end_time
        varchar status "10"
        bigint version "bloqueo optimista"
    }
    treatment_plans {
        bigint id PK
        bigint patient_id FK
        bigint physiotherapist_id FK
        varchar diagnosis
        integer total_sessions
        integer frequency_per_week
        varchar status "10"
        timestamp created_at
    }
    sessions {
        bigint id PK
        bigint treatment_plan_id FK
        bigint slot_id FK "nullable"
        integer session_number
        varchar status "20"
        boolean reminder_sent
    }
    notifications {
        bigint id PK
        bigint user_id FK
        bigint session_id FK "nullable"
        varchar type "30"
        varchar message "texto es-419 de respaldo"
        varchar message_key "100, nullable"
        varchar message_args "500, nullable"
        boolean is_read
        timestamp created_at
    }

    conversations {
        bigint id PK
        bigint user_id FK
        timestamp started_at
        timestamp last_activity_at
    }
    chat_messages {
        bigint id PK
        bigint conversation_id FK
        varchar sender "10"
        text content
        timestamp created_at
    }

    users ||--o| patients : "tiene perfil"
    users ||--o| physiotherapists : "tiene perfil"
    physiotherapists ||--o{ availability_slots : publica
    patients ||--o{ treatment_plans : sigue
    physiotherapists ||--o{ treatment_plans : dirige
    treatment_plans ||--|{ sessions : contiene
    availability_slots |o--o{ sessions : ocupa
    users ||--o{ notifications : recibe
    sessions |o--o{ notifications : genera
    users ||--o{ conversations : conversa
    conversations ||--o{ chat_messages : contiene
```

## Explicación

Cada entidad del diagrama de clases se convierte en una tabla con clave primaria autoincremental. Las
relaciones se implementan con claves foráneas: `patients` y `physiotherapists` apuntan a `users` (relación uno
a uno, garantizada con `UNIQUE`), `treatment_plans` une a un paciente con un fisioterapeuta, `sessions`
pertenece a un plan y ocupa un bloque de `availability_slots`, `notifications` se dirige a un usuario, y
`conversations` (con sus `chat_messages`) guarda el historial del asistente de cada usuario (US17–US20). Las
restricciones `UNIQUE` en `email` y `license_number` impiden cuentas y colegiaturas duplicadas, y la columna
`version` permite el bloqueo optimista de la reserva (US10).
