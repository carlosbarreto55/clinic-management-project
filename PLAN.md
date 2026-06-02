# PLAN.md — Clinic Room Scheduling API

**Project:** Clinic Room Scheduling API (internal iOS app backend)  
**Generated:** 2026-06-01  
**Total Tasks:** 28  

## Progress Summary

| Phase | Name | Tasks | Completed | Remaining | Status |
|-------|------|-------|-----------|-----------|--------|
| 0 | Project Setup | 4 | 4 | 0 | ✅ Complete |
| 1 | Database & Entities | 4 | 4 | 0 | ✅ Complete |
| 2 | Authentication | 3 | 3 | 0 | ✅ Complete |
| 3 | Services & Staff | 2 | 2 | 0 | ✅ Complete |
| 4 | Appointments CRUD | 6 | 6 | 0 | ✅ Complete |
| 5 | Real-Time Streaming | 3 | 0 | 3 | ⬜ Not Started |
| 6 | Integrations | 3 | 0 | 3 | ⬜ Not Started |
| 7 | Security Hardening & Review | 3 | 0 | 3 | ⬜ Not Started |
| **Overall** | | **28** | **19** | **9** | 🟡 68% |

---

## Phase 0: Project Setup

- **Status**: ✅ Complete

| ID | Task | Status |
|----|------|--------|
| SETUP-01 | Add Maven dependencies (spring-boot-starter-data-jpa, mysql-connector-j, spring-boot-starter-validation, jjwt-api/impl/jackson) | ✅ Completed (2026-06-01) |
| SETUP-02 | Configure `application.yaml` (MySQL datasource, JPA/Hibernate, JWT secret/expiry, server port, logging) | ✅ Completed (2026-06-01) |
| SETUP-03 | Write `compose.yaml` with MySQL 8.0 service (health check, volume, credentials) | ✅ Completed (2026-06-01) |
| SETUP-04 | Create base package structure (`controller`, `service`, `repository`, `entity`, `dto`, `config`, `event`, `exception`) | ✅ Completed (2026-06-01) |

---

## Phase 1: Database & Entities

- **Status**: ✅ Complete

| ID | Task | Status |
|----|------|--------|
| DB-01 | Create `User` entity (`id`, `email`, `passwordHash`, `role`) + `UserRepository` | ✅ Completed (2026-06-01) |
| DB-02 | Create `Staff` entity (`id`, `name`, `userId` FK) + `StaffRepository` | ✅ Completed (2026-06-01) |
| DB-03 | Create `Service` entity (`id`, `name`, `price`, `durationMinutes`) + `ServiceRepository` | ✅ Completed (2026-06-01) |
| DB-04 | Create `Appointment` entity (`id`, `clientName`, `startTime`, `endTime`, `status`, `type`, `serviceId` FK, `staffId` FK, `createdAt`, `updatedAt`) + `AppointmentRepository` | ✅ Completed (2026-06-01) |

---

## Phase 2: Authentication

- **Status**: ✅ Complete

| ID | Task | Status |
|----|------|--------|
| AUTH-01 | Create JWT utility (`JwtUtil` — generate token, validate token, extract claims) | ✅ Completed (2026-06-01) |
| AUTH-02 | Create `POST /api/auth/login` endpoint + `AuthService` (validate credentials, return JWT) | ✅ Completed (2026-06-01) |
| AUTH-03 | Create `SecurityConfig` — Spring Security filter chain: permit `/api/auth/**`, require JWT on all other endpoints, stateless session | ✅ Completed (2026-06-01) |

---

## Phase 3: Services & Staff

- **Status**: ✅ Complete

| ID | Task | Status |
|----|------|--------|
| STAFF-01 | Create `GET /api/services` endpoint — return all services (id, name, price, durationMinutes) | ✅ Completed (2026-06-02) |
| STAFF-02 | Create `GET /api/staff` endpoint — return all active staff (id, name) | ✅ Completed (2026-06-02) |

---

## Phase 4: Appointments CRUD

- **Status**: ✅ Complete

| ID | Task | Status |
|----|------|--------|
| APPT-01 | Create `GET /api/appointments` — query by `startDate`/`endDate`, return appointment list | ✅ Completed (2026-06-02) |
| APPT-02 | Create `POST /api/appointments` — create appointment/LOCK with date/time overlap validation (database-level: same staff, overlapping range, non-cancelled). Publish `AppointmentEvent` on success | ✅ Completed (2026-06-02) |
| APPT-03 | Create `GET /api/appointments/{id}` — return single appointment details | ✅ Completed (2026-06-02) |
| APPT-04 | Create `DELETE /api/appointments/{id}` — soft delete (status → CANCELLED). Publish `AppointmentEvent` | ✅ Completed (2026-06-02) |
| APPT-05 | Create `AppointmentEvent` and async event publisher configuration (`@EnableAsync`, event listener skeleton) | ✅ Completed (2026-06-02) |
| APPT-06 | Create DTOs (`AppointmentRequest`, `AppointmentResponse`, `AppointmentEventPayload`) | ✅ Completed (2026-06-02) |

---

## Phase 5: Real-Time Streaming

- **Status**: ⬜ Not Started

| ID | Task | Status |
|----|------|--------|
| SSE-01 | Create `GET /api/stream/appointments` SSE endpoint — open `SseEmitter`, register in active connections pool | ⬜ Pending |
| SSE-02 | Create `AppointmentEventListener` — listens to `AppointmentEvent`, serializes to JSON, writes to all active `SseEmitter` connections | ⬜ Pending |
| SSE-03 | Handle connection lifecycle — remove dead emitters on error/completion, heartbeat to keep alive | ⬜ Pending |

---

## Phase 6: Integrations

- **Status**: ⬜ Not Started

| ID | Task | Status |
|----|------|--------|
| INT-01 | Create `GET /api/appointments/{id}/whatsapp-link?action=SEND_FORM|CONFIRM` — return pre-formatted `wa.me` URL with appointment details | ⬜ Pending |
| INT-02 | Create FCM push notification listener — receives `AppointmentEvent`, sends push to relevant iOS devices (device token storage TBD) | ⬜ Pending |
| INT-03 | Create FCM configuration and device token storage skeleton | ⬜ Pending |

---

## Phase 7: Security Hardening & Review

- **Status**: ⬜ Not Started

| ID | Task | Status |
|----|------|--------|
| SEC-01 | Role-based access control: define roles (ADMIN, STAFF, RECEPTIONIST), enforce on endpoints via `@PreAuthorize` or URL-based rules | ⬜ Pending |
| SEC-02 | Input validation review — ensure all endpoints validate input (DTO validation, path variables, query params). Add `@Valid` everywhere | ⬜ Pending |
| SEC-03 | Final security audit — no hardcoded secrets, no PII in logs, CORS restrictive, rate limiting on auth endpoint, SQL injection review (JPA parameterized queries) | ⬜ Pending |

---

## Task Dependency Graph

```
Phase 0 ──► Phase 1 ──► Phase 2 ──► Phase 3
                                    │
                                    ▼
                               Phase 4 ──► Phase 5
                               │    │
                               │    └──► Phase 6
                               ▼
                          Phase 7 (can run in parallel with 5/6)
```

Phases 0–2 are sequential. Phases 3–6 can partially overlap once Phase 2 auth is done. Phase 7 is the final gate.
