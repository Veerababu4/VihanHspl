# Handover — Vihan Multi Speciality Hospital, Phase 1

*Updated 2026-10-12 (M9 finished, waiting for the user's review and OK to commit). Start here, then read `docs/DESIGN.md` (v0.13) and `README.md`.*

## 1. Where things stand

| Milestone | Status | Commit |
|---|---|---|
| M1 Foundation | ✅ done | `d19184c` |
| M2 Public website + IT Admin APIs | ✅ done | `52de17f` |
| M3 Staff login, users, audit, admin screens | ✅ done | `23cd3fb` |
| M4 Scheduling (timetables, slots, leave, holidays) | ✅ done | `abc5224` |
| M5 Booking (online wizard, check/cancel/reschedule, counter) | ✅ done | `d60db86` |
| M6 Reception & dashboard (check-in, walk-ins, reports) | ✅ done | `153129e` |
| M7 Patients (Aadhaar encryption, duplicates, search, profile) | ✅ done | `aaf2e28` |
| M8 Clinical (doctor queue, visit, prescription, history, print) | ✅ done | `80aec9a` |
| **M9 Hardening & UAT** | **done, not yet committed** (WIP commit `ee8bf2c` + the finishing changes, see §4) | — |
| **M10 Deployment** | next (decisions open, see §5) | — |

- Branch `main`, pushed to the **private** GitHub repo `https://github.com/Veerababu4/VihanHspl.git` (`origin`). Pre-push audit (2026-10-11): no secrets in any commit; real `.env` files are git-ignored.
- Last verified 2026-10-12 (M9 finished): backend **177/177** (`mvnw test`, 2 min 38 s), frontend unit **63/63**, E2E **9/9** (`npm run e2e`, incl. 4 accessibility tests), typecheck + lint clean. Performance check passed 2026-10-11.
- Database migrations applied: `V1__schema` … `V4__reception` (plus the repeatable local sample data).

## 2. How to start everything (local)

```powershell
# 1. Docker Desktop (per-user install)
& "C:\Users\Veera\AppData\Local\Programs\DockerDesktop\Docker Desktop.exe"
docker start vihan-oracle            # Oracle Database Free 23, volume vihan-oracle-data

# 2. Backend → http://localhost:8080  (Swagger: /swagger-ui)
cd C:\Veera\Work\Vihan-Hospital-Phase1\backend
.\mvnw spring-boot:run               # JAVA_HOME = C:\Program Files\Java\jdk-21.0.12

# 3. Website → http://localhost:5173  (staff portal: /staff/login)
cd C:\Veera\Work\Vihan-Hospital-Phase1\frontend
npm run dev
```

- Logins and passwords: `backend/.env` (git-ignored). `DEV_SAMPLE_PASSWORD` is for `itadmin`, `reception.mlg|hzn|kdd`, `doctor001`–`doctor020`; `BOOTSTRAP_ADMIN_PASSWORD` is for `admin`. Never print these in chat.
- The captcha uses Cloudflare Turnstile **test keys** (accept any token) in `backend/.env` and `frontend/.env.local`.
- All doctors, patients and visits are **fictional sample data** (local profile only; the yellow banner says so).

## 3. Tests

```powershell
cd backend;  .\mvnw test        # ~5 min; Testcontainers starts its own Oracle (Docker must run)
cd frontend; npm test; npm run lint; npm run typecheck; npm run build
cd frontend; npm run e2e       # Playwright E2E + accessibility on the production build (backend + Docker must run)
```

Since M9 the committed suite `frontend/e2e` replaces the ad-hoc browser scripts. The older `playwright-core` scripts (installed Edge) are still in the old session scratchpads under `C:\Users\Veera\AppData\Local\Temp\claude\C--Veera-Work\` (`c6661b5d…\scratchpad\pw`: booking.js, sched.js, overflow.js, staff.js; later ones: reception.js, patients.js, clinical.js).

## 4. M9 Hardening & UAT (DESIGN §18)

**Done (2026-10-11/12):**
- DESIGN v0.12 §16: free hosting = Oracle Cloud Always Free (Hyderabad), Caddy serves site + `/api` on one VM, idle-reclamation safeguards. *The user could not create an OCI account yet*; fallback offered: a ~₹400–800/month 4 GB VPS with the same setup (no code change), or a hospital PC + Cloudflare Tunnel. Decide in M10.
- Security headers: backend `SecurityConfig` (`API_CSP`, Referrer/Permissions-Policy, HSTS), tested in `FoundationIntegrationTest`. Website headers in `frontend/security-headers.ts` (used by `vite preview`) and `deploy/Caddyfile` (draft); `security-headers.test.ts` keeps them identical. Zod `jitless` (CSP has no eval). Vite proxy `changeOrigin: false` (same-origin like Caddy). Cloud profile binds the backend to 127.0.0.1.
- E2E suite `frontend/e2e` (`npm run e2e`, installed Edge, builds + previews on :4173, needs backend + Docker): public pages, booking → find → reschedule → cancel, phone widths, walk-in → doctor completes → dashboard count, reception kept out of clinical, plus the accessibility tests. 9/9 pass. Every test fails on a CSP violation or browser error.
- Performance: `PerformanceCheckTest` (`@Tag("perf")`, excluded from `mvnw test`; run `.mvnw test -Dgroups=perf -Dperf.excluded=`) loads a year of data into the throwaway test DB (30k patients, 178k slots, 140k appointments, 112k visits, 224k prescription lines, 300k audit rows). **All 20 heavy calls under 0.5 s** (slowest: 31-day daily register 0.49 s / 1.4 MB, audit log first page 0.35 s). No index changes needed.
- Accessibility: `frontend/e2e/a11y.spec.ts` (axe-core, WCAG 2.1 AA) on public pages, every booking step, staff and doctor screens, plus a keyboard test for dialogs. Fixed: brand-600 darkened to `#0e7a73` (contrast 5.2:1), slate-400 → slate-500, login `<dl>` order, underlined phone link, booking summary `<dl>` (fee line moved out), white instead of 80 % white text on selected date/time chips (booking + staff Slots tab).
- Dialog focus: new `src/lib/dialogFocus.ts` `useDialogFocus` hook used by `Modal` (`components/ui.tsx`) and the gallery lightbox (now a `Lightbox` component): focus moves to the first field (else the dialog), Tab/Shift+Tab wrap inside, Escape closes, focus returns to the opener. `onClose` is kept in a ref, so pages passing an inline arrow no longer re-run the effect and yank focus on re-render. Unit test `src/components/Modal.test.tsx`.
- `docs/UAT-CHECKLIST.md` for hospital staff (v1).
- Test DB container: 5 min start-up timeout + 2 attempts. DoctorsAdminPage test: 15 s first wait.

**Finished 2026-10-12:** all of the above, full test runs (numbers in §1), DESIGN v0.13 (§12.4 headers, §15 E2E / accessibility / performance, §18 M9 ✅), README (E2E, performance, preview, layout).

**Next:** the user reviews M9 → on their OK, commit (and ask again before any push). Then M10 (§5). Running the UAT checklist with real data moves to M10, since it needs the live server.

### Original M9 plan

Planned scope:
1. **Security headers** (§12.4): CSP, HSTS, X-Content-Type-Options, frame-ancestors, Referrer-Policy. The CSP must allow `https://challenges.cloudflare.com` (script + frame) for Turnstile and Google Maps embeds (`https://www.google.com`) for branch maps. Swagger stays local-only.
2. **E2E tests** (§15, Playwright): book → lookup → cancel; staff login → check-in → doctor completes visit → reception sees the counts. Turn the existing ad-hoc scripts into a committed, repeatable suite.
3. **Performance check**: dashboard, reports and the slot board on realistic data volumes; check query plans and indexes.
4. **Accessibility pass**: WCAG AA contrast, keyboard navigation, focus handling in dialogs, form labels.
5. **UAT checklist** for hospital staff, run against real data before go-live.

Finish the milestone as usual: tests + browser check → DESIGN.md v0.12 as-built notes + README → summary → **ask before committing**.

## 5. M10 Deployment: decisions still open

A needs checklist was given to the user (claude.ai artifact `1KYG6VL9V9DaaAsZfqcCnK`, v2 includes the database choice).

- **Website hosting**: DESIGN §16 says Vercel, but Vercel Hobby is non-commercial only. Recommended: (A) serve the frontend from the same Oracle Cloud VM via Caddy (₹0, same-origin); (B) Cloudflare Pages; (C) Vercel Pro (~US$20/month). Update §16 once the user chooses.
- **Database**: production must **not** be the laptop Oracle (availability, internet exposure, DPDP Act / laptop loss, no off-site backup). Use Autonomous DB on Oracle Cloud, or Oracle Database Free in Docker on the server. The laptop DB stays for development and demos.
- **Domain name** (hospital-owned), **account ownership** (hospital email, 2FA, a second admin), Oracle Cloud home region (India South / Hyderabad; upgrade to Pay As You Go to avoid idle reclamation), real Turnstile keys, alert contact.
- Rate limits use the client IP, so the reverse proxy must pass the real visitor IP.
- Secrets are generated on the server; the user keeps copies in a password manager (Aadhaar keys also offline). The user must not paste passwords into chat: they put them in a local file and the session reads it without printing.

## 6. Working agreements with the user

- Work milestone by milestone: build, test, browser-check, summarise, then wait.
- **Commit only when the user says so.** "resume" / "next" = continue from the plan.
- **Never delete anything** (files, folders, DB rows, containers, temp files) without asking first. Only add or update code in this project folder.
- When the session context gets near full, update this file (and the memory notes) at a clean, compiling point, then hand off to a fresh session.

## 7. Gotchas learned

- Adding a Maven dependency needs a full backend restart (devtools can't hot-load jars). Vite reads `.env*` only at startup.
- Backend integration tests share one database: tests that create doctors deactivate them afterwards, and use random mobile numbers (the 3-bookings-per-mobile-per-day rule spans doctors) and random client IPs (booking rate limit 10/min). The login limit is raised to 1000 in tests.
- `SlotRepository` modifying queries clear the JPA persistence context: re-read entities you change after `bookIfOpen` / `releaseIfBooked`.
- Jackson 3 fails on missing primitive booleans in request bodies: use `Boolean` in DTOs.
- On Oracle 23, Hibernate needs `preferred_boolean_jdbc_type: BIT` for the `NUMBER(1)` booleans.
- Frontend tests: threads pool, 20 s test timeout, 5 s wait timeout, and the Turnstile site key forced empty so a developer's `.env.local` never leaks in.
- Phone layouts: `<fieldset>` and grid items need `min-w-0`, or wide rows (e.g. date chips) widen the page. Check overflow with Playwright `isMobile: false` at 390 px.
- Aadhaar search and duplicate checks use POST bodies, so the number never appears in URLs or logs.
- When opening a new Claude Code window from a session, clear the inherited marker first, or the new session won't save its transcript:
  `powershell -NoExit -Command "Remove-Item Env:CLAUDE_CODE_CHILD_SESSION -ErrorAction SilentlyContinue; claude 'resume Vihan hospital session'"`
