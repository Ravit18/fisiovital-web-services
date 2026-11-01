# Checklist de entrega TB2 (Semana 7) — según rúbrica y enunciado

Fuente: `enunciado/Trabajo_1ACC0236-202620.pdf` y `enunciado/Rubrica_TB2_1ACC0236_202620.xlsx`.
Leyenda: ✅ listo en el repositorio · 📝 lo completa el equipo en el informe · ⏳ requiere una acción pendiente.

**La TB2 cubre:** cierre del Capítulo III (3.1 Diseño Técnico), Capítulo IV (4.1 y 4.2, Sprint 1),
Conclusiones y Recomendaciones de TB2, Bibliografía y **Web Services v1 funcional**.

## Sección 1 · Proyecto (8 pts)

### Criterio 1 · Diseño Técnico (2.5 pts)

| Exigencia | Estado | Dónde |
|---|---|---|
| Diagrama de **Contexto** C4 + explicación de 3 o más líneas | ✅ | [c4-1-contexto.md](diagrams/c4-1-contexto.md) |
| Diagrama de **Contenedores** C4 (frontend, backend, BD, servicios externos) + explicación | ✅ | [c4-2-contenedores.md](diagrams/c4-2-contenedores.md) |
| Diagrama de Componentes (opcional, recomendado por el profesor en el feedback) | ✅ | [c4-3-componentes.md](diagrams/c4-3-componentes.md) |
| Diagrama de Clases alineado a las US (sin `ClinicalNote`) | ✅ | [clases.md](diagrams/clases.md) |
| Diseño de Base de Datos coherente con las clases | ✅ | [base-de-datos.md](diagrams/base-de-datos.md) |
| Pegar capturas y explicaciones en el informe (3.1.1, 3.1.2, 3.1.3) | 📝 | Exportar desde https://mermaid.live |

> El enunciado sugiere Structurizr o LucidChart, o diagram-as-code con Structurizr DSL o PlantUML. Los
> diagramas están en Mermaid, que también es diagram-as-code. Si el profesor exige una de esas herramientas,
> se pueden pasar a Structurizr DSL.

### Criterio 2 · Gestión de Configuración (1.5 pts)

| Exigencia | Estado |
|---|---|
| Tabla de herramientas por categoría (Project Management, Requirements Management, Product Design, Software Development, Software Documentation) | 📝 Ya estaba completa en TB1 (el profesor la elogió) |
| GitFlow, SemVer y Conventional Commits | ✅ Se aplican en el repositorio (ramas `feature/*`, commits `feat(...)`, `CHANGELOG.md` 1.0.0) |
| Tabla de repositorios: URL, rama `main`, rama `develop` y **último release** de Web Services | ⏳ Falta publicar `v1.0.0` en el repositorio oficial |

Tabla para el informe (completar el Landing Page cuando exista):

| Producto | Repositorio GitHub | Rama main | Rama develop | Último release |
|---|---|---|---|---|
| Landing Page | (Sprint 2) | main | develop | — |
| Web Services | https://github.com/Ravit18/fisiovital-web-services | main | develop | v1.0.0 |

### Criterio 3 · Sprint 1, proceso ágil (1.5 pts)

| Exigencia | Estado |
|---|---|
| Sprint Planning 1: fecha, participantes, Sprint Goal, Velocity, Story Points (**47**) y evidencia de reunión | 📝 Corregir la suma de puntos (decía 50) |
| Sprint Backlog 1: tasks por US con estado, captura y URL público de Trello | 📝 Completar capturas y URL; T23 (US14) ya está implementada de verdad |
| Team Collaboration Insights: capturas de commits, Contributors y Pull Requests **de todos** | ⏳📝 Ver [REPARTO_TAREAS.md](REPARTO_TAREAS.md): cada integrante debe tener commits y PR propios |

### Criterio 4 · Implementación del backend (2.5 pts)

| Exigencia | Estado |
|---|---|
| Todos los endpoints del Product Backlog | ✅ US01–US16 del Sprint 1 y, adelantadas, US17–US20 (asistente con IA): 20/20 con sus 3 escenarios, ver [RESULTADO_SPRINT1.md](RESULTADO_SPRINT1.md) |
| Persistencia en base de datos | ✅ PostgreSQL con Spring Data JPA |
| Autenticación y autorización | ✅ Spring Security + JWT, roles PATIENT / PHYSIOTHERAPIST |
| Funcionamiento en el entorno local | ✅ 75 tests + Postman 71/71 contra la app en ejecución; 📝 capturas de Swagger, Postman y BD |
| Development Evidence: 3 a 5 commits representativos con descripción | 📝 Propuesta abajo |

