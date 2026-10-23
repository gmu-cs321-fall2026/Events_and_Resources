# Events_and_Resources

**Subsystem 3 of MasonCareerLaunch: Career Events & Resources**

The discovery layer of the platform: students find upcoming career fairs, workshops, and info sessions, and browse curated prep resources. Career Services staff publish the content; students use it.

📘 **Full documentation is in the** [**Wiki**](../../wiki): architecture, interface contract, sprint plans, and acceptance criteria.

📄 **Current interface contract:** [**Interface Contract v3**](../../wiki/Interface-Contract-v3) (updated 2026-10-20)

---

## Current status (Sprint 3)

**Done in Sprint 2**

| Feature | Status |
|---|---|
| Create Event page (form) | ✅ Working |
| Events list + event detail pop-up | ✅ Working (frontend) |
| Register button in the pop-up | ✅ Working |
| Standard error format `{"error", "code", "detail"}` | ✅ Done |
| Auth: 401 without a token, 403 for non-staff | ✅ Done (test tokens until Shared Core's auth is ready) |
| `POST /events/{id}/register`, `GET /users/{userId}/events/registered` | ✅ Done |
| PostgreSQL database | ✅ Connected (local Docker for now) |

**Sprint 3 (Weeks 7–8): full feature set**

| Feature | Status |
|---|---|
| Update event fields: `tag` → `category`; new `audience`, `format`, `capacity`, `registeredCount` (frontend + database) | ✅ Done (Interface Contract v3) |
| "Spots: x / capacity registered" in the event pop-up | ✅ Done |
| `POST /events` (carried over from Sprint 2) | 🚧 In progress (branch `Cindy`, needs v3 fields) |
| `GET /events` with filters, `GET /events/{id}` | 🚧 In progress |
| Event calendar (day / week / month view) | 📋 Planned |
| Q&I report | 📋 Planned |
| Docker: add backend + database to the container | ⚠️ Still frontend only |

> **Note:** Until `POST /events` and `GET /events` are merged, run the frontend with `VITE_USE_MOCK=true npm run dev` to save events in your **browser's localStorage** instead of the server. Those events only show up in the browser you made them in.

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
   The backend runs at **http://localhost:8080**. Tables and three sample events are created at startup.

> ⚠️ **After pulling the v3 field changes,** recreate your database once. `CREATE TABLE IF NOT EXISTS` won't add new columns to a table that already exists:
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

Then open the link Vite prints (usually **http://localhost:5173**).

| Command (inside `frontend/`) | What it does |
|---|---|
| `npm run dev` | Starts the dev server; the page reloads when you save a file |
| `VITE_USE_MOCK=true npm run dev` | Same, but uses localStorage instead of the backend |
| `VITE_DEV_TOKEN=staff-token npm run dev` | Acts as Career Services Staff (needed to create events) |
| `npm run build` | Builds the production version into `frontend/dist/` |
| `npm run lint` | Checks code style with ESLint |

**Test tokens** (until Shared Core's auth is ready)

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
├── frontend/                      # React app (Vite)
│   └── src/
│       ├── App.jsx                # Main page: shows CreateEventPage + EventsPage
│       ├── api/
│       │   ├── eventsApi.js       # The ONE place that calls the backend (fetch + token)
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
│       └── resources/
│           ├── application.properties  # Database connection settings
│           ├── schema.sql         # Creates the tables
│           └── data.sql           # Three sample events
└── docs/
    ├── architecture/              # Architecture doc and sketch
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
| `category` | `"Workshop"` | Required. **Renamed from `tag`.** `Internal`, `Career Fair`, `Workshop`, `Employer Info Session`, `Networking`, or `Panel` |
| `audience` | `"All Students"` | Required. Who the event is for |
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
