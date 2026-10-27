# Events_and_Resources

**Subsystem 3 of MasonCareerLaunch: Career Events & Resources**

The discovery layer of the platform: students find upcoming career fairs, workshops, and info sessions, and browse curated prep resources. Career Services staff publish the content; students use it.

📘 **Full documentation is in the** [**Wiki**](../../wiki): architecture, interface contract, sprint plans, and acceptance criteria.

📄 **Current interface contract:** [**Interface Contract v3**](../../wiki/Interface-Contract-v3) (updated 2026-10-20)

---

## Current status (Sprint 3)

**Done**

| Feature | Status |
|---|---|
| Create Event page (form) | ✅ Saves to the real backend |
| Events list + event detail pop-up + Register button | ✅ Working |
| Standard error format `{"error", "code", "detail"}` | ✅ Done |
| Auth: 401 without a token, 403 for non-staff | ✅ Done (using test tokens) |
| `POST /events/{id}/register`, `GET /users/{userId}/events/registered` | ✅ Done (capacity checked in one SQL statement, so two students can't take the last seat) |
| Event fields: `tag` → `category`; `audience`, `format`, `capacity`, `registeredCount` | ✅ Done (Interface Contract v3) |
| `POST /events` (Career Services Staff only) | ✅ Done, returns the saved event |
| `GET /events` with filters `from`, `to`, `category`, `audience`, `format` | ✅ Done |
| `GET /events/{id}` | ✅ Done |
| Lookup tables: `categories`, `audiences`, `event_audiences` | ✅ Done (the database refuses unknown categories) |
| CI (GitHub Actions `lint-and-test`): backend tests, ESLint, Vite build, Vitest, Docker build | ✅ Runs on every PR into `main` |

**Still to do**

| Feature | Status |
|---|---|
| Event calendar (day / week / month view) | 🚧 In progress |
| Keyword search `?q=` on `GET /events` (Browse search bar) | 📋 Planned |
| `PUT /events/{id}`, `DELETE /events/{id}` (staff only) | 📋 Planned |
| `GET /resources`, `GET /opportunities` | 📋 Planned (tables in progress) |
| Registration confirmation through Notifications | 📋 Planned |
| Real Shared Core auth instead of test tokens | ⏳ Planned |
| Docker: add backend + database to the container | ⚠️ Still frontend only |

> **Mock mode:** `VITE_USE_MOCK=true npm run dev` still works if you want to try the frontend without the backend. Events are saved in your **browser's localStorage** and only show up in that browser.

---

## Running the app

### Option 1: Docker (frontend only)

You only need [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running.

```bash
docker build -t events-resources .
docker run -p 5173:5173 events-resources
```

Then open **http://localhost:5173**.

### Option 2: Development (backend + frontend)

**Backend** (Spring Boot + PostgreSQL, needs Java 17 and Docker)

1. Start the database once:
   ```bash
   docker run --name events-db -e POSTGRES_PASSWORD=dev -e POSTGRES_DB=events -p 5432:5432 -d postgres:17
   ```
2. From `backend/`:
   ```bash
   ./mvnw spring-boot:run
   ```
   The backend runs at **http://localhost:8080**. At startup, `schema.sql` creates the tables and `data.sql` adds the categories, audiences, and seven sample events (Oct–Dec 2026).

> ⚠️ **After pulling a change to `schema.sql`** (like the new `categories` / `audiences` tables), recreate your database once. `CREATE TABLE IF NOT EXISTS` won't change a table that already exists:
> ```bash
> docker rm -f events-db
> docker run --name events-db -e POSTGRES_PASSWORD=dev -e POSTGRES_DB=events -p 5432:5432 -d postgres:17
> ```

**Frontend** (needs [Node.js](https://nodejs.org/) 22 or newer)

```bash
cd frontend
npm install
npm run dev
```

Then open the link Vite prints (usually **http://localhost:5173**). Vite sends every `/api/...` request to the backend on port 8080, so start the backend first.

| Command (inside `frontend/`) | What it does |
|---|---|
| `npm run dev` | Starts the dev server; the page reloads when you save a file |
| `VITE_USE_MOCK=true npm run dev` | Same, but uses localStorage instead of the backend |
| `VITE_DEV_TOKEN=staff-token npm run dev` | Acts as Career Services Staff (needed to create events) |
| `npm run build` | Builds the production version into `frontend/dist/` |
| `npm run lint` | Checks code style with ESLint |
| `npm test` | Runs the frontend tests (Vitest) |

**Backend tests:** from `backend/`, run `./mvnw verify` (needs the database running).

**Try the API quickly** (backend running):

```bash
curl "http://localhost:8080/events?category=Workshop&format=In-person"
curl "http://localhost:8080/events?from=2026-10-27&to=2026-11-02"
curl -X POST http://localhost:8080/events/3/register -H "Authorization: Bearer student-token"
```

**Test tokens**

| Token | User |
|---|---|
| `student-token` (default) | Student, id 7 |
| `student2-token` | Student, id 8 |
| `staff-token` | Career Services Staff, id 1 |

---

## Project structure

```
Events_and_Resources/
├── Dockerfile                     # Builds and runs the React frontend
├── .github/workflows/ci.yml       # CI: the "lint-and-test" check on every PR
├── frontend/                      # React app (Vite)
│   └── src/
│       ├── App.jsx                # Main page: shows CreateEventPage + EventsPage
│       ├── api/
│       │   ├── eventsApi.js       # The ONE place that calls the backend (fetch + token)
│       │   ├── eventsApi.test.js  # Vitest tests for eventsApi.js
│       │   └── mockEventsApi.js   # localStorage version, used with VITE_USE_MOCK=true
│       ├── components/
│       │   ├── EventCard.jsx      # One event in the list
│       │   ├── EventDetailModal.jsx  # Event pop-up (details + spots + Register button)
│       │   └── RegisterButton.jsx # Register button and its states (full, registered, ...)
│       ├── models/
│       │   └── Event.js           # The Event object and its fields
│       └── pages/
│           ├── CreateEventPage.jsx / .css   # Form for creating an event
│           └── EventsPage.jsx               # List of upcoming events
├── backend/                       # Spring Boot (Java 17, Maven), layered architecture
│   └── src/main/
│       ├── java/edu/gmu/cs321/events_resources/
│       │   ├── controller/        # HTTP endpoints
│       │   ├── service/           # Business rules
│       │   ├── repository/        # SQL (JdbcTemplate)
│       │   ├── model/             # Data shapes (records)
│       │   ├── common/            # Error format, auth check, paging
│       │   └── integration/       # Calls to other subsystems (Shared Core, Notifications, Admin)
│       ├── test/                  # JUnit tests (EventService, AuthInterceptor, app startup)
│       └── resources/
│           ├── application.properties  # Database connection settings
│           ├── schema.sql         # Creates the tables: categories, audiences, events, event_audiences, registrations
│           └── data.sql           # Sample categories, audiences, and events
└── docs/
    ├── architecture/              # Architecture doc, sketch, and ER diagram
    └── ai-log/                    # AI usage logs (see below)
```

---

## Event fields (Interface Contract v3)

| Field | Example | Notes |
|---|---|---|
| `eventTitle` | `"Resume Workshop"` | Required |
| `host` | `"Career Services"` | Required. Employer or office running it |
| `date` | `"2026-10-20"` | Required. `YYYY-MM-DD` |
| `time` | `"15:00"` | Required. 24-hour `HH:mm` |
| `location` | `"Horizon Hall 2400"` | Required |
| `category` | `"Workshop"` | Required. **Renamed from `tag`.** `Internal`, `Career Fair`, `Workshop`, `Employer Info Session`, `Networking`, or `Panel` (must be in the `categories` table) |
| `audience` | `"CS Majors, IT Majors"` | Required. Who the event is for. Names that match the `audiences` table are linked so `?audience=` can find the event |
| `format` | `"In-person"` | Required. `In-person` or `Virtual` |
| `registerLink` | URL | Optional; may be `null` |
| `capacity` | `30` | Optional. Max seats; `0` (or blank) = unlimited |
| `registeredCount` | `12` | Set by the server. Seats taken so far |

Full request/response details are in **Interface Contract v3** on the Wiki.

---

## Team workflow

* **Branches:** each teammate works on their own branch (`Cindy`, `Jean`, `Trang`, `Yasmin`) and opens a **pull request into `main`**.
* **Before starting work,** pull the latest `main` into your branch so you're not working on old code:

  ```bash
  git checkout <your-branch>
  git pull origin main
  ```

* **CI must pass:** the `lint-and-test` check has to be green before a PR can merge. Run `npm run lint`, `npm test`, and `./mvnw verify` locally first.
* **Commit your own work:** each person commits the code they wrote on their own branch.
* **Don't commit IDE files** (`.idea/`, `*.iml`) or `node_modules/`.

---

## Team reminder: AI logging

1. [ ] Create a log file in `docs/ai-log/` for any AI-assisted work, named `MM-DD-topic.md` (e.g. `09-28-create-event-page.md`).
2. [ ] Use the required 3-section format:

   * **Asked:** the prompt or goal given to the AI.
   * **Produced:** the code or solution the AI generated.
   * **Changed or rejected:** what you modified, added, or threw out.

---

## Where code goes

| You're writing… | Put it in |
|---|---|
| An endpoint | `backend/.../controller/` |
| Business rules | `backend/.../service/` |
| SQL / JdbcTemplate | `backend/.../repository/` |
| Event, Registration, etc. | `backend/.../model/` |
| Calls to Shared Core / Notifications | `backend/.../integration/` |
| Error format, pagination | `backend/.../common/` |
| React | `frontend/` |
