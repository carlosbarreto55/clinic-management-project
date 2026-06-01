# Clinic Room Scheduling API — Specification

**Version:** 1.0.0  
**Context:** Backend API for an internal iOS app managing shared clinic rooms. Prevents double-booking and updates clients in real-time.  
**Tech Stack:** Kotlin, Spring Boot MVC, MySQL.  
**Architecture:** Single-instance monolith. Uses Spring's `@Async` + `ApplicationEventPublisher` for internal messaging, Server-Sent Events (SSE) for active app real-time updates, and Firebase Cloud Messaging (FCM) for background push notifications.

**Project Stage:** Early-stage internal tool. Not designed for millions of concurrent users — correctness, security, and reliability are the priorities.

**Testing Requirements (per feature):**
- Every feature MUST include **unit tests** (business logic, service methods) and **integration tests** (full HTTP round-trip with real MySQL via Testcontainers, or at minimum Spring Boot context with MockMvc).
- Tests must cover happy path, error paths, edge cases, and security enforcement (authentication, authorization).

**Security Requirements (per feature):**
- Every feature MUST pass security verification. No code is accepted without a security review.
- Reviews must check: JWT enforcement, input validation, role-based access, no hardcoded secrets, no PII in logs, SQL injection prevention, and CORS configuration.

---

## 1. Authentication

### `POST /api/auth/login`

Accepts email and password. Returns a JWT token.

- **Request Body (JSON):**
  ```json
  {
    "email": "string",
    "password": "string"
  }
  ```
- **Response 200 (JSON):**
  ```json
  {
    "token": "string"
  }
  ```
- **Errors:** `401 Unauthorized` — invalid credentials.

All subsequent requests require an `Authorization: Bearer <token>` header.

---

## 2. Appointments

### `GET /api/appointments`

Retrieves appointments within a date range.

- **Query Parameters (required):** `startDate`, `endDate` (ISO-8601, e.g. `2026-01-01T00:00:00`)
- **Response 200 (JSON):** Array of appointment objects.
- **Errors:** `400 Bad Request` — missing or invalid date params.

### `POST /api/appointments`

Creates a new appointment or blocks a time slot.

- **Request Body (JSON):**
  ```json
  {
    "clientName": "string | null",
    "serviceId": "long",
    "staffId": "long",
    "startTime": "datetime",
    "endTime": "datetime",
    "type": "APPOINTMENT | LOCK"
  }
  ```
  - `clientName` is optional for `LOCK` type, required for `APPOINTMENT`.
- **Business Logic:**
  - Perform **database-level** date/time range overlap validation to prevent double-booking on the same staff member.
  - On success, publish an internal `AppointmentEvent` (async).
- **Response 201 (JSON):** Created appointment.
- **Errors:** `409 Conflict` — time slot overlaps with an existing booking. `400 Bad Request` — validation failure.

### `GET /api/appointments/{id}`

Retrieves a single appointment by ID.

- **Response 200 (JSON):** Appointment details.
- **Errors:** `404 Not Found`.

### `DELETE /api/appointments/{id}`

Cancels an appointment (soft delete — status change to `CANCELLED`).

- **Business Logic:**
  - On success, publish an internal `AppointmentEvent` (async).
- **Response 200 (JSON):** Updated appointment.
- **Errors:** `404 Not Found`. `409 Conflict` — already cancelled.

---

## 3. Real-Time Streaming

### `GET /api/stream/appointments`

Opens an SSE (Server-Sent Events) connection. The server pushes JSON payloads to the iOS client whenever an appointment is created, modified, or cancelled.

- **Response:** `text/event-stream`
- **Event types:** `APPOINTMENT_CREATED`, `APPOINTMENT_UPDATED`, `APPOINTMENT_CANCELLED`
- **Implementation note:** Driven by Spring's `ApplicationEventPublisher` — the SSE emitter listens to `AppointmentEvent` and broadcasts to all active connections.
- **Auth:** Requires valid JWT (passed via query param or header — TBD).

---

