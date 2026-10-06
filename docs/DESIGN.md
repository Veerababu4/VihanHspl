# Vihan Multi Speciality Hospital — Phase 1 Design Document

| | |
|---|---|
| **Version** | 0.4 — M1 built (v0.2: hosting path §16.1–16.2, gap analysis §19, reschedule, reports · v0.3: all open questions decided §17 · v0.4: M1 Foundation done, versions pinned §7, local setup §14) |
| **Date** | 2026-10-06 |
| **Owner** | Veera |
| **Status** | Signed off → **M1 Foundation complete (2026-10-06)** → M2 next |

> **How to review:** Read sections 1–6 for *what* we are building, 7–12 for *how*, and 13–16 for setup/deployment/plan.
> **Section 19 explains how the 8 additional requirements found during the initial review are handled.**
> **Section 17 records the decisions on all open questions (answered 2026-10-06).**

---

## Table of Contents
1. [Overview & Goals](#1-overview--goals)
2. [Scope](#2-scope)
3. [Branches](#3-branches)
4. [Users, Roles & Permissions](#4-users-roles--permissions)
5. [Functional Requirements](#5-functional-requirements)
6. [Screens / Pages](#6-screens--pages)
7. [Technology Stack](#7-technology-stack)
8. [Architecture](#8-architecture)
9. [Data Model (Oracle)](#9-data-model-oracle)
10. [Key Business Logic](#10-key-business-logic)
11. [REST API](#11-rest-api)
12. [Security & Privacy](#12-security--privacy)
13. [Configuration & Environments](#13-configuration--environments)
14. [Local Development Setup](#14-local-development-setup)
15. [Testing Strategy](#15-testing-strategy)
16. [Deployment (Free Hosting)](#16-deployment-free-hosting)
17. [Decisions (Open Questions — Answered)](#17-decisions-open-questions--answered)
18. [Delivery Plan / Milestones](#18-delivery-plan--milestones)
19. [Additional Requirements (Gap Analysis)](#19-additional-requirements-gap-analysis)

---

## 1. Overview & Goals

Build a website and staff portal for **Vihan Multi Speciality Hospital**, a network of 3 hospitals in Nalgonda / Suryapet districts, Telangana.

**Goals**
1. Public website showing the hospital network, branches, doctors (photo, speciality, qualification, experience, serving branches) and hospital images.
2. Anyone can **book an appointment online without logging in**.
3. Secure staff portal for **Doctors, Receptionists and IT Admins**.
4. Doctors / IT Admin publish **daily time slots** per doctor per branch.
5. Receptionists see **appointment counts per doctor, per branch and for the whole network** for any day.
6. Receptionists / IT Admin **register patients** (name, age, gender, mobile, Aadhaar) and look up patient details and visit history.
7. Doctors record and view **clinical health history** (diagnosis, prescriptions, vitals).
8. Oracle Database, environment-file-driven config (local DB first, cloud DB later), hosted on free tiers.

---

## 2. Scope

### In scope (Phase 1)
- Public site: home, branches, doctors, doctor profile, gallery, about, contact, book appointment, check/cancel appointment.
- Staff login (JWT), role-based access.
- Doctor schedule templates, slot generation, leave/holiday blocking, manual slot add/block.
- Online + counter (walk-in / phone) bookings, appointment status workflow, daily token numbers.
- Receptionist dashboard (doctor / branch / network counts with status breakdown).
- Patient registration, search, profile, visit list.
- Doctor clinical module: visit notes, vitals, diagnosis, prescriptions, medical profile (allergies, chronic conditions…).
- IT Admin: manage staff users, doctors, branches, specialities, images, view audit log.
- Image upload (doctor photos, branch photos, gallery).
- Audit logging of sensitive access.

### Out of scope (Phase 1 — candidates for later phases)
- Billing, pharmacy, lab (LIS), radiology (PACS), IPD / bed management, insurance.
- Online payments.
- SMS / WhatsApp / email notifications (design keeps a hook for it).
- Patient self-login portal, telemedicine.
- Telugu language UI (design keeps i18n-ready).
- Lab report file attachments on visits.

---

## 3. Branches

| Code | Branch | District | Notes |
|---|---|---|---|
| `MLG` | Vihan Multi Speciality Hospital — **Miryalaguda** | Nalgonda | |
| `HZN` | Vihan Multi Speciality Hospital — **Huzurnagar** | Suryapet | |
| `KDD` | Vihan Multi Speciality Hospital — **Kodad** | Suryapet | |

> Spelling assumed as "Miryalaguda" (input said "Miryalaguga"). Addresses, phone numbers, emergency numbers, timings and photos are needed — see §17.

---

## 4. Users, Roles & Permissions

| Actor | Login? | Description |
|---|---|---|
| **Public user / Patient** | No | Browses site, books / checks / cancels own appointment using booking ref + mobile. |
| **Doctor** (`DOCTOR`) | Yes | Linked to one doctor profile. Manages own slots, sees own appointments, records clinical data. |
| **Receptionist** (`RECEPTIONIST`) | Yes | Has a home branch. Handles counter bookings, check-in, patient registration, dashboards. |
| **IT Admin** (`IT_ADMIN`) | Yes | Manages users, doctors, branches, slots, images, patients (non-clinical). |

### Permission matrix

| Capability | Public | Doctor | Receptionist | IT Admin |
|---|:-:|:-:|:-:|:-:|
| View branches / doctors / gallery | ✅ | ✅ | ✅ | ✅ |
| Book / check / cancel appointment (own, by ref + mobile) | ✅ | | | |
| Counter / phone booking for a patient | | | ✅ | ✅ |
| Manage schedule & slots | | ✅ own | | ✅ all |
| Mark doctor leave | | ✅ own | | ✅ all |
| View appointments list | | ✅ own | ✅ all branches | ✅ all |
| Change appointment status (check-in, no-show, cancel) | | ✅ own | ✅ | ✅ |
| Dashboard counts (doctor / branch / network) | | ✅ own | ✅ | ✅ |
| Register / edit patient demographics | | | ✅ | ✅ |
| Search patients, view demographics + **visit list** | | ✅ | ✅ | ✅ |
| View full Aadhaar number | | | ❌ masked | ❌ masked |
| **View / write clinical history** (diagnosis, notes, vitals, prescriptions, allergies) | | ✅ | ❌ | ❌ |
| Manage staff users, doctors, branches, specialities, images | | | | ✅ |
| View audit log | | | | ✅ |

**Decision recorded:** clinical history is visible to **Doctors only**. Receptionist / IT Admin see *visit list* = date, branch, doctor, visit type, status — not diagnosis or notes.

---

## 5. Functional Requirements

### 5.1 Public website
- **FR-P1** Home page: hero banner, hospital intro, 3 branch cards, specialities, featured doctors, "Book Appointment" CTA, emergency numbers.
- **FR-P2** Doctors page: list with photo, name, speciality, qualification, experience (years), serving branches. Filters: branch, speciality, name search.
- **FR-P3** Doctor profile: details + bio + weekly availability per branch + "Book" button.
- **FR-P4** Branch page: address, map, phone, timings, photos, doctors at this branch.
- **FR-P5** Gallery: hospital images, filterable by branch.
- **FR-P6** About / Contact pages.

### 5.2 Online booking (no login)
- **FR-B1** Steps: choose branch → speciality (optional) → doctor → date (today … +N days) → available slot → enter patient name, age, gender, mobile, reason (optional) → captcha → confirm.
- **FR-B2** On success show **booking reference** (e.g. `VMH-261006-7K3Q`), token number, doctor, branch, date/time, "arrive 15 min early" note. Printable / screenshot-friendly.
- **FR-B3** "Check my appointment": enter booking ref + mobile → view status; cancel allowed until **1 hour** before the slot.
- **FR-B3a** **Reschedule** (same page): pick another open slot with the **same doctor** (any of the doctor's branches) until the same cutoff. The booking ref stays the same, the old slot is released, and the new token is shown. Max **2 reschedules** per booking; after that, the patient cancels and books again.
- **FR-B4** Abuse protection: Cloudflare Turnstile captcha + max **3 active bookings per mobile per day** + per-IP rate limit.
- **FR-B5** A slot can never be double-booked (DB-enforced, §10.3).

### 5.3 Staff authentication
- **FR-A1** Login with username + password. Staff login page also shows a hospital summary panel and a doctor carousel (per original requirement #2).
- **FR-A2** Account lock after 5 failed attempts for 15 min. Force password change on first login.
- **FR-A3** Change password; IT Admin can reset a user's password.
- **FR-A4** Session: short-lived access token + refresh token (§12.2). Logout revokes refresh token.

### 5.4 Schedules & slots (Doctor own / IT Admin all)
- **FR-S1** Weekly **schedule template** per doctor per branch: day of week, start time, end time, slot length (minutes), effective-from / to. Default slot length **20 min**. Example: Dr. X — Mon/Wed/Fri at MLG 10:00–14:00 (20 min → 12 slots); Tue/Thu at KDD 16:00–19:00.
- **FR-S2** **Generate slots** for a date range from the template (default: rolling 14 days, nightly job + manual "Generate" button).
- **FR-S3** Per-day overrides: add an extra slot / session, block a slot, block a whole day.
- **FR-S4** Doctor **leave** (date range) → blocks open slots; if bookings exist, warn and list them for the receptionist to reschedule/cancel.
- **FR-S5** **Branch holidays** → no slots generated.
- **FR-S6** Validation: a doctor cannot have overlapping sessions across branches at the same time.

### 5.5 Appointments & reception
- **FR-R1** Appointment list with filters: date, branch, doctor, status; search by booking ref / mobile / name.
- **FR-R2** Status workflow: `BOOKED → CHECKED_IN → COMPLETED`, `BOOKED → CANCELLED`, `BOOKED → NO_SHOW` (§10.4).
- **FR-R3** Counter / phone booking by receptionist (channel = `COUNTER` / `PHONE`), optionally directly for a registered patient.
- **FR-R4** At check-in, **link appointment to a patient**: search existing by mobile/UHID/Aadhaar or register new. Check-in creates a **Visit**.
- **FR-R5** Walk-in without slot: receptionist can create a visit directly (doctor's queue), flagged `WALK_IN`.
- **FR-R6** Staff reschedule: move a booking to any open slot of the same or another doctor (e.g. when a doctor goes on leave). No cutoff or count limit for staff; reason required.
- **FR-R7** **Leave-conflict list:** when a doctor's leave or a blocked day hits existing bookings, those appointments are flagged `NEEDS_ACTION` on the receptionist's screen with patient name and mobile, so they can phone the patient and reschedule or cancel.

### 5.6 Dashboard (Receptionist / IT Admin; Doctor sees own)
- **FR-D1** Date picker (default today). Cards: **Network total**, **per-branch totals**.
- **FR-D2** Table: per doctor per branch — Total, Booked, Checked-in, Completed, Cancelled, No-show, Open slots remaining.
- **FR-D3** Branch filter ("All branches" / MLG / HZN / KDD). Receptionist defaults to home branch.
- **FR-D4** Optional range view (week / month) with simple chart — nice-to-have in Phase 1.
- **FR-D5** Export current table to Excel/CSV.
- **FR-D6** **Reports** page (Receptionist / IT Admin), each with date range + branch filter, viewable on screen and exportable to **Excel (XLSX), CSV and print/PDF**:
  | Report | Content |
  |---|---|
  | Daily appointment register | Every appointment for a day: token, time, patient, mobile (masked in export), doctor, status, channel |
  | Doctor-wise summary | Per doctor: booked, completed, cancelled, no-show, utilisation % (booked ÷ total slots) |
  | Branch-wise summary | Per branch per day/month: totals and status breakdown |
  | Patient registrations | New UHIDs per branch per day/month |
  | Online vs counter | Bookings by channel — shows website adoption |
  | No-show / cancellation trend | Rates per doctor / branch over time |

### 5.7 Patients
- **FR-PT1** Register: full name*, age* (or DOB), gender*, mobile*, Aadhaar (optional, validated), alternate mobile, address, city, blood group, emergency contact. System assigns **UHID** (e.g. `VMH-26-000123`) — one UHID valid across all 3 branches.
- **FR-PT2** Duplicate check before saving: same Aadhaar → block; same mobile + similar name → warn.
- **FR-PT3** Search by UHID, mobile, name, full Aadhaar (exact match via hash).
- **FR-PT4** Patient profile: demographics, upcoming appointments, **visit list** (date, branch, doctor, type, status).
- **FR-PT5** Edit demographics (audited).

### 5.8 Clinical (Doctor only)
- **FR-C1** Doctor's queue for today: checked-in patients in token order.
- **FR-C2** Visit screen: vitals (BP, pulse, temp, SpO₂, weight, height), chief complaint, diagnosis, clinical notes, advice, follow-up date, prescription items (medicine, dosage, frequency, duration, instructions).
- **FR-C3** Complete visit → appointment `COMPLETED`.
- **FR-C4** Patient **health history**: medical profile (allergies, chronic conditions, past surgeries, current medications, family history) + chronological list of past visits with full clinical details, across all branches and doctors.
- **FR-C5** Print prescription (simple print-friendly page with hospital letterhead).

### 5.9 IT Admin
- **FR-AD1** Users: create / edit / disable / reset password; role; home branch; link to doctor profile for `DOCTOR`.
- **FR-AD2** Doctors: create / edit, upload photo, speciality, qualification, registration no., practice start year (experience auto-computed), fee, bio, branches, active flag.
- **FR-AD3** Branches & specialities: CRUD, branch images.
- **FR-AD4** Gallery: upload, caption, order, branch tag.
- **FR-AD5** Audit log viewer (filter by user, action, date).

---

## 6. Screens / Pages

### Public (`/`)
| Route | Page |
|---|---|
| `/` | Home |
| `/doctors` | Doctor directory (filters) |
| `/doctors/:id` | Doctor profile |
| `/branches/:code` | Branch detail |
| `/gallery` | Gallery |
| `/about`, `/contact` | Static pages |
| `/book` | Booking wizard |
| `/book/confirmation/:ref` | Booking confirmation |
| `/appointment-status` | Check / cancel booking |

### Staff portal (`/staff`)
| Route | Roles | Page |
|---|---|---|
| `/staff/login` | all | Login (with hospital + doctor showcase panel) |
| `/staff/dashboard` | all | Dashboard (counts) |
| `/staff/appointments` | R, A, D(own) | Appointment list / check-in / counter booking |
| `/staff/patients` | R, A, D | Patient search & register |
| `/staff/patients/:id` | R, A, D | Patient profile + visit list |
| `/staff/patients/:id/history` | D | Clinical history |
| `/staff/queue` | D | Today's queue |
| `/staff/visits/:id` | D | Visit / prescription entry |
| `/staff/schedule` | D(own), A | Schedule templates, slots, leave |
| `/staff/admin/users` | A | Staff users |
| `/staff/admin/doctors` | A | Doctors + photos |
| `/staff/admin/branches` | A | Branches, specialities, holidays |
| `/staff/admin/gallery` | A | Images |
| `/staff/admin/audit` | A | Audit log |
| `/staff/profile` | all | Change password |

UI: mobile-first, responsive, hospital brand colours (to be confirmed — §17), accessible (WCAG AA contrast, keyboard nav).

---

## 7. Technology Stack

| Layer | Choice | Notes |
|---|---|---|
| Frontend | **React 19 + TypeScript + Vite** | SPA, built with Node 22 (already installed) |
| UI | **Tailwind CSS** + a small component kit (shadcn/ui style) | Responsive, quick to style |
| Frontend libs | React Router 7 (v8 needs Node ≥ 22.22), TanStack Query (API caching), React Hook Form + Zod (forms/validation), Recharts (dashboard chart), react-helmet-async (SEO meta) | |
| Backend | **Java 21 LTS + Spring Boot 4.1.1** (Spring Security 7, Hibernate 7.4, Jackson 3) | Single REST API, Maven wrapper (`mvnw`) — no global Maven needed |
| Backend libs | Spring Web, Spring Data JPA (Hibernate), Spring Security, Bean Validation, Flyway, jjwt, Bucket4j (rate limit), Thumbnailator (image resize), Apache POI or OpenCSV (export), springdoc-openapi (Swagger UI) | |
| Database | **Oracle** — local: Oracle Database Free; cloud: **Oracle Autonomous Database (Always Free)** | JDBC driver `ojdbc17` |
| Migrations | **Flyway 12** (`flyway-database-oracle`) | Same scripts for local & cloud |
| Auth | Spring Security + JWT + BCrypt | |
| Captcha | Cloudflare Turnstile | Free |
| Image storage | Phase 1: server disk (`/uploads`) behind a `FileStorageService` interface; later: OCI Object Storage | DB stores only URL/path |
| Build/CI | GitHub + GitHub Actions (build + tests) | optional but recommended |

### Where Java Collections are used (as requested)
- **Dashboard aggregation:** fetch the day's appointments once, then `Collectors.groupingBy(branch, groupingBy(doctor, groupingBy(status, counting())))` into `Map<String, Map<Long, EnumMap<Status, Long>>>` for network → branch → doctor counts.
- **Slot generation:** `TreeMap<LocalDate, List<SlotWindow>>` built from schedule templates, minus `Set<LocalDate>` holidays / leave dates, minus existing slots (`Set<LocalDateTime>`) to stay idempotent.
- **Overlap validation:** sort a doctor's sessions (`List` + `Comparator`) and scan adjacent pairs.
- **Reference-data cache:** branches & specialities in a `ConcurrentHashMap` refreshed on admin change.
- **Rate limiting:** `ConcurrentHashMap<String, Bucket>` keyed by IP / mobile.
- **Duplicate patient check:** normalised-name `Set` comparison.

---

## 8. Architecture

```
                    ┌─────────────────────────────────────────┐
  Browser (mobile / │  React SPA  (Vercel / Netlify, free)    │
  desktop)  ───────►│  /            public site               │
                    │  /staff/*     staff portal              │
                    │  /api/*  ──► rewrite/proxy to backend   │
                    └───────────────────┬─────────────────────┘
                                        │ HTTPS (JSON, JWT)
                    ┌───────────────────▼─────────────────────┐
                    │  Spring Boot API  (OCI Always Free VM)  │
                    │  Caddy reverse proxy (auto HTTPS)       │
                    │                                         │
                    │  Controllers → Services → Repositories  │
                    │  Security (JWT, roles) · Rate limiter   │
                    │  Scheduler (nightly slot generation)    │
                    │  FileStorageService → /uploads          │
                    └───────────────────┬─────────────────────┘
                                        │ JDBC (TLS / wallet)
                    ┌───────────────────▼─────────────────────┐
                    │  Oracle DB                              │
                    │  local: Oracle Database Free            │
                    │  cloud: Autonomous DB (Always Free)     │
                    └─────────────────────────────────────────┘
```

Proxying `/api/*` through the frontend host makes the API **same-origin** for the browser → simpler CORS and allows secure first-party refresh-token cookies.

### Repository layout
```
Vihan-Hospital-Phase1/
├── docs/
│   └── DESIGN.md
├── backend/
│   ├── mvnw, pom.xml
│   ├── .env.example
│   └── src/main/java/com/vihan/hospital/
│       ├── VihanHospitalApplication.java
│       ├── config/          (security, CORS, web, scheduling, OpenAPI)
│       ├── security/        (JWT filter, token service, user details)
│       ├── common/          (exceptions, error handler, audit, crypto, util)
│       ├── branch/          (entity, repo, service, controller, dto)
│       ├── speciality/
│       ├── doctor/
│       ├── schedule/        (templates, slots, leave, holidays, generator)
│       ├── appointment/
│       ├── patient/
│       ├── clinical/        (visits, prescriptions, medical profile)
│       ├── dashboard/
│       ├── user/            (staff users, auth endpoints)
│       ├── gallery/
│       └── storage/         (FileStorageService + LocalFileStorage)
│   └── src/main/resources/
│       ├── application.yml, application-local.yml, application-cloud.yml
│       └── db/migration/    (V1__schema.sql, V2__seed_reference.sql, …)
├── frontend/
│   ├── package.json, vite.config.ts, .env.example
│   └── src/
│       ├── api/             (typed API client)
│       ├── components/      (shared UI)
│       ├── features/public/ (home, doctors, booking…)
│       ├── features/staff/  (dashboard, patients, clinical, admin…)
│       ├── auth/            (auth context, route guards)
│       └── routes.tsx
├── .gitignore               (.env, wallet/, uploads/, node_modules, target)
└── README.md                (setup & run instructions)
```

Package-by-feature; each feature has `entity`, `repository`, `service`, `controller`, `dto` (Java records). Entities are never returned directly from controllers.

---

## 9. Data Model (Oracle)

### 9.1 Conventions
- Identity PKs: `NUMBER(19) GENERATED BY DEFAULT ON NULL AS IDENTITY`.
- Booleans: `NUMBER(1)` with `CHECK (col IN (0,1))` (works on Oracle 19c and 23ai/26ai). Hibernate is set to `hibernate.type.preferred_boolean_jdbc_type: BIT`; without it, Hibernate expects Oracle 23's native `BOOLEAN` and schema validation fails.
- Every table also has audit columns: `created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL, created_by NUMBER(19), updated_at TIMESTAMP, updated_by NUMBER(19)` (omitted below for brevity).
- All times are **IST (Asia/Kolkata)**; JVM and JDBC session time zone set explicitly (Autonomous DB runs in UTC).
- Soft delete via `is_active` for master data; transactional data is never hard-deleted.

### 9.2 Entity relationships
```
BRANCH ─┬─< DOCTOR_BRANCH >─┬─ DOCTOR >── SPECIALITY
        │                   │     │
        ├─< DOCTOR_SCHEDULE >─────┤
        ├─< SLOT >────────────────┤
        ├─< BRANCH_HOLIDAY        ├─< DOCTOR_LEAVE
        ├─< GALLERY_IMAGE         └── APP_USER (role=DOCTOR, 0..1)
        │
SLOT ──< APPOINTMENT >── PATIENT (nullable until linked)
APPOINTMENT ── VISIT (0..1) ──< PRESCRIPTION_ITEM
PATIENT ──< VISIT ;  PATIENT ── PATIENT_MEDICAL_PROFILE (0..1)
APP_USER ──< REFRESH_TOKEN ;  APP_USER ──< AUDIT_LOG
```

### 9.3 Tables (draft DDL → becomes Flyway `V1__schema.sql`)

```sql
-- ===== Master data =====
CREATE TABLE branch (
  branch_id        NUMBER(19) GENERATED BY DEFAULT ON NULL AS IDENTITY PRIMARY KEY,
  code             VARCHAR2(10)  NOT NULL CONSTRAINT uq_branch_code UNIQUE,  -- MLG/HZN/KDD
  name             VARCHAR2(120) NOT NULL,
  address          VARCHAR2(400) NOT NULL,
  city             VARCHAR2(60)  NOT NULL,
  district         VARCHAR2(60),
  pincode          VARCHAR2(6),
  phone            VARCHAR2(15),
  emergency_phone  VARCHAR2(15),
  email            VARCHAR2(120),
  map_url          VARCHAR2(500),
  image_url        VARCHAR2(500),
  opening_hours    VARCHAR2(200),
  is_active        NUMBER(1) DEFAULT 1 NOT NULL CHECK (is_active IN (0,1))
);

CREATE TABLE speciality (
  speciality_id    NUMBER(19) GENERATED BY DEFAULT ON NULL AS IDENTITY PRIMARY KEY,
  name             VARCHAR2(80) NOT NULL CONSTRAINT uq_speciality_name UNIQUE,
  description      VARCHAR2(1000),
  icon_url         VARCHAR2(500),
  is_active        NUMBER(1) DEFAULT 1 NOT NULL CHECK (is_active IN (0,1))
);

CREATE TABLE doctor (
  doctor_id           NUMBER(19) GENERATED BY DEFAULT ON NULL AS IDENTITY PRIMARY KEY,
  full_name           VARCHAR2(120) NOT NULL,
  gender              VARCHAR2(1)   CHECK (gender IN ('M','F','O')),
  speciality_id       NUMBER(19)    NOT NULL REFERENCES speciality,
  qualification       VARCHAR2(300) NOT NULL,           -- e.g. "MBBS, MS (Ortho)"
  designation         VARCHAR2(120),                    -- e.g. "Senior Consultant"
  registration_no     VARCHAR2(40)  CONSTRAINT uq_doctor_regno UNIQUE, -- TSMC reg no.
  practice_start_year NUMBER(4)     NOT NULL,           -- experience = current year - this
  languages           VARCHAR2(200),
  bio                 VARCHAR2(2000),
  photo_url           VARCHAR2(500),
  consultation_fee    NUMBER(8,2),
  is_active           NUMBER(1) DEFAULT 1 NOT NULL CHECK (is_active IN (0,1))
);

CREATE TABLE doctor_branch (
  doctor_id   NUMBER(19) NOT NULL REFERENCES doctor,
  branch_id   NUMBER(19) NOT NULL REFERENCES branch,
  CONSTRAINT pk_doctor_branch PRIMARY KEY (doctor_id, branch_id)
);

CREATE TABLE gallery_image (
  image_id    NUMBER(19) GENERATED BY DEFAULT ON NULL AS IDENTITY PRIMARY KEY,
  branch_id   NUMBER(19) REFERENCES branch,            -- null = network-wide
  title       VARCHAR2(120),
  caption     VARCHAR2(500),
  image_url   VARCHAR2(500) NOT NULL,
  thumb_url   VARCHAR2(500) NOT NULL,
  sort_order  NUMBER(5) DEFAULT 0 NOT NULL,
  is_active   NUMBER(1) DEFAULT 1 NOT NULL CHECK (is_active IN (0,1))
);

-- ===== Scheduling =====
CREATE TABLE doctor_schedule (                          -- weekly template
  schedule_id     NUMBER(19) GENERATED BY DEFAULT ON NULL AS IDENTITY PRIMARY KEY,
  doctor_id       NUMBER(19) NOT NULL REFERENCES doctor,
  branch_id       NUMBER(19) NOT NULL REFERENCES branch,
  day_of_week     NUMBER(1)  NOT NULL CHECK (day_of_week BETWEEN 1 AND 7), -- 1=Mon (ISO)
  start_time      VARCHAR2(5) NOT NULL,                 -- 'HH24:MI'
  end_time        VARCHAR2(5) NOT NULL,
  slot_minutes    NUMBER(3)  DEFAULT 20 NOT NULL CHECK (slot_minutes BETWEEN 5 AND 120),
  effective_from  DATE NOT NULL,
  effective_to    DATE,
  is_active       NUMBER(1) DEFAULT 1 NOT NULL CHECK (is_active IN (0,1))
);

CREATE TABLE slot (
  slot_id     NUMBER(19) GENERATED BY DEFAULT ON NULL AS IDENTITY PRIMARY KEY,
  doctor_id   NUMBER(19) NOT NULL REFERENCES doctor,
  branch_id   NUMBER(19) NOT NULL REFERENCES branch,
  slot_date   DATE       NOT NULL,
  start_at    TIMESTAMP  NOT NULL,
  end_at      TIMESTAMP  NOT NULL,
  token_no    NUMBER(4)  NOT NULL,                      -- position in doctor's day at branch
  status      VARCHAR2(10) DEFAULT 'OPEN' NOT NULL CHECK (status IN ('OPEN','BOOKED','BLOCKED')),
  source      VARCHAR2(10) DEFAULT 'TEMPLATE' NOT NULL CHECK (source IN ('TEMPLATE','MANUAL')),
  version     NUMBER(10) DEFAULT 0 NOT NULL,            -- optimistic locking
  CONSTRAINT uq_slot_doctor_time UNIQUE (doctor_id, start_at)  -- one place at a time
);
CREATE INDEX ix_slot_lookup ON slot (doctor_id, branch_id, slot_date, status);

CREATE TABLE doctor_leave (
  leave_id    NUMBER(19) GENERATED BY DEFAULT ON NULL AS IDENTITY PRIMARY KEY,
  doctor_id   NUMBER(19) NOT NULL REFERENCES doctor,
  from_date   DATE NOT NULL,
  to_date     DATE NOT NULL,
  reason      VARCHAR2(300),
  CHECK (to_date >= from_date)
);

CREATE TABLE branch_holiday (
  holiday_id    NUMBER(19) GENERATED BY DEFAULT ON NULL AS IDENTITY PRIMARY KEY,
  branch_id     NUMBER(19) REFERENCES branch,           -- null = all branches
  holiday_date  DATE NOT NULL,
  name          VARCHAR2(120) NOT NULL
);

-- ===== Patients =====
CREATE SEQUENCE seq_uhid START WITH 1 NOCACHE;

CREATE TABLE patient (
  patient_id          NUMBER(19) GENERATED BY DEFAULT ON NULL AS IDENTITY PRIMARY KEY,
  uhid                VARCHAR2(20)  NOT NULL CONSTRAINT uq_patient_uhid UNIQUE, -- VMH-26-000123
  full_name           VARCHAR2(120) NOT NULL,
  date_of_birth       DATE          NOT NULL,
  dob_is_estimated    NUMBER(1) DEFAULT 0 NOT NULL CHECK (dob_is_estimated IN (0,1)), -- when only age given
  gender              VARCHAR2(1)   NOT NULL CHECK (gender IN ('M','F','O')),
  mobile              VARCHAR2(10)  NOT NULL,           -- not unique: families share phones
  alt_mobile          VARCHAR2(10),
  aadhaar_enc         VARCHAR2(200),                    -- AES-256-GCM ciphertext (Base64)
  aadhaar_hash        VARCHAR2(64)  CONSTRAINT uq_patient_aadhaar UNIQUE, -- HMAC-SHA256 for exact search
  aadhaar_last4       VARCHAR2(4),                      -- for masked display XXXX-XXXX-1234
  address             VARCHAR2(400),
  city                VARCHAR2(60),
  blood_group         VARCHAR2(5),
  emergency_name      VARCHAR2(120),
  emergency_phone     VARCHAR2(10),
  registered_branch_id NUMBER(19) NOT NULL REFERENCES branch,
  consent_at          TIMESTAMP     NOT NULL,           -- privacy consent captured at registration
  is_active           NUMBER(1) DEFAULT 1 NOT NULL CHECK (is_active IN (0,1))
);
CREATE INDEX ix_patient_mobile ON patient (mobile);
CREATE INDEX ix_patient_name   ON patient (UPPER(full_name));

CREATE TABLE patient_medical_profile (                  -- DOCTOR-only
  patient_id           NUMBER(19) PRIMARY KEY REFERENCES patient,
  allergies            VARCHAR2(1000),
  chronic_conditions   VARCHAR2(1000),
  past_surgeries       VARCHAR2(1000),
  current_medications  VARCHAR2(1000),
  family_history       VARCHAR2(1000)
);

-- ===== Appointments & visits =====
CREATE TABLE appointment (
  appointment_id   NUMBER(19) GENERATED BY DEFAULT ON NULL AS IDENTITY PRIMARY KEY,
  booking_ref      VARCHAR2(20)  NOT NULL CONSTRAINT uq_appt_ref UNIQUE, -- VMH-261006-7K3Q
  slot_id          NUMBER(19)    REFERENCES slot,       -- null for walk-in
  doctor_id        NUMBER(19)    NOT NULL REFERENCES doctor,
  branch_id        NUMBER(19)    NOT NULL REFERENCES branch,
  appt_date        DATE          NOT NULL,              -- denormalised for fast dashboards
  start_at         TIMESTAMP,
  token_no         NUMBER(4),
  patient_id       NUMBER(19)    REFERENCES patient,    -- linked at check-in
  patient_name     VARCHAR2(120) NOT NULL,              -- as entered by booker
  patient_mobile   VARCHAR2(10)  NOT NULL,
  patient_age      NUMBER(3),
  patient_gender   VARCHAR2(1)   CHECK (patient_gender IN ('M','F','O')),
  reason           VARCHAR2(500),
  channel          VARCHAR2(10)  NOT NULL CHECK (channel IN ('ONLINE','COUNTER','PHONE','WALK_IN')),
  status           VARCHAR2(12)  DEFAULT 'BOOKED' NOT NULL
                   CHECK (status IN ('BOOKED','CHECKED_IN','COMPLETED','CANCELLED','NO_SHOW')),
  cancel_reason    VARCHAR2(300),
  cancelled_by     VARCHAR2(10)  CHECK (cancelled_by IN ('PATIENT','STAFF')),
  reschedule_count NUMBER(2) DEFAULT 0 NOT NULL,         -- public limit = 2
  needs_action     NUMBER(1) DEFAULT 0 NOT NULL CHECK (needs_action IN (0,1)), -- leave conflict
  booked_ip        VARCHAR2(45),
  consent_at       TIMESTAMP     NOT NULL,              -- privacy consent on booking form
  checked_in_at    TIMESTAMP,
  completed_at     TIMESTAMP,
  version          NUMBER(10) DEFAULT 0 NOT NULL
);
-- A slot can have at most one non-cancelled appointment (function-based unique index)
CREATE UNIQUE INDEX ux_appt_active_slot
  ON appointment (CASE WHEN status <> 'CANCELLED' THEN slot_id END);
CREATE INDEX ix_appt_dashboard ON appointment (appt_date, branch_id, doctor_id, status);
CREATE INDEX ix_appt_mobile    ON appointment (patient_mobile, appt_date);

CREATE TABLE visit (
  visit_id         NUMBER(19) GENERATED BY DEFAULT ON NULL AS IDENTITY PRIMARY KEY,
  appointment_id   NUMBER(19) CONSTRAINT uq_visit_appt UNIQUE REFERENCES appointment,
  patient_id       NUMBER(19) NOT NULL REFERENCES patient,
  doctor_id        NUMBER(19) NOT NULL REFERENCES doctor,
  branch_id        NUMBER(19) NOT NULL REFERENCES branch,
  visit_date       DATE       NOT NULL,
  visit_type       VARCHAR2(12) DEFAULT 'OP' NOT NULL CHECK (visit_type IN ('OP','FOLLOW_UP','EMERGENCY')),
  status           VARCHAR2(12) DEFAULT 'OPEN' NOT NULL CHECK (status IN ('OPEN','COMPLETED')),
  -- clinical fields (DOCTOR-only in API)
  bp_systolic      NUMBER(3),
  bp_diastolic     NUMBER(3),
  pulse            NUMBER(3),
  temperature_f    NUMBER(4,1),
  spo2             NUMBER(3),
  weight_kg        NUMBER(5,2),
  height_cm        NUMBER(5,1),
  chief_complaint  VARCHAR2(1000),
  diagnosis        VARCHAR2(1000),
  clinical_notes   CLOB,
  advice           VARCHAR2(2000),
  follow_up_date   DATE
);
CREATE INDEX ix_visit_patient ON visit (patient_id, visit_date);

CREATE TABLE prescription_item (
  item_id        NUMBER(19) GENERATED BY DEFAULT ON NULL AS IDENTITY PRIMARY KEY,
  visit_id       NUMBER(19) NOT NULL REFERENCES visit,
  medicine_name  VARCHAR2(200) NOT NULL,
  dosage         VARCHAR2(60),                          -- 500 mg
  frequency      VARCHAR2(60),                          -- 1-0-1
  duration       VARCHAR2(60),                          -- 5 days
  instructions   VARCHAR2(300),                         -- after food
  sort_order     NUMBER(3) DEFAULT 0 NOT NULL
);

-- ===== Security & audit =====
CREATE TABLE app_user (
  user_id               NUMBER(19) GENERATED BY DEFAULT ON NULL AS IDENTITY PRIMARY KEY,
  username              VARCHAR2(60)  NOT NULL CONSTRAINT uq_user_username UNIQUE,
  password_hash         VARCHAR2(100) NOT NULL,         -- BCrypt
  full_name             VARCHAR2(120) NOT NULL,
  role                  VARCHAR2(15)  NOT NULL CHECK (role IN ('DOCTOR','RECEPTIONIST','IT_ADMIN')),
  doctor_id             NUMBER(19) CONSTRAINT uq_user_doctor UNIQUE REFERENCES doctor,
  home_branch_id        NUMBER(19) REFERENCES branch,
  mobile                VARCHAR2(10),
  email                 VARCHAR2(120),
  is_active             NUMBER(1) DEFAULT 1 NOT NULL CHECK (is_active IN (0,1)),
  must_change_password  NUMBER(1) DEFAULT 1 NOT NULL CHECK (must_change_password IN (0,1)),
  failed_attempts       NUMBER(2) DEFAULT 0 NOT NULL,
  locked_until          TIMESTAMP,
  last_login_at         TIMESTAMP,
  CHECK ((role = 'DOCTOR' AND doctor_id IS NOT NULL) OR (role <> 'DOCTOR' AND doctor_id IS NULL))
);

CREATE TABLE refresh_token (
  token_id     NUMBER(19) GENERATED BY DEFAULT ON NULL AS IDENTITY PRIMARY KEY,
  user_id      NUMBER(19) NOT NULL REFERENCES app_user,
  token_hash   VARCHAR2(64) NOT NULL CONSTRAINT uq_refresh_hash UNIQUE, -- SHA-256 of token
  expires_at   TIMESTAMP NOT NULL,
  revoked      NUMBER(1) DEFAULT 0 NOT NULL CHECK (revoked IN (0,1))
);

CREATE TABLE audit_log (
  audit_id     NUMBER(19) GENERATED BY DEFAULT ON NULL AS IDENTITY PRIMARY KEY,
  user_id      NUMBER(19) REFERENCES app_user,          -- null = public action
  action       VARCHAR2(40) NOT NULL,                   -- LOGIN, VIEW_PATIENT, VIEW_HISTORY, UPDATE_PATIENT…
  entity_type  VARCHAR2(40),
  entity_id    VARCHAR2(40),
  details      VARCHAR2(1000),                          -- never contains Aadhaar / clinical text
  ip_address   VARCHAR2(45),
  created_at   TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL
);
CREATE INDEX ix_audit_time ON audit_log (created_at);
```

### 9.4 Seed data (`V2__seed_reference.sql`)
- 3 branches (MLG, HZN, KDD).
- Specialities: General Medicine, General Surgery, Orthopaedics, Obstetrics & Gynaecology, Paediatrics, Cardiology, Neurology, Nephrology, Urology, Pulmonology, Gastroenterology, ENT, Ophthalmology, Dermatology, Dental, Anaesthesiology, Radiology, Emergency & Critical Care. *(Trim to what the hospital actually offers.)*
- One bootstrap `IT_ADMIN` user whose password comes from env var `BOOTSTRAP_ADMIN_PASSWORD` on first startup (not stored in SQL) — must change on first login.
- **Local-only sample data** (never deployed; `db/dev-data` Flyway location enabled only in the `local` profile, plus a `LocalDataSeeder` bean annotated `@Profile("local")` that runs only when tables are empty):
  | Source | Data |
  |---|---|
  | SQL (`db/dev-data/R__dev_sample_data.sql`) ✅ M1 | 20 **fictional** doctors (`SAMPLE-001…020`, every speciality covered, 5 serving 2–3 branches), doctor↔branch mappings, 162 weekly sessions (10:00–14:00, some 17:00–19:00, 20-min slots, no cross-branch overlaps) |
  | Java `LocalDataSeeder` (needs app crypto; each part is built in the milestone that adds its code: users M3, slots M4, appointments M5, patients M7, visits M8) | Staff users with BCrypt passwords (1 IT Admin, doctor logins, 1 receptionist per branch; password from `DEV_SAMPLE_PASSWORD`), ~40 fictional patients (Aadhaar encrypted with the env key, checksum-valid fictional numbers), slots via the real slot generator, appointments for yesterday/today/next 7 days in every status, ~60 past visits with vitals, diagnoses and prescriptions |
  | Static files | Initials-based avatar images and placeholder hospital images — no real photos |
- In the `local` profile the UI shows a **"SAMPLE DATA"** banner.

---

## 10. Key Business Logic

### 10.1 Identifiers
| ID | Format | Example | Notes |
|---|---|---|---|
| UHID | `VMH-YY-NNNNNN` | `VMH-26-000123` | From `seq_uhid`; network-wide |
| Booking ref | `VMH-YYMMDD-XXXX` | `VMH-261006-7K3Q` | 4 random chars (no 0/O/1/I) — non-guessable, required with mobile for public lookup |
| Token no. | integer per doctor/branch/day | `7` | = slot position in that session |

### 10.2 Slot generation
```
for each active doctor_schedule overlapping [from, to]:
  for each date in range where ISO day_of_week matches:
     skip if branch holiday or doctor on leave
     for t = start_time; t + slot_minutes <= end_time; t += slot_minutes:
        if no slot exists for (doctor, date+t)  → insert OPEN slot, token = sequence
```
- Idempotent (re-running never duplicates; DB unique key is the final guard).
- Nightly `@Scheduled` job (00:30 IST) keeps a rolling **14-day** window; "Generate" button for manual runs.
- Template changes affect only **future, OPEN** slots; booked slots are never silently removed.

### 10.3 Booking (concurrency-safe)
Single transaction:
1. Verify captcha token (Turnstile server-side API). Check IP rate limit and **≤ 3 active bookings for this mobile on that date**.
2. `UPDATE slot SET status='BOOKED', version=version+1 WHERE slot_id=:id AND status='OPEN'` → if 0 rows → `409 Slot no longer available`.
3. `INSERT appointment` (unique index `ux_appt_active_slot` is a second safety net).
4. Return booking ref + token.

Cancel → appointment `CANCELLED`, slot back to `OPEN` (if slot still in future).

**Reschedule** (one transaction): lock new slot exactly as in step 2 → set old slot `OPEN` → update appointment's `slot_id`, `doctor_id`, `branch_id`, `appt_date`, `start_at`, `token_no`, `reschedule_count + 1`, clear `needs_action` → audit-log old/new slot. If the new slot is taken → `409`, and nothing changes.

### 10.4 Appointment state machine
```
            ┌──────────► CANCELLED   (patient before cutoff / staff any time before check-in)
 BOOKED ────┼──────────► NO_SHOW     (staff, or auto at end of day)
            └──► CHECKED_IN ──► COMPLETED   (doctor completes visit)
```
- `CHECKED_IN` requires a linked `patient_id` and creates a `VISIT`.
- End-of-day job marks remaining `BOOKED` (past) as `NO_SHOW`.
- Reschedule keeps the status `BOOKED` (only the slot changes).
- Invalid transitions → `409`.

### 10.5 Dashboard query
One query per request: `SELECT branch_id, doctor_id, status, COUNT(*) FROM appointment WHERE appt_date = :d GROUP BY …` plus open-slot counts from `slot`; assembled into network → branch → doctor tree with Java collections (§7). Cached 30 s.

### 10.6 Age ↔ DOB
Registration accepts **age** (requirement) or exact DOB. If only age: `date_of_birth = 1-Jul of (current year − age)`, `dob_is_estimated = 1`. Age is always computed on display, so it stays correct over the years.

### 10.7 Aadhaar handling
- Optional field. Validate 12 digits, not starting with 0/1, **Verhoeff checksum**.
- Store: `aadhaar_enc` (AES-256-GCM, key from env), `aadhaar_hash` (HMAC-SHA256 with separate key → exact-match search + uniqueness), `aadhaar_last4`.
- API always returns masked `XXXX-XXXX-1234`. No endpoint returns the full number in Phase 1.

---

## 11. REST API

Base path `/api`. JSON. Errors use a single shape: `{ "timestamp", "status", "code", "message", "fieldErrors": [...] }`. Swagger UI at `/swagger-ui` (local profile only). Lists are paginated: `?page=0&size=20&sort=…`.

### 11.1 Public (no auth)
| Method | Path | Purpose |
|---|---|---|
| GET | `/public/branches` | All active branches |
| GET | `/public/branches/{code}` | Branch detail + its doctors |
| GET | `/public/specialities` | Specialities |
| GET | `/public/doctors?branch=&speciality=&q=` | Doctor directory |
| GET | `/public/doctors/{id}` | Doctor profile + weekly availability |
| GET | `/public/doctors/{id}/slots?branch=&date=` | Open slots for a date |
| GET | `/public/gallery?branch=` | Images |
| POST | `/public/appointments` | Book (body incl. `captchaToken`) |
| POST | `/public/appointments/lookup` | `{bookingRef, mobile}` → status |
| POST | `/public/appointments/cancel` | `{bookingRef, mobile, reason}` |
| POST | `/public/appointments/reschedule` | `{bookingRef, mobile, newSlotId}` |
| GET | `/files/**` | Serve uploaded images (cache headers) |

### 11.2 Auth
| Method | Path | Purpose |
|---|---|---|
| POST | `/auth/login` | → access token (body) + refresh token (httpOnly cookie) |
| POST | `/auth/refresh` | Rotate refresh token, new access token |
| POST | `/auth/logout` | Revoke refresh token |
| POST | `/auth/change-password` | Authenticated |
| GET | `/auth/me` | Current user, role, doctorId, homeBranch |

### 11.3 Staff — schedules & slots (`DOCTOR` own, `IT_ADMIN` all)
| Method | Path |
|---|---|
| GET / POST | `/staff/doctors/{id}/schedules` |
| PUT / DELETE | `/staff/schedules/{scheduleId}` |
| POST | `/staff/doctors/{id}/slots/generate` `{from, to}` |
| GET | `/staff/doctors/{id}/slots?date=&branch=` |
| POST | `/staff/slots` (manual slot/session) |
| PATCH | `/staff/slots/{id}` `{status: BLOCKED/OPEN}` |
| GET / POST / DELETE | `/staff/doctors/{id}/leaves` |
| GET / POST / DELETE | `/staff/holidays` (`IT_ADMIN`) |

### 11.4 Staff — appointments & dashboard
| Method | Path | Roles |
|---|---|---|
| GET | `/staff/appointments?date=&branch=&doctor=&status=&q=` | R, A, D(own) |
| POST | `/staff/appointments` (counter/phone/walk-in) | R, A |
| PATCH | `/staff/appointments/{id}/status` `{status, reason}` | R, A, D(own) |
| POST | `/staff/appointments/{id}/reschedule` `{newSlotId, reason}` | R, A |
| GET | `/staff/appointments/needs-action?branch=` | R, A |
| GET | `/staff/appointments/{id}/patient-matches` (candidates by mobile/name) | R, A |
| POST | `/staff/appointments/{id}/check-in` `{patientId}` → creates visit | R, A |
| GET | `/staff/dashboard/summary?date=&branch=` | R, A, D(own) |
| GET | `/staff/dashboard/summary/export?date=&branch=` (CSV/XLSX) | R, A |
| GET | `/staff/reports/{reportType}?from=&to=&branch=&format=json\|csv\|xlsx` | R, A |

### 11.5 Staff — patients
| Method | Path | Roles |
|---|---|---|
| POST | `/staff/patients` | R, A |
| GET | `/staff/patients?uhid=&mobile=&name=&aadhaar=` | R, A, D |
| GET | `/staff/patients/{id}` (masked Aadhaar) | R, A, D |
| PUT | `/staff/patients/{id}` | R, A |
| GET | `/staff/patients/{id}/visits` (non-clinical list) | R, A, D |
| GET | `/staff/patients/duplicates?mobile=&name=&aadhaar=` | R, A |

### 11.6 Clinical (`DOCTOR` only)
| Method | Path |
|---|---|
| GET | `/clinical/queue?date=` (own checked-in patients) |
| GET | `/clinical/patients/{id}/history` (profile + full visits) |
| GET / PUT | `/clinical/patients/{id}/medical-profile` |
| GET / PUT | `/clinical/visits/{id}` (vitals, diagnosis, notes…) |
| PUT | `/clinical/visits/{id}/prescription` (replace items) |
| POST | `/clinical/visits/{id}/complete` |

### 11.7 Admin (`IT_ADMIN`)
| Method | Path |
|---|---|
| CRUD | `/admin/users`, `POST /admin/users/{id}/reset-password` |
| CRUD | `/admin/doctors`, `POST /admin/doctors/{id}/photo` (multipart) |
| CRUD | `/admin/branches`, `POST /admin/branches/{id}/image` |
| CRUD | `/admin/specialities` |
| CRUD | `/admin/gallery` (multipart upload) |
| GET | `/admin/audit-logs?user=&action=&from=&to=` |

---

## 12. Security & Privacy

### 12.1 Compliance context
Patient data (health records, Aadhaar) is **sensitive personal data** under India's **Digital Personal Data Protection Act, 2023** and the **Aadhaar Act** rules. Phase 1 measures:
- Collect Aadhaar only if the patient provides it (optional), encrypt it, never display it in full, never log it.
- Clinical data accessible to doctors only (role check at API level, not just UI).
- Audit every patient view, history view, patient edit, login, failed login, export.
- Privacy notice on the booking form + consent checkbox.
- *Recommendation:* get the hospital's privacy policy / consent text reviewed by their legal/compliance contact before go-live.

### 12.2 Authentication
- Passwords: BCrypt (cost 12); min 8 chars with letters + digits.
- **Access token:** JWT (HS256, 30 min), sent as `Authorization: Bearer`, held in memory on the frontend.
- **Refresh token:** random 256-bit, stored **hashed** in DB, 8 h (one shift), rotated on use, `httpOnly; Secure; SameSite=Strict` cookie on `/api/auth`. Works because `/api` is proxied same-origin (§8).
- Lockout: 5 failures → 15 min lock.

### 12.3 Authorization
- `@PreAuthorize` on every staff endpoint + ownership checks (doctor can only touch own schedule/appointments).
- Separate DTOs: `PatientSummaryDto` (non-clinical) vs `ClinicalHistoryDto` (doctor only) so clinical fields can't leak via the wrong endpoint.

### 12.4 Other controls
- Public booking: Turnstile captcha, Bucket4j rate limits (e.g. 10 req/min/IP on booking, 30 req/min/IP on lookups), 3 bookings/mobile/day.
- Input validation (Bean Validation + Zod on frontend). JPA parameter binding → no SQL injection.
- File upload: max 5 MB, allow JPEG/PNG/WebP, verify magic bytes, re-encode/resize (strips EXIF), random file names.
- HTTPS everywhere (Caddy / host-provided TLS). Security headers (CSP, HSTS, X-Content-Type-Options, frame-ancestors).
- CORS restricted to configured origins.
- Logs: no Aadhaar, no clinical text, mobile masked (`98XXXXXX21`).
- Secrets only in env vars / `.env` (git-ignored). DB user for the app is **not** the ADMIN user.

---

## 13. Configuration & Environments

### 13.1 Profiles
| Profile | DB | Purpose |
|---|---|---|
| `local` | Oracle Database Free on your PC | Development, sample data, Swagger on |
| `cloud` | Oracle Autonomous DB (wallet / TLS) | Free-hosting deployment |

`application.yml` imports the env file: `spring.config.import: optional:file:.env[.properties]`, and every secret is a placeholder like `${DB_PASSWORD}`. Switching from local to cloud = **change the `.env` file only**, no code change.

### 13.2 `backend/.env.example` (committed — real `.env` is git-ignored)
```properties
SPRING_PROFILES_ACTIVE=local
SERVER_PORT=8080
TZ=Asia/Kolkata

# ---- Oracle (local) ----
DB_URL=jdbc:oracle:thin:@localhost:1521/FREEPDB1
DB_USER=VIHAN_APP
DB_PASSWORD=change_me

# ---- Oracle (cloud: Autonomous DB) — fill later ----
# DB_URL=jdbc:oracle:thin:@vihandb_tp?TNS_ADMIN=/opt/vihan/wallet
#   or TLS without wallet:
# DB_URL=jdbc:oracle:thin:@(description=(retry_count=20)(retry_delay=3)(address=(protocol=tcps)(port=1522)(host=<HOSTNAME>))(connect_data=(service_name=<SERVICE>_tp.adb.oraclecloud.com))(security=(ssl_server_dn_match=yes)))
# DB_WALLET_PASSWORD=

# ---- Security ----
JWT_SECRET=<64+ random chars>
AADHAAR_ENC_KEY=<base64 32-byte key>
AADHAAR_HASH_KEY=<base64 32-byte key>
BOOTSTRAP_ADMIN_PASSWORD=<first-run only>
TURNSTILE_SECRET_KEY=<from Cloudflare>   # local: Cloudflare test key
CORS_ALLOWED_ORIGINS=http://localhost:5173

# ---- Files ----
UPLOAD_DIR=./uploads
PUBLIC_FILE_BASE_URL=http://localhost:8080/files
```

### 13.3 `frontend/.env.example`
```properties
VITE_API_BASE_URL=/api            # Vite dev proxy → http://localhost:8080
VITE_TURNSTILE_SITE_KEY=<site key>
```

---

## 14. Local Development Setup

Current machine check (2026-10-06, M1): **Node 22.20 ✅, Git ✅, Oracle JDK 21.0.12 ✅ (set `JAVA_HOME`), Maven — not needed (wrapper), Docker Desktop 4.94 ✅. Oracle runs in Docker (option B) as container `vihan-oracle`, image `gvenzl/oracle-free:23-slim-faststart` (Oracle 23.26).**

| Tool | Action |
|---|---|
| **JDK 21** | Install Eclipse Temurin 21 (MSI), set `JAVA_HOME`, add to PATH |
| **Oracle Database Free** | Option A (no Docker): install *Oracle Database Free* for Windows. Option B: install Docker Desktop and run the `gvenzl/oracle-free` image (lighter, easy reset). |
| DB client | SQL Developer or the *Oracle SQL Developer* extension for VS Code |
| IDE | IntelliJ IDEA Community (backend) + VS Code (frontend) |
| Node 22 | Already installed |

One-time DB setup: with Docker (option B) the container creates the user automatically from `APP_USER` / `APP_USER_PASSWORD` (see README). Without Docker (option A), run as SYSTEM in `FREEPDB1`:
```sql
CREATE USER vihan_app IDENTIFIED BY "change_me" QUOTA UNLIMITED ON users;
GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE VIEW, CREATE PROCEDURE TO vihan_app;
```
Flyway creates all tables on first backend start.

Run:
```
backend>  .\mvnw spring-boot:run          → http://localhost:8080  (Swagger: /swagger-ui)
frontend> npm install ; npm run dev       → http://localhost:5173
```

---

## 15. Testing Strategy

| Level | Tooling | Focus |
|---|---|---|
| Unit | JUnit 5 + Mockito + AssertJ | Slot generator, state machine, Aadhaar Verhoeff/crypto, dashboard aggregation, ID formats |
| Integration | Spring Boot Test + **Testcontainers** (`gvenzl/oracle-free`): a fresh Oracle per run, so Flyway is tested from scratch | Repositories, Flyway migrations, booking concurrency (parallel threads → exactly one success), security rules per role |
| API | MockMvc / REST Assured | Every endpoint: 200/400/401/403/404/409 |
| Frontend | Vitest + React Testing Library | Forms, guards, booking wizard |
| E2E | Playwright | Book → lookup → cancel; staff login → check-in → doctor completes visit → receptionist sees counts |
| Manual UAT | Checklist per milestone | Run with real hospital data before go-live |

Target: ≥ 70 % line coverage on service layer; all role-permission rules covered by tests.

---

## 16. Deployment (Free Hosting)

| Component | Free option (recommended) | Notes |
|---|---|---|
| Database | **Oracle Cloud Always Free — Autonomous Database** | Download wallet or enable TLS (no wallet) + IP allow-list. Always Free ADBs are stopped after a period of inactivity — check current OCI policy; light traffic keeps it alive. |
| Backend | **Oracle Cloud Always Free Compute VM** (Ampere A1 / AMD micro), Ubuntu, Java 21, app runs as `systemd` service | Same cloud region as DB = low latency. **Caddy** for automatic HTTPS. |
| Frontend | **Vercel** or **Netlify** (free) → `vihan-hospital.vercel.app` | `/api/*` rewrite to backend URL |
| Images | VM disk `/opt/vihan/uploads` (Phase 1) → OCI Object Storage later | Back up with the DB |
| Domain | Start with free subdomains (`*.vercel.app` for site, DuckDNS or similar for API) | Later buy e.g. `vihanhospital.in` (low annual cost) for credibility |
| CI/CD | GitHub Actions: build + test on push; manual deploy script (`scp` jar + `systemctl restart`) | |

> Oracle Cloud sign-up requires card verification (not charged for Always Free resources). Keep the account on Always Free only.

### 16.1 Hosting during development (before M10)
| Need | Option |
|---|---|
| Show work-in-progress to hospital staff from your PC | **Cloudflare Tunnel** (free): exposes `localhost` on a public HTTPS URL; no server needed. Use a non-production DB with sample data only. |
| Always-on dev/demo environment | Oracle Cloud Always Free (ADB + VM) as above, plus Vercel preview URLs for each git branch |
| Why not Render / Railway / Koyeb free tiers? | No managed Oracle DB. Free web services sleep when idle (slow first request), and some offer only trial credits. Fine for the frontend, weak for an Oracle-backed API. |

### 16.2 Moving to own hosting later
The design is host-agnostic, so moving is a redeploy, not a rewrite:
- **Frontend** = static files from `npm run build`. They can be served by any web server (Nginx, Apache, cPanel) or CDN. Move the `/api` rewrite rule into the web server config.
- **Backend** = one executable jar, or a Docker image (Dockerfile added in M10). It runs on any VPS/server with Java 21.
- **Database:** keep Autonomous DB, or move with Oracle Data Pump (`expdp` / `impdp`) to the hospital's own Oracle server. Flyway keeps the schema identical.
- **Config:** change only `.env` (DB URL, wallet, CORS origins, file base URL).
- **Images:** copy the `uploads/` folder (or switch `FileStorageService` to object storage).
- **Domain:** point DNS (`A` / `CNAME` records) at the new server. If the domain is bought early, the URL never changes for patients.

**Go-live checklist:** real `.env` on VM with strong secrets · `SPRING_PROFILES_ACTIVE=cloud` · Swagger disabled · bootstrap admin password changed · HTTPS verified · backup job (Data Pump export / DB backups) scheduled and a restore tested · privacy policy page live.

---

## 17. Decisions (Open Questions — Answered)

All open questions were answered by Veera on **2026-10-06**. Items marked *changed* differ from the originally proposed default.

| # | Question | Decision |
|---|---|---|
| 1 | Real data: branch addresses, phones, emergency numbers, timings, map links, photos | **Placeholders now; real data supplied before launch** (via IT Admin screens or seed update) |
| 2 | Doctor list: name, speciality, qualification, reg. no., practice start year, branches, weekly timings, photo (with consent) | **Fictional sample doctors + placeholder avatars, local only — never deployed publicly**; real data before launch |
| 3 | Specialities actually offered | **Default list of 18 in §9.4**; IT Admin can edit anytime |
| 4 | Default slot length | **20 min** *(changed from 15)*, overridable per schedule |
| 5 | How far ahead can public book? | **14 days** |
| 6 | Max active bookings per mobile per day | **3** |
| 7 | Patient cancel / reschedule cutoff | **1 hour before slot** *(changed from 2 h)* |
| 8 | Consultation fee / online payment | **Show fee; pay at counter; no online payment in Phase 1** |
| 9 | Receptionist branch visibility | **All branches visible; home branch is the default filter** |
| 10 | IT Admin access to patient demographics | **Yes — non-clinical only** |
| 11 | Doctor sees history recorded by other doctors | **Yes — full history across doctors & branches** |
| 12 | Aadhaar mandatory? | **Optional** |
| 13 | Telugu language support | **Phase 2; UI built i18n-ready in Phase 1** |
| 14 | SMS / WhatsApp confirmation | **Phase 2; `NotificationService` hook (log-only) in Phase 1** |
| 15 | Brand: logo, colours, fonts | **Placeholder teal/blue medical palette + text logo**; swap via one theme config |
| 16 | Domain name | **Free subdomain during development; buy domain before public launch** |
| 17 | Branch spelling | **Miryalaguda** |
| 18 | Lab report upload on visits | **Phase 2** |

---

## 18. Delivery Plan / Milestones

Each milestone ends with: working build, tests passing, demo on local, your review.

| # | Milestone | Deliverables |
|---|---|---|
| **M0** | Design sign-off | This document reviewed; §17 answered ✅ |
| **M1** ✅ | Foundation | Repo + `.gitignore`, Spring Boot & React scaffolds, `.env` config, local Oracle connection, Flyway V1/V2, error handling, logging, README |
| **M2** | Public website | Branches, specialities, doctors, doctor profile, gallery, about/contact; IT Admin doctor/branch/gallery management + image upload |
| **M3** | Auth & users | Login (with showcase panel), JWT + refresh, roles, lockout, change/reset password, user admin, audit log |
| **M4** | Scheduling | Schedule templates, slot generation job, manual slots, block, leave, holidays, overlap validation |
| **M5** | Booking | Public booking wizard, captcha, rate limits, confirmation, lookup/cancel; counter/phone booking; concurrency tests |
| **M6** | Reception & dashboard | Appointment list, status workflow, check-in + patient linking, dashboard (doctor/branch/network), export |
| **M7** | Patients | Registration (Aadhaar encryption, duplicates), search, profile, visit list |
| **M8** | Clinical | Doctor queue, visit entry, prescriptions, medical profile, history, print prescription |
| **M9** | Hardening & UAT | Security headers, E2E tests, performance check, accessibility pass, UAT with real data |
| **M10** | Deployment | OCI ADB + VM, Caddy HTTPS, Vercel frontend, cloud `.env`, backups, go-live checklist |

---

## 19. Additional Requirements (Gap Analysis)

The original input had 10 points. The initial review found **8 further requirements** a hospital system needs. Each one is listed below with how this design handles it and where the details are.

| # | Gap | Status in Phase 1 | Where |
|---|---|---|---|
| 1 | Legal protection of Aadhaar & health data | ✅ Designed | §10.7, §12, §19.1 |
| 2 | Abuse of no-login public booking | ✅ Designed | §5.2 FR-B4, §12.4, §19.2 |
| 3 | Appointment lifecycle, cancel/reschedule, token | ✅ Designed | §5.2, §5.5, §10.1, §10.3, §10.4, §19.3 |
| 4 | Doctors working at multiple branches; leave & holidays | ✅ Designed | §5.4, §10.2, §19.4 |
| 5 | Linking online bookings & walk-ins to patient records | ✅ Designed | §5.5 FR-R4, §19.5 |
| 6 | Staff account management | ✅ Designed | §5.9, §12.2, §19.6 |
| 7 | "Login page" vs public home page | ✅ Clarified | §5.3 FR-A1, §19.7 |
| 8 | Operational items (maps, emergency, fees, notifications, Telugu, reports, backups, HTTPS, domain) | ✅ Phase 1 / 🔜 Phase 2 hooks | §19.8 |

### 19.1 Legal protection of Aadhaar & health data
**Risk:** Aadhaar numbers and health records are sensitive personal data under the **DPDP Act 2023** and **Aadhaar Act** rules. A leak harms patients and creates legal exposure for the hospital.

**Handled by:**
- Aadhaar is **optional**, validated (Verhoeff), stored **AES-256-GCM encrypted**, searchable only by **HMAC hash**, and always **displayed masked** (`XXXX-XXXX-1234`). No API returns the full number.
- Clinical data is **doctor-only**, enforced in the API with separate DTOs, not just hidden in the UI.
- **Audit log** for: patient view, history view, patient edit, export, login, failed login, password reset.
- **Consent:** checkbox + privacy notice link on the booking form and the registration screen. `patient.consent_at` and `appointment.consent_at` timestamps are recorded (§9.3).
- **Data minimisation:** logs never contain Aadhaar or clinical text; mobile numbers are masked in logs and exports.
- **Encryption in transit** (HTTPS / TLS to DB) and **at rest** (Autonomous DB encrypts storage by default).
- **Retention:** clinical records are kept; patient correction requests are handled by IT Admin via edit (audited). The formal retention / erasure policy should be confirmed by the hospital's legal contact.
- **Breach readiness:** audit log + access logs kept 1 year so incidents can be investigated.

### 19.2 Abuse of no-login public booking
**Risk:** bots or pranksters fill all slots with fake bookings, so real patients can't book.

**Handled by (layered):**
1. **Cloudflare Turnstile captcha** verified server-side on booking, cancel and reschedule.
2. **Per-IP rate limits** (Bucket4j): booking 10/min, lookup 30/min.
3. **Per-mobile limit:** max 3 active bookings per mobile per date; max 2 reschedules per booking.
4. **Indian mobile validation:** 10 digits, starts with 6–9.
5. **No-show tracking:** mobiles with ≥ 3 no-shows in 30 days are flagged to the receptionist (not blocked automatically).
6. **Phase 2:** SMS OTP verification can be switched on via a config flag (`BOOKING_OTP_ENABLED`) without changing the booking flow.

### 19.3 Appointment lifecycle, cancel/reschedule, token
**Risk:** without a defined lifecycle, counts are wrong, cancelled slots aren't reused and patients can't change plans.

**Handled by:**
- Status flow `BOOKED → CHECKED_IN → COMPLETED`, plus `CANCELLED` / `NO_SHOW` (§10.4). Auto no-show at end of day.
- **Patient self-service** with booking ref + mobile: view, **cancel**, **reschedule** (FR-B3, FR-B3a) up to 1 h before.
- **Staff reschedule** without limits (FR-R6).
- Cancelled or rescheduled slots return to `OPEN` immediately, so they can be rebooked.
- **Token number** = position of the slot in that doctor's session; shown on confirmation and used for the doctor's queue order.

### 19.4 Doctors at multiple branches; leave & holidays
**Risk:** a doctor visiting Kodad on Monday and Huzurnagar on Tuesday must not get slots in two places at once, and leave must not leave patients stranded.

**Handled by:**
- `doctor_branch` many-to-many + `doctor_schedule` **per doctor per branch per weekday**, with effective dates (§9.3).
- **Overlap validation** across branches when saving a template (§7, §5.4 FR-S6) + DB unique key `(doctor_id, start_at)` as a final guard.
- **Doctor leave** and **branch holidays** stop slot generation; existing bookings on those days become **`NEEDS_ACTION`** for reception to call and reschedule (FR-R7).
- **Doctor profile** shows the weekly availability per branch to the public (FR-P3).

### 19.5 Linking online bookings & walk-ins to patient records
**Risk:** online bookings have no UHID. Without linking, the same patient gets duplicate records and history is split.

**Handled by:**
- Public booking stores what the patient typed (name, age, gender, mobile); `patient_id` stays empty until check-in.
- **Optional UHID field** in the booking form for returning patients. It is pre-linked only if the UHID's mobile matches the entered mobile.
- **At check-in** the receptionist sees **suggested matches** (same mobile; similar name; same age ± 2), then either picks one or clicks **"Register new"** with the form pre-filled from the booking (FR-R4).
- **Duplicate prevention** on registration: same Aadhaar → blocked; same mobile + similar name → warning (FR-PT2).
- **Walk-ins** without a booking: register or find the patient first, then create the visit (`WALK_IN`).
- Check-in is only allowed after linking, so every visit always belongs to exactly one patient record.

### 19.6 Staff account management
**Risk:** shared logins, ex-employees still having access, forgotten passwords handled insecurely.

**Handled by:**
- **One account per person**: IT Admin creates users with a role, home branch, and (for doctors) a link to the doctor profile.
- **Temporary password + forced change** on first login; IT Admin **reset** (also forces change).
- **Deactivate** instead of delete; deactivation **revokes all refresh tokens immediately**, so the user is logged out within 30 min at most (access token expiry).
- **Doctor leaves the hospital:** deactivate the doctor profile → hidden from the public site, future open slots blocked, future bookings flagged `NEEDS_ACTION`.
- **Lockout** after 5 failed attempts; all account actions audited.
- At least **2 IT Admin accounts** recommended so admin access isn't lost.

### 19.7 "Login page" vs public home page
**Original requirement #2:** "Login page should show hospital details with all doctor details…"

**Handled by doing both:**
- The **public home page / doctors page** (no login) shows hospital details and all doctors with speciality, experience, qualification and serving branches (FR-P1, FR-P2).
- The **staff login page** (`/staff/login`) has a side panel with hospital highlights, branch summary and a doctor carousel (FR-A1).
- Both use the same `/public/*` APIs, so there is no duplicate logic.

### 19.8 Operational items
| Item | Phase 1 | Phase 2 / later |
|---|---|---|
| **Google Maps** | Branch page embeds the Google Maps *embed* URL stored in `branch.map_url` (no API key or billing needed) + "Get directions" link | — |
| **Emergency numbers** | Sticky "🚑 Emergency" call button on mobile; `tel:` links to each branch's `emergency_phone`; shown in header/footer | — |
| **Consultation fee** | Shown on doctor profile and booking confirmation (`doctor.consultation_fee`); paid at the counter | Online payment (Razorpay / PhonePe) |
| **SMS / WhatsApp confirmation** | `NotificationService` interface with a **log-only** implementation; events: `BOOKED`, `CANCELLED`, `RESCHEDULED`, `NEEDS_ACTION`, `REMINDER` | Plug in MSG91 / Twilio / WhatsApp Business by adding an implementation + DLT template registration — no change to booking code |
| **Telugu language** | UI text in `react-i18next` resource files (`en.json`); no hard-coded strings | Add `te.json` + Telugu names for doctors/specialities/branches |
| **Reports & export** | Dashboard export + 6 reports in XLSX / CSV / print (FR-D6) | Scheduled email of daily report |
| **DB backups** | Daily **Data Pump export** (`expdp`) of the app schema + `uploads/` archive, copied to OCI Object Storage, **keep 14 days**; monthly **restore test** into the local DB | Point-in-time recovery on paid tier |
| **HTTPS** | Automatic certificates via Vercel/Netlify (frontend) and Caddy + Let's Encrypt (API); HSTS on | — |
| **Domain** | Free subdomain during development | Buy `vihanhospital.in` (or similar) before public launch; point DNS |
| **SEO** | Page titles/meta per page (react-helmet-async), `sitemap.xml`, `robots.txt`, schema.org `Hospital` / `Physician` JSON-LD | Pre-rendering of public pages if search ranking matters |
| **Monitoring** | Spring Boot Actuator `/actuator/health` + free uptime monitor (e.g. UptimeRobot) pinging every 5 min — also keeps Autonomous DB active | Centralised logs |

### 19.9 Impact on milestones
| Gap | Delivered in |
|---|---|
| 1 Aadhaar/privacy | M3 (audit), M7 (encryption, consent) |
| 2 Booking abuse | M5 |
| 3 Lifecycle / reschedule | M5, M6 |
| 4 Multi-branch scheduling | M4 |
| 5 Patient linking | M6, M7 |
| 6 Staff accounts | M3 |
| 7 Login/home page | M2, M3 |
| 8 Operational items | Maps/emergency/fee/SEO → M2; notifications hook → M5; reports → M6; backups/HTTPS/monitoring/domain → M10 |

---

*End of document — v0.4.*
