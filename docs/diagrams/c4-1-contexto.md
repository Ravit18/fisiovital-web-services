# C4 · Nivel 1 — Diagrama de Contexto

```mermaid
flowchart TB
    patient["👤 <b>Paciente</b><br/><i>[Persona]</i><br/>Busca especialistas, sigue un plan de<br/>tratamiento y reserva, reprograma o<br/>cancela sus sesiones"]
    physio["👤 <b>Fisioterapeuta</b><br/><i>[Persona]</i><br/>Publica su disponibilidad, revisa su<br/>agenda diaria y el historial de citas"]

    fisiovital["<b>FisioVital</b><br/><i>[Sistema de software]</i><br/>Plataforma de gestión de centros de<br/>fisioterapia: Landing Page, Web App y<br/>Web Services para especialistas, horarios,<br/>planes, sesiones, recordatorios e historial"]

    smtp["<b>Servidor de correo SMTP</b><br/><i>[Sistema externo]</i><br/>Envía recordatorios por e-mail<br/>(opcional, p. ej. Gmail)"]
    gemini["<b>Google Gemini API</b><br/><i>[Sistema externo]</i><br/>Servicio de IA de terceros<br/>para el asistente (US17–US20)"]

    patient -- "Conoce el producto, se registra y<br/>gestiona sus sesiones [HTTPS]" --> fisiovital
    physio -- "Publica horarios y consulta<br/>agenda e historial [HTTPS]" --> fisiovital
    fisiovital -- "Envía recordatorios<br/>24 h antes [SMTP]" --> smtp
    smtp -. "Correo de recordatorio" .-> patient
    fisiovital -- "Consultas del asistente<br/>[HTTPS/JSON]" --> gemini

    classDef person fill:#08427b,stroke:#052e56,color:#ffffff
    classDef system fill:#1168bd,stroke:#0b4884,color:#ffffff
    classDef external fill:#999999,stroke:#6b6b6b,color:#ffffff
    class patient,physio person
    class fisiovital system
    class smtp external
    class gemini external
```

**Leyenda:** azul oscuro = persona · azul = sistema en alcance · gris = sistema externo.

## Explicación

El diagrama muestra a **FisioVital como una caja negra** y delimita qué queda dentro y fuera del sistema.
Sus dos actores son el **paciente**, que busca especialistas por especialidad, sigue un plan de tratamiento
y reserva, reprograma o cancela sesiones, y el **fisioterapeuta**, que publica bloques de disponibilidad y
consulta su agenda diaria y el historial de citas de sus pacientes. Fuera del sistema están el **servidor
SMTP**, que se usa de forma opcional para enviar por correo los recordatorios 24 horas antes de cada sesión,
y la **API de Google Gemini**, el servicio de IA de terceros con el que el asistente resuelve dudas, recomienda
especialistas y reserva o reprograma sesiones (US17–US20).
