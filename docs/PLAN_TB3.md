# Plan TB3 (Semana 11) — Sprint 2: Frontend, Landing Page e integración

Fuente: `enunciado/Trabajo_1ACC0236-202620.pdf` y `enunciado/Rubrica_TB3_1ACC0236_202620.xlsx`.
**No empezar hasta cerrar la TB2** ([CHECKLIST_TB2.md](CHECKLIST_TB2.md)).

## Cronograma del curso

| Entrega | Sprint | Alcance |
|---|---|---|
| TB2 (sem. 7) | Sprint 1 | Backend (Web Services) — **este repositorio, cerrado** |
| **TB3 (sem. 11)** | **Sprint 2** | **Web App en Angular + Landing Page + integración completa con el backend** |
| TB4 (sem. 15) | Sprint 3 | Cierre del frontend, integración de la IA en la interfaz, despliegue en la nube (CI/CD con GitHub Actions) y pruebas de aceptación con usuarios |

El backend del asistente con IA (US17–US20, Spring AI + Gemini) **ya está implementado y probado** por adelantado.
En la TB3 la Web App puede incluir el chat del asistente; en la TB4 se cierra su integración final y el despliegue.

## Qué se entrega en la TB3

- Mock-ups completos (3.2) y Guía de Estilo (3.2.1) en Figma.
- Capítulo IV, sección 4.3 (Sprint 2): Planning con Review/Retrospective del Sprint 1, Backlog, Development
  Evidence y Team Collaboration Insights.
- Web App funcional según los mock-ups e **integrada por completo con el backend (sin datos simulados)**.
- Landing Page v1 según su mock-up.
- Conclusiones y Recomendaciones de TB3, y Bibliografía.
- Zip `project_1ACC0236-20262.zip` con Web Services, Web App y Landing Page, **sin `node_modules`**.

## Rúbrica TB3 (20 pts)

| # | Criterio | Pts | Qué exige para "Sobresaliente" |
|---|---|---|---|
| 1 | Diseño de Interfaz | 2.5 | Style Guide en Figma basado en el **Figma UI Kit oficial de Material Design** (captura + URL público). Mock-ups de alta fidelidad de las **4 vistas**: Landing web, Landing mobile, Web App web y Web App mobile. Si falta una, queda parcial |
| 2 | Sprint 2, proceso ágil | 1.5 | Planning completo, Backlog con tasks y Trello (captura + URL), capturas de commits, Contributors y PRs **de todos** |
| 3 | Implementación del frontend | 1.5 | 3 a 5 commits representativos (Web App y Landing) con descripción, y el frontend funcionando en local |
| 4 | Integración | 2.5 | Web App en Angular según mock-ups, **sin datos simulados**; Landing según mock-up; **los 3 productos funcionando juntos** en local |
| 5 | Informe y documentos | 4 | 4 documentos + zip con nombres exactos, Registro de Versiones desde TB2, Student Outcome TB1–TB3, 2 conclusiones y 2 recomendaciones, APA |
| 6 | Exposición (grupal) | 3 | 12 min, demo de la Web App y la Landing integradas con el backend |
| 7 | Sustentación (individual) | 5 | Diseño de interfaz, heurísticas de usabilidad, Material Design, Angular, Figma, integración frontend-backend, GitFlow, Trello |
| — | Mejora continua | −1.5 / −3 | Descuento si no se corrigieron **todas** las observaciones de la TB2 |

## Tecnologías obligatorias

| Producto | Tecnología |
|---|---|
| Landing Page | HTML5, CSS3, JavaScript |
| Web App | Angular, TypeScript, Angular Material |
| Diseño | Figma con el Figma UI Kit oficial de Material Design |
| Gestión | Trello, GitHub con GitFlow, Conventional Commits y SemVer |

## Consideraciones transversales (aplican desde la TB3)

- **i18n**: Landing Page y Web App en `en_US` y `es_419`; el backend ya responde en ambos idiomas por `Accept-Language`.
- **a11y**: atributos ARIA en la Landing Page y en la Web App.
- **Ética**: Términos de Servicio enlazados en el footer de la Landing y de la Web App (código de ética ACM/IEEE y CIP).

## Lo que el backend ya ofrece para el frontend

- CORS habilitado para `http://localhost:4200` (`CORS_ALLOWED_ORIGINS`).
- JWT: `POST /api/v1/auth/sign-in` devuelve `token`, `role`, `profileId` y `fullName` para armar la sesión en Angular.
- Todos los endpoints de US01–US20, documentados en Swagger con ejemplos ([RESULTADO_SPRINT1.md](RESULTADO_SPRINT1.md)).
- Chat del asistente: `POST /api/v1/assistant/messages` con `{conversationId?, message}`; la respuesta trae
  `conversationId` (enviarlo en el siguiente mensaje para mantener el contexto), `reply` y `source` (`AI` o `SAFETY`).
- Errores con `{status, message, details}` y mensajes i18n listos para mostrarse en la interfaz.

Al diseñar los mock-ups, revisar si alguna pantalla necesita un endpoint que no existe (por ejemplo, "mis
bloques publicados" o "mi perfil"); si es así, se agrega al backend como una tarea del Sprint 2.

## Organización sugerida

- Repositorios nuevos: `fisiovital-web-app` (Angular) y `fisiovital-landing-page`, cada uno con `main`,
  `develop` y release `v1.0.0` al cerrar el Sprint 2 (lo pide la tabla de repositorios del informe).
- Estructura de la Web App por bounded context, igual que el backend: `iam`, `profiles`, `scheduling`,
  `treatment`, `appointments`, `notifications` y `shared`.
- Reparto en [REPARTO_TAREAS.md](REPARTO_TAREAS.md).
