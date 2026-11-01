# Reparto de tareas — corrección TB2 y siguiente entrega

El profesor pidió que **todos** hagan commits de implementación, en **todo** el proyecto y repartidos a lo
largo del sprint. Reglas:

- Cada integrante commitea **desde su propia cuenta** y abre su Pull Request hacia `develop` (GitFlow).
- Conventional Commits (`feat(...)`, `fix(...)`, `test(...)`, `docs(...)`), commits pequeños, en días distintos.
- Nadie trabaja solo en "su" módulo: cada uno toca código, tests y documentación.
- Antes del primer commit, cada uno crea su `.gitignore` local con `.env` (ver README).

## Correcciones del informe (TB2)

| Integrante | Rama | Tareas | Archivos |
|---|---|---|---|
| Ricardo Victor Ambrocio Tantalean | `docs/report-design` | Diagramas C4 (contexto, contenedores, componentes), clases y BD en el informe (desde `docs/diagrams/`, con sus explicaciones); Tabla 8 con la tabla real | Informe cap. III, `docs/diagrams/*` |
| Gabriel Aldo Zavala Arteaga | `docs/report-evidence` | Development Evidence y Student Outcome sin notas clínicas; capturas de PRs y Contributors | Informe cap. IV |
| Franco Andre Parra Flores | `docs/report-backlog` | Corregir Story Points (47 / 68), Sprint Backlog (T23 Done verificado), capturas y URL de Trello, APA | Informe cap. IV, Trello |
| Chris Carrasco Betancourt | `docs/report-closing` | Conclusión 4 (US04 y US14), fecha de versión TB2, renombrar a `keynote_1ACC0236-20262.pptx` | Informe, presentación |

## TB3 · Sprint 2 (frontend) — propuesta

Cada integrante diseña y construye **una funcionalidad completa** (mock-up → Angular → integración con el backend),
así todos tienen commits de implementación y pueden explicar el sistema completo. Detalle en [PLAN_TB3.md](PLAN_TB3.md).

| Integrante | Diseño (Figma) | Implementación | Rama |
|---|---|---|---|
| Ricardo | Style Guide (Material Design UI Kit) | Proyecto Angular base: rutas, interceptor JWT, i18n, layout y footer con Términos de Servicio; módulo `iam` (registro e inicio de sesión) | `feature/web-app-setup`, `feature/web-app-iam` |
| Gabriel | Mock-ups Web App: búsqueda y perfil | Módulo `profiles` + `scheduling` (búsqueda, perfil, horarios libres, publicar bloques) | `feature/web-app-search-availability` |
| Franco | Mock-ups Web App: plan, sesiones y agenda | Módulo `treatment` (plan, reserva, reprogramar, cancelar, agenda diaria) | `feature/web-app-treatment` |
| Chris | Mock-ups Landing Page (web y mobile) | Landing Page (HTML5, CSS3, JS, i18n, ARIA) + módulos `appointments` y `notifications` de la Web App | `feature/landing-page`, `feature/web-app-history` |

## TB4 · Sprint 3 (cierre, despliegue y aceptación) — propuesta

El backend del asistente con IA (US17–US20) ya está implementado. Para que todos puedan explicarlo en la
sustentación, cada integrante estudia y defiende una historia del asistente, y además toma una tarea de cierre:

| Integrante | Historia del asistente que defiende | Tarea de cierre |
|---|---|---|
| Ricardo | US19 reservar (tool calling) | Pipeline CI/CD con GitHub Actions (build y tests) |
| Gabriel | US18 recomendación de especialista | Despliegue del backend y PostgreSQL en la nube |
| Franco | US17 dudas y reglas de seguridad | Despliegue de la Web App y la Landing Page |
| Chris | US20 reprogramar | Pruebas de aceptación con usuarios (Capítulo VI) y video de demostración |
