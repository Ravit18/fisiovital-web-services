# Bitácora de uso de IA — TB2 (base para `report_ia_1ACC0236-20262.docx`)

El enunciado permite usar IA en las entregas (TB1 a TB4) **siempre que se adjunte una bitácora detallada**,
y la bitácora se verifica en la **defensa oral**: si un integrante no puede sustentar lo registrado, su aporte se
considera parcial o nulo. Por eso, cada integrante debe entender y poder explicar los cambios de esta lista.

Formato del Anexo C: **Etapa del trabajo · Herramienta de IA utilizada · Uso realizado**.

## Herramienta

| Campo | Valor |
|---|---|
| Herramienta | Claude Code (Anthropic), agente de programación en la aplicación de escritorio |
| Modelo | Claude Opus 5.5 (`claude-opus-5-5`) |
| Modo de uso | El integrante da instrucciones en español; el agente lee el repositorio, propone cambios, ejecuta pruebas y crea commits locales que el integrante revisa |
| Commits generados con IA | Llevan la línea `Co-Authored-By: Claude` en el mensaje del commit |

> Completar con cualquier otra herramienta de IA usada antes por el equipo (por ejemplo, en sesiones
> previas a la del 2026-10-21, o en la redacción del informe), indicando etapa y uso.

## Registro

### Sesiones previas (2026-10-20 y madrugada del 2026-10-21) — completar la herramienta exacta

| Etapa del trabajo | Uso realizado | Verificación humana |
|---|---|---|
| Diagnóstico | Diagnóstico en vivo de US01–US16 contra la retroalimentación | Revisión de la lista de fallas |
| Implementación (Sprint 1) | Correcciones de US04, US05, US07, US14 y del manejo de errores (404/405/409/503); rutas nuevas de US14–US16; CORS e intervalo de recordatorios configurables | Pruebas en Swagger y Postman |
| Pruebas | Suite de 56 tests de integración (H2) y colección de Postman con 48 escenarios y aserciones | `mvn test` y Collection Runner |

### Sesión del 2026-10-21 con Claude Code (Claude Opus 5.5) — Ricardo Ambrocio

| Etapa del trabajo | Uso realizado | Verificación humana |
|---|---|---|
| Configuración | Limpieza del repositorio, commit de ejemplos de Swagger, `.gitignore` fuera del repositorio, `.env` para secretos y documentación de la API key gratuita de Gemini | Revisión de los cambios |
| Implementación (refactor) | Paso de la lógica de búsqueda de especialistas del controller a `PhysiotherapistSearchService` | 56/56 tests en verde |
| Pruebas | Ejecución de la colección de Postman con Newman contra la app en ejecución (48/48) | Lectura del reporte |
| Integración | Merge de `origin/develop`, resolución de conflictos y eliminación del módulo `clinical/` | 56/56 tests en verde |
| Diseño técnico | Diagramas C4 (contexto, contenedores, componentes y código), de clases y de base de datos en Mermaid, generados a partir del código real | Contraste con entidades y controladores |
| Implementación (US13) | Configuración de Gmail SMTP por variables de entorno y guía para la contraseña de aplicación | Revisión del README |
| Implementación (US17–US20) | Asistente con Spring AI + Gemini: entidades `Conversation`/`ChatMessage`, reglas de seguridad, herramientas para reservar y reprogramar, actualización a Spring Boot 3.5 | 73/73 tests y conversación en vivo con Gemini real |
| Documentación | README con el estado del proyecto, CHANGELOG, checklist de la TB2, resultado del Sprint 1, reparto de tareas y esta bitácora | Lectura y ajuste por el equipo |

## Qué debe poder explicar cada integrante

- Por qué US04, US05 y US14 estaban incompletas y cómo se corrigieron.
- Cómo funciona el bloqueo optimista con `@Version` (US10) y la regla de cambio tardío de 24 horas (US11–US12).
- El flujo controller → service → repository y por qué ningún controller usa repositorios directamente.
- Qué valida cada test de integración y cada petición de Postman (código HTTP y mensaje i18n).
- Cómo leer los diagramas C4 y por qué se eliminó `ClinicalNote`.
- Cómo funciona el asistente: qué responde sin consultar a Gemini (reglas de seguridad), qué es el tool calling y
  por qué reservar con el asistente respeta las mismas reglas que la API.
