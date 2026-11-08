# Despliegue: Supabase (BD) + Render (backend)

## 1. Base de datos en Supabase
- La app se conecta por el **Session pooler** (IPv4). La "Direct connection" solo funciona por IPv6 y no sirve
  ni desde una red casera ni desde Render.
  - Supabase → **Connect** → Method: **Session pooler** → copiar host y user.
- Variables (en `.env` local o en Render):
  ```
  DB_URL=jdbc:postgresql://aws-1-us-west-2.pooler.supabase.com:5432/postgres?sslmode=require
  DB_USERNAME=postgres.<project-ref>
  DB_PASSWORD=<contraseña de la BD>
  ```
- Las tablas las crea Hibernate al arrancar (`ddl-auto=update`). Datos de demo: con la app corriendo,
  `.\scripts\seed-demo.ps1` (para Render: `-BaseUrl https://<servicio>.onrender.com/api/v1`).

## 2. Seguridad en Supabase (avisos "RLS Disabled" / "Sensitive Columns Exposed")
Supabase expone el esquema `public` por su Data API con una clave pública (`anon`). La app no usa esa API, así
que al arrancar `config/SupabaseSecurityConfig` activa RLS en todas las tablas (sin políticas: la API no ve nada)
y quita los permisos de `anon`/`authenticated`, también para tablas futuras. El backend entra como `postgres`
(dueño de las tablas) y no se ve afectado. En el log aparece `tablas de public sin RLS = 0`.
- Para comprobarlo: Supabase → **Advisors → Security Advisor → Rerun linter**. Los avisos deben desaparecer.
- Para desactivarlo (no recomendado): `app.db.supabase-hardening=false`.

## 3. Backend en Render
1. Mezclar a `main` (el `render.yaml` despliega esa rama).
2. Render → **New → Blueprint** → repo `fisiovital-web-services`.
3. Llenar las variables que pide (`DB_*`, `CORS_ALLOWED_ORIGINS`, `GEMINI_API_KEY`, `MAIL_*`). `JWT_SECRET` se genera solo.
4. Probar `https://<servicio>.onrender.com/swagger-ui/index.html`.

## 4. CI/CD con GitHub Actions (Capítulo V, 5.1)
Pipeline `Commit → Build → Test → Deploy` en `.github/workflows/ci-cd.yml` de cada repo:
- **Backend:** en push/PR a `main` o `develop` compila y corre las 105 pruebas (H2) y construye la imagen Docker.
  En push a `main` llama al Deploy Hook de Render (`autoDeploy: false` en `render.yaml`).
  - Secreto: `RENDER_DEPLOY_HOOK_URL` (Render → servicio → Settings → Deploy Hook).
- **Web App:** `npm ci`, pruebas (`ng test`), `ng build` (usa `environment.prod.ts` → URL de Render).
  En push a `main` publica en Netlify (`public/_redirects` para las rutas de Angular).
  - Netlify → **Add new site → Deploy manually** (crea el sitio vacío) → copiar **Site ID**.
  - Netlify → User settings → **Personal access tokens** → crear token.
  - Secretos en GitHub (repo → Settings → Secrets and variables → Actions): `NETLIFY_AUTH_TOKEN`, `NETLIFY_SITE_ID`.
- Luego, en Render, poner la URL de Netlify en `CORS_ALLOWED_ORIGINS`.
- Evidencia: pestaña **Actions** del repo → ejecución en verde.

Plan gratis: Render se duerme tras ~15 min sin uso (la primera petición tarda 30–60 s) y Supabase pausa el
proyecto tras 7 días sin actividad (se reactiva desde su panel).
