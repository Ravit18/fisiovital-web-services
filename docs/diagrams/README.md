# Diagramas de FisioVital

Todos están en Mermaid, así que GitHub los muestra directamente. Para exportarlos como imagen, copia el
bloque `mermaid` en https://mermaid.live y descarga PNG o SVG.

## Modelo C4

| Nivel | Diagrama | Qué muestra |
|---|---|---|
| 1 | [Contexto](c4-1-contexto.md) | Paciente, fisioterapeuta, FisioVital y los sistemas externos (SMTP, Gemini) |
| 2 | [Contenedores](c4-2-contenedores.md) | Web App (Angular), Web Services (Spring Boot) y PostgreSQL |
| 3 | [Componentes](c4-3-componentes.md) | Capas controller → service (+ mapper) → repository, agrupadas por módulo funcional |
| 4 | [Código](c4-4-codigo.md) | Clases del componente Sesiones (US11–US12), de controller a entidad |

## Diseño de datos

- [Diagrama de clases del dominio](clases.md): entidades, enums y trazabilidad con US01–US16.
- [Modelo de base de datos](base-de-datos.md): tablas, PK, FK y UNIQUE generadas por JPA.
