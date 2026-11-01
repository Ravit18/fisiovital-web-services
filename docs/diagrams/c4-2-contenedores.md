# C4 · Nivel 2 — Diagrama de Contenedores

```mermaid
flowchart TB
    patient["👤 <b>Paciente</b><br/><i>[Persona]</i>"]
    physio["👤 <b>Fisioterapeuta</b><br/><i>[Persona]</i>"]

    subgraph fisiovital["FisioVital [Sistema de software]"]
        landing["<b>Landing Page</b><br/><i>[Contenedor: HTML5, CSS3, JavaScript · Sprint 2]</i><br/>Presenta la propuesta de valor y<br/>dirige al registro e inicio de sesión"]
        spa["<b>Web App</b><br/><i>[Contenedor: Angular, TypeScript,<br/>Angular Material · Sprint 2]</i><br/>Interfaz responsive del paciente<br/>y del fisioterapeuta"]
        api["<b>Web Services</b><br/><i>[Contenedor: Spring Boot 3.5 · Java 21]</i><br/>API REST /api/v1: JWT y roles, reglas de<br/>negocio, i18n es-419/en-US, recordatorios<br/>@Scheduled, asistente con Spring AI y Swagger"]
        db[("<b>Base de datos</b><br/><i>[Contenedor: PostgreSQL 16]</i><br/>Usuarios, perfiles, bloques, planes,<br/>sesiones, notificaciones y conversaciones")]
    end

    smtp["<b>Servidor SMTP</b><br/><i>[Sistema externo]</i>"]
    gemini["<b>Google Gemini API</b><br/><i>[Sistema externo]</i>"]

    patient -- "Visita [HTTPS]" --> landing
    physio -- "Visita [HTTPS]" --> landing
    landing -- "Registrarse / Iniciar sesión" --> spa
    patient -- "Usa [HTTPS]" --> spa
    physio -- "Usa [HTTPS]" --> spa
    spa -- "Llama a la API<br/>[JSON/HTTPS + Bearer JWT]" --> api
    api -- "Lee y escribe<br/>[JDBC · JPA/Hibernate]" --> db
    api -- "Envía recordatorios<br/>[SMTP, opcional]" --> smtp
    api -- "Asistente US17–US20<br/>[Spring AI · HTTPS/JSON]" --> gemini

    classDef person fill:#08427b,stroke:#052e56,color:#ffffff
    classDef container fill:#438dd5,stroke:#2e6295,color:#ffffff
    classDef planned fill:#438dd5,stroke:#2e6295,color:#ffffff,stroke-dasharray:5 5
    classDef external fill:#999999,stroke:#6b6b6b,color:#ffffff
    class patient,physio person
    class api,db container
    class landing,spa planned
    class smtp external
    class gemini external
    style fisiovital fill:none,stroke:#1168bd,stroke-width:2px,stroke-dasharray:6 4
```

**Leyenda:** azul oscuro = persona · azul claro = contenedor · gris = sistema externo ·
borde punteado = planificado (Landing Page y Web App en el Sprint 2).

## Explicación

FisioVital se compone de cuatro contenedores. El **Landing Page** (HTML5, CSS3 y JavaScript) presenta el
producto y lleva al registro, y la **Web App** en Angular con Angular Material es la interfaz responsive
donde pacientes y fisioterapeutas trabajan a diario; ambos se construyen en el Sprint 2 (TB3). Los
**Web Services** en Spring Boot exponen la API REST `/api/v1`, protegida con Spring Security y JWT por roles,
concentran las reglas de negocio (reserva con control de concurrencia, regla de 24 horas, recordatorios
programados) y responden en español e inglés. La **base de datos PostgreSQL** guarda la información mediante
Spring Data JPA. Como servicios externos, el backend usa un servidor **SMTP** (Gmail) para los recordatorios y
la **API de Gemini** mediante Spring AI para el asistente conversacional (US17–US20). Web Services y
PostgreSQL se levantan juntos con Docker Compose.