**Propuesta de Development Evidence** (repositorio `fisiovital-web-services`; usar los ids que queden en el
repositorio oficial después del push):

| Branch | Commit | Mensaje | Qué se implementó |
|---|---|---|---|
| feature/us01-us03-iam | `af9001a` | feat(iam): add sign-up, sign-in and JWT security (US01-US03) | Registro de pacientes y fisioterapeutas, login con JWT y bloqueo tras 5 intentos |
| feature/us06-us07-availability | `2a96dca` | feat(scheduling): publish and query availability slots (US06-US07) | Publicación de bloques de 60 min sin cruces y consulta de horarios libres |
| feature/us09-us12-treatment | `560d43a` | feat(treatment): add plans, booking, reschedule and cancel (US08-US12, US16) | Planes de tratamiento, reserva con `@Version`, reprogramación y cancelación con regla de 24 h |
| feature/us13-reminders | `928f006` | feat(notifications): add scheduled session reminders (US13) | Recordatorio automático con `@Scheduled` |
| feature/fix-tb2-feedback | `8652866` | feat(appointments): add paginated appointment history for US14-US16 | Historial de citas paginado, búsqueda por paciente y 503 por carga lenta |

Estos commits son de tres integrantes distintos (Ricardo, Gabriel y Dinamage), lo que también ayuda en el criterio 3.

## Sección 2 · Informe (4 pts)

| Exigencia | Estado |
|---|---|
| `report_1ACC0236-20262.docx` (Informe de Proyecto) | 📝 |
| `keynote_1ACC0236-20262.pptx` (Presentación) — el profesor observó que el nombre estaba mal | 📝 |
| `report_team_1ACC0236-20262.docx` (Informe de Participación, Anexo B) | 📝 Team Leader |
| `report_ia_1ACC0236-20262.docx` (Declaración de Uso de IA, Anexo C) | 📝 Base en [BITACORA_IA_TB2.md](BITACORA_IA_TB2.md) |
| `project_1ACC0236-20262.zip` con el proyecto Web Services | ✅ Comando abajo |
| Carpeta de entrega `TB2_NroGrupo` | 📝 |
| Registro de Versiones con los cambios desde TB1 (y la fecha de la versión TB2) | 📝 |
| Student Outcome con acciones de TB1 y TB2 por integrante, sin notas clínicas | 📝 |
| Mínimo 2 conclusiones y 2 recomendaciones acumuladas (corregir conclusión 4: US04 y US14) | 📝 |
| Bibliografía en APA (incluida la del tablero de Trello) | 📝 |

**Generar el zip del proyecto** (sale limpio desde git: sin `target/`, `.env`, `presentacion/` ni `enunciado/`):

```bash
git archive --format=zip -o project_1ACC0236-20262.zip HEAD
```

## Sección 3 · Exposición (8 pts)

| Exigencia | Preparación |
|---|---|
| Máximo 12 minutos, alineada a los capítulos III y IV | Guion sugerido: problema (1 min) → C4 y clases (3) → Sprint 1 y colaboración (2) → demo (5) → cierre (1) |
| Demostración funcional del backend | Swagger (`/swagger-ui.html`) o Postman Runner (71/71, ~4 min por las pausas del asistente). Cierre con el asistente: "Resérvame el jueves en la tarde" → "el segundo" → "sí". Ensayar el flujo registro → búsqueda → publicar bloques → plan → reservar → reprogramar → historial |
| Sustentación individual (5 pts): C4, DDD, POO, BD, Spring Boot, JPA, Security/JWT, GitFlow, Trello | Cada integrante debe poder explicar **todo** el sistema, no solo su módulo |

## Mejora continua

En la TB3 se descuentan **1.5 puntos** si se corrigió solo parte de las observaciones de la TB2, y
**3 puntos** si no se corrigió ninguna. El estado de cada observación está en el README (sección
"Corregido según la retroalimentación del profesor").
