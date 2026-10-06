# Vihan Multi Speciality Hospital — Phase 1

Public website, no-login online booking, and staff portal (Doctors, Receptionists, IT Admin) for the
hospital network in **Miryalaguda, Huzurnagar and Kodad**.

The full design, decisions and milestones are in **[docs/DESIGN.md](docs/DESIGN.md)**.

| Part | Stack | Folder |
|---|---|---|
| Backend API | Java 21, Spring Boot 4.1, Spring Security, JPA/Hibernate, Flyway | `backend/` |
| Frontend | React 19, TypeScript, Vite, Tailwind CSS, React Router, TanStack Query | `frontend/` |
| Database | Oracle Database Free 23ai (local, Docker) → Oracle Autonomous DB (cloud) | — |

## Prerequisites

| Tool | Version | Check |
|---|---|---|
| JDK | 21 | `java -version` |
| Node.js | 22+ | `node -v` |
| Docker Desktop | running | `docker version` |

Maven is **not** needed: use the wrapper `mvnw`.

Set `JAVA_HOME` once (PowerShell), then open a new terminal:

```powershell
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Java\jdk-21.0.12", "User")
```

## 1. Start the local Oracle database (first time)

```powershell
cd backend
# Pick your own passwords. VIHAN_APP is the schema the app uses; Flyway creates all tables in it.
docker run -d --name vihan-oracle -p 1521:1521 `
  -e ORACLE_PASSWORD=<system-password> `
  -e APP_USER=VIHAN_APP -e APP_USER_PASSWORD=<app-password> `
  -v vihan-oracle-data:/opt/oracle/oradata `
  gvenzl/oracle-free:23-slim-faststart
docker logs -f vihan-oracle      # wait for "DATABASE IS READY TO USE!", then Ctrl+C
```

After that, start or stop it with `docker start vihan-oracle` / `docker stop vihan-oracle`.
Data survives restarts (volume `vihan-oracle-data`).
To **reset** the database completely: `docker rm -f vihan-oracle; docker volume rm vihan-oracle-data`, then run the command above again.

## 2. Configure the backend

```powershell
cd backend
copy .env.example .env     # then edit .env: DB_PASSWORD = <app-password>, and fill in the secrets
```

`.env` is git-ignored. Every secret and environment-specific value lives there. Moving to the cloud
database later means changing `.env` only (DESIGN.md §13).

## 3. Run

```powershell
# Terminal 1: API on http://localhost:8080
cd backend
.\mvnw spring-boot:run

# Terminal 2: website on http://localhost:5173
cd frontend
npm install
npm run dev
```

On first start, Flyway creates the schema (`V1`), the reference data (`V2`: 3 branches, 18 specialities) and,
in the `local` profile only, the **fictional sample data** (`db/dev-data`: 20 doctors with weekly schedules, placeholder gallery photos).
Uploaded images go to `backend/uploads/` (git-ignored).
The website shows a yellow **SAMPLE DATA** banner while sample data is in use.

| URL | What |
|---|---|
| http://localhost:5173 | Website (Vite dev server; proxies `/api` to the backend) |
| http://localhost:8080/swagger-ui | API docs (local profile only) |
| http://localhost:8080/actuator/health | Health check (includes the DB) |

## Tests

```powershell
cd backend;  .\mvnw test          # unit tests + integration tests on a throwaway Oracle container (needs Docker)
cd frontend; npm test             # Vitest + React Testing Library
cd frontend; npm run lint; npm run typecheck; npm run build
```

Run one backend test class: `.\mvnw test -Dtest=FoundationIntegrationTest`.
Run one frontend test file: `npx vitest run src/features/public/HomePage.test.tsx`.

## Project layout

```
backend/
  src/main/java/com/vihan/hospital/
    config/      security, CORS, JPA auditing, OpenAPI, app properties
    common/      error handling (ApiError), request logging, masking, base entity, reference-data cache
    branch/ speciality/ doctor/ gallery/   public API (/api/public/**) + IT Admin API (/api/admin/**)
    schedule/    weekly doctor schedules (read-only until M4)
    storage/     image upload checks, resizing, file storage, /files/** serving
  src/main/resources/
    application.yml (+ -local / -cloud)
    db/migration/  Flyway: V1 schema, V2 reference data (all environments)
    db/dev-data/   Flyway: fictional sample data (local profile only)
frontend/
  src/api/        typed API client + React Query hooks
  src/components/ layout and shared UI
  src/features/   pages by area (public/, staff/ …)
  src/lib/        formatting and SEO (schema.org) helpers
  public/placeholders/  labelled placeholder images used by the local sample data
  src/locales/    UI text (en.json; Telugu in Phase 2)
docs/DESIGN.md
```

## Conventions

- API errors always use one JSON shape: `{ timestamp, status, code, message, fieldErrors }`.
- Controllers return DTOs (Java records), never entities.
- Schema changes go in a **new** Flyway file (`V3__…sql`). Never edit an applied `V` file.
- No Aadhaar, clinical text or full mobile numbers in logs (`Masking.mobile()`).
- All times are IST (`Asia/Kolkata`), fixed in code.
- UI text goes in `src/locales/en.json`, not inline in components.