## 4. Services & Staff (Read-Only)

### `GET /api/services`

Retrieves available services.

- **Response 200 (JSON):**
  ```json
  [
    {
      "id": "long",
      "name": "string",
      "price": "decimal",
      "durationMinutes": "int"
    }
  ]
  ```

### `GET /api/staff`

Retrieves active professionals.

- **Response 200 (JSON):**
  ```json
  [
    {
      "id": "long",
      "name": "string"
    }
  ]
  ```

---

## 5. Integrations

### `GET /api/appointments/{id}/whatsapp-link`

Returns a pre-formatted `wa.me` URL injected with appointment details for the iOS app to open.

- **Query Parameter (required):** `action` — enum: `SEND_FORM` | `CONFIRM`
- **Response 200 (JSON):**
  ```json
  {
    "url": "string"
  }
  ```
- **Errors:** `404 Not Found` — appointment doesn't exist. `400 Bad Request` — invalid action.

---

## 6. Database Schema

### `users`
| Column          | Type                          | Constraints |
|-----------------|-------------------------------|-------------|
| id              | BIGINT AUTO_INCREMENT         | PK          |
| email           | VARCHAR(255)                  | UNIQUE, NOT NULL |
| password_hash   | VARCHAR(255)                  | NOT NULL    |
| role            | VARCHAR(50)                   | NOT NULL    |

### `staff`
| Column  | Type                      | Constraints      |
|---------|---------------------------|------------------|
| id      | BIGINT AUTO_INCREMENT     | PK               |
| name    | VARCHAR(255)              | NOT NULL         |
| user_id | BIGINT                    | FK → users.id    |

### `services`
| Column           | Type                      | Constraints |
|------------------|---------------------------|-------------|
| id               | BIGINT AUTO_INCREMENT     | PK          |
| name             | VARCHAR(255)              | NOT NULL    |
| price            | DECIMAL(10,2)             | NOT NULL    |
| duration_minutes | INT                       | NOT NULL    |

### `appointments`
| Column      | Type                          | Constraints             |
|-------------|-------------------------------|-------------------------|
| id          | BIGINT AUTO_INCREMENT         | PK                      |
| client_name | VARCHAR(255)                  | NULLABLE (null for LOCK)|
| start_time  | TIMESTAMP                     | NOT NULL                |
| end_time    | TIMESTAMP                     | NOT NULL                |
| status      | VARCHAR(50)                   | NOT NULL, DEFAULT 'ACTIVE' |
| type        | VARCHAR(50)                   | NOT NULL (APPOINTMENT / LOCK) |
| service_id  | BIGINT                        | FK → services.id        |
| staff_id    | BIGINT                        | FK → staff.id, NOT NULL |
| created_at  | TIMESTAMP                     | NOT NULL, DEFAULT CURRENT_TIMESTAMP |
| updated_at  | TIMESTAMP                     | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP |

---

## 7. Overlap Prevention Logic

When creating an appointment, the system must check for overlapping time ranges at the database level for the same staff member:

```sql
SELECT COUNT(*) FROM appointments
WHERE staff_id = :staffId
  AND status != 'CANCELLED'
  AND start_time < :newEndTime
  AND end_time > :newStartTime
```

If count > 0 → reject with `409 Conflict`.

---

## 8. Internal Event Flow

1. `AppointmentService` creates/modifies/cancels an appointment.
2. Publishes `AppointmentEvent` via `ApplicationEventPublisher` (annotated with `@Async`).
3. **SSE Listener** receives event → writes JSON to all active `SseEmitter` connections.
4. **FCM Listener** receives event → sends push notification to relevant iOS devices.

---

## 9. Pending Decisions / Open Items

- [ ] JWT secret/expiry configuration
- [ ] FCM integration details (device token storage, topic/channel strategy)
- [ ] SSE auth mechanism (query param vs. header)
- [ ] Pagination for `GET /api/appointments`
- [ ] Role-based access (admin vs. staff vs. receptionist)
- [ ] Audit logging requirements
