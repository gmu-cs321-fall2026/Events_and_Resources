# AI Log — Auth (401/403), PostgreSQL setup, registration endpoints

- **Date:** 10/12/2026 · **Author:** Yasmin · **AI tool:** Claude (claude.ai)
- **Commit:** `52475b6` "Add auth (401/403), PostgreSQL setup, and event registration endpoints"
- **Jira:** MASON-34 (auth), MASON-33 (register + registered list)
- **Files:** `model/CurrentUser`, `Registration`, `RegisteredEvent`; `integration/TokenValidator`, `FakeTokenValidator`; `common/AuthInterceptor`, `WebConfig`, `PagedResponse`; `repository/RegistrationRepository`; `service/RegistrationService`; `controller/RegistrationController`; `resources/application.properties`, `schema.sql`, `data.sql`

## Asked
- Build the auth check for MASON-34: 401 with no/bad token, 403 when a non-staff user calls `POST /events`, using a stand-in for Shared Core's login since their API isn't ready.
- Since our team agreed to work separately and merge later, set up a database I can run on my own laptop.
- Build MASON-33: `POST /events/{id}/register` with a capacity check, and `GET /users/{userId}/events/registered`.
- Explain records, Bearer tokens, every method and import in `AuthInterceptor`, `application.properties`, and why the `DataSourceAutoConfiguration` exclude line had to be removed. Add inline comments to every file for teammates.

## Produced
- `CurrentUser` record (id + role) and a `TokenValidator` interface with a `FakeTokenValidator` (tokens `student-token`, `student2-token`, `staff-token`).
- `AuthInterceptor` + `WebConfig`: runs before controllers, public for `GET /events*`, 401/403 otherwise as needed.
- `application.properties` for local PostgreSQL in Docker (with `${DB_URL:...}` overrides), `schema.sql` (`events`, `registrations` with `UNIQUE (event_id, user_id)`), `data.sql` (3 sample events).
- Registration layers: repository (SQL with `?` placeholders; capacity check as one `UPDATE ... WHERE registered_count < capacity` statement), service (404 → already registered → full → save, `@Transactional`), controller (201 on success), `PagedResponse` for the list.

## Changed or rejected
- **`CurrentUser` kept as a record, not a regular class**, to avoid writing a constructor plus getters/setters for two fields. Records are also immutable, so nothing later in a request can change a STUDENT into STAFF, which matters for an auth object.
- **Kept the fake validator behind an interface** so swapping in Shared Core's real `/auth/validate` later only means moving one `@Component`. I asked whether to leave the fake out of my push and decided to keep it, because the interceptor needs a `TokenValidator` or the app won't start, and teammates need the test tokens.
- **Fixed a file the AI's instructions didn't cover:** my `FakeTokenValidator` ended up in `controller/` with no `.java` extension, so only 7 of 8 files compiled. Moved it to `integration/` and renamed it.
- **Database approach changed from the AI's first suggestion.** It first said to wait for teammates' `events` table and `EventRepository`. Because we work separately, I asked for my own local setup instead. To avoid merge conflicts, I kept `tryTakeSeat()` in my own `RegistrationRepository` (not a shared `EventRepository`) and named the list model `RegisteredEvent` instead of `Event`.
- **Kept the `${DB_URL:...}` style settings** after asking whether teammates using different ports would break: each person's `localhost` is their own laptop, and anyone on a different port can override with an environment variable without editing the shared file.
- **Kept the one-statement capacity check** instead of "read count, then update," because two students clicking at once could otherwise both get the last seat. The ticket put race conditions in Sprint 3; this covers it now.
- **Asked for inline comments** in all 6 registration files and in `AuthInterceptor` (every import, every method) so teammates can work in this code later. While adding comments, the AI wrote one condition as a confusing double negative; it was rewritten to a plain `if (registrations.isRegistered(...))` before I used it.
- **How I verified before keeping it:**
  - Server compiled (16 source files) and connected to Postgres (`HikariPool-1 - Added connection`).
  - Auth curl tests: no token → 401, `fake-token` → 401, `student-token` on `POST /events` → 403, `staff-token` passed the check (404 because `POST /events` isn't built on my branch yet), no token on my list → 401.
  - Registration curl tests: register → 201, again → 409 `ALREADY_REGISTERED`, second student on the 1-seat event → 409 `EVENT_FULL`, event 999 → 404, no token → 401, own list → 200 with `registeredCount: 1`, other student's list → 403.
  - The AI also ran the SQL against a real PostgreSQL: re-running `schema.sql`/`data.sql` made no duplicates, and 50 simultaneous seat requests on a 30-seat event let exactly 30 in.
