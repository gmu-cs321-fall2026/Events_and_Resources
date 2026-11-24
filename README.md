# Events_and_Resources

**Subsystem 3 of MasonCareerLaunch: Career Events & Resources**

The discovery layer of the platform: students find upcoming career fairs, workshops, and info sessions, register for them, and browse curated prep resources. Career Services staff publish the content; students use it.

📘 **Full documentation is in the [Wiki](../../wiki):** architecture, interface contract (current: v2), sprint plans, and acceptance criteria.

---

## Current status (Sprint 2)

| Feature | Status |
|---|---|
| Create Event page (form) | ✅ Working |
| Events list + event detail pop-up | ✅ Working (frontend) |
| Register button in the pop-up | ✅ Working |
| Standard error format `{"error", "code", "detail"}` | ✅ Done |
| Auth: 401 without a token, 403 for non-staff | ✅ Done (test tokens until Shared Core's auth is ready) |
| `POST /events/{id}/register`, `GET /users/{userId}/events/registered` | ✅ Done |
| PostgreSQL database | ✅ Connected (local Docker for now) |
| `GET /events`, `POST /events`, filters | 🚧 In progress |
| Docker | ⚠️ Frontend only for now; backend + database not containerized yet |

---

## Running the app

### Option 1: Docker (frontend only)

You only need [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running.

```bash
docker build -t events-resources .
docker run -p 5173:5173 events-resources
```

Then open **http://localhost:5173**. The container doesn't include the backend yet, so use mock mode below or run the backend yourself.

### Option 2: Backend (Spring Boot + PostgreSQL)

You need Java 17+ and Docker.

```bash
# 1. Start PostgreSQL once (it keeps running in the background)
docker run --name events-db -e POSTGRES_PASSWORD=dev -e POSTGRES_DB=events -p 5432:5432 -d postgres:17

# 2. Start the backend
cd backend
./mvnw spring-boot:run
```

The backend runs on **http://localhost:8080**. Tables and three sample events are created automatically at startup.
Using a different port or database? Set `DB_URL`, `DB_USER`, `DB_PASSWORD` instead of editing `application.properties`.

### Option 3: Frontend (React + Vite)

You need [Node.js](https://nodejs.org/) 22 or newer.

```bash
cd frontend
npm install
npm run dev                      # talks to the backend on :8080
VITE_USE_MOCK=true npm run dev   # no backend needed: saves to the browser instead
```

Then open the link Vite prints (usually **http://localhost:5173**).

| Command (inside `frontend/`) | What it does |
|---|---|
| `npm run dev` | Starts the dev server; the page reloads when you save a file |
| `npm run build` | Builds the production version into `frontend/dist/` |
| `npm run lint` | Checks code style with ESLint |

### Test tokens (until Shared Core's auth is ready)

Protected endpoints need `Authorization: Bearer <token>`:

| Token | User |
|---|---|
| `student-token` | Student, id 7 |
| `student2-token` | Student, id 8 |
| `staff-token` | Career Services Staff, id 1 |

The frontend sends `student-token` by default. To act as staff: `VITE_DEV_TOKEN=staff-token npm run dev`.

---

## Project structure

```
Events_and_Resources/
├── Dockerfile                    # Builds and runs the React frontend
├── frontend/                     # React app (Vite)
│   └── src/
│       ├── App.jsx               # Main page: CreateEventPage + EventsPage
│       ├── api/
│       │   ├── eventsApi.js      # The ONE place the frontend calls the backend
│       │   └── mockEventsApi.js  # Stand-in backend (VITE_USE_MOCK=true)
│       ├── components/           # EventCard, EventDetailModal, RegisterButton
│       ├── models/Event.js       # The Event object and its fields
│       └── pages/                # CreateEventPage, EventsPage
├── backend/                      # Spring Boot (Java 17, Maven)
│   └── src/main/
│       ├── java/edu/gmu/cs321/events_resources/
│       │   ├── controller/       # Endpoints (HTTP ↔ Java)
│       │   ├── service/          # Business rules
│       │   ├── repository/       # SQL (JdbcTemplate)
│       │   ├── model/            # Data shapes (records)
│       │   ├── common/           # Error format, auth check, pagination
│       │   └── integration/      # Calls to other subsystems (Shared Core, Notifications)
│       └── resources/
│           ├── application.properties  # Database connection settings
│           ├── schema.sql        # Creates the tables
│           └── data.sql          # Sample events
└── docs/
    ├── architecture/             # Architecture doc and sketch
    └── ai-log/                   # AI prompt logs (see below)
```

## Where code goes

| You're writing… | Put it in |
|---|---|
| An endpoint | `backend/.../controller/` |
| Business rules | `backend/.../service/` |
| SQL / JdbcTemplate | `backend/.../repository/` |
| Event, Registration, etc. | `backend/.../model/` |
| Calls to Shared Core / Notifications | `backend/.../integration/` |
| Error format, pagination, auth | `backend/.../common/` |
| React | `frontend/` |

---

## Event fields

| Field | Example | Notes |
|---|---|---|
| `id` | `3` | Set by the server |
| `eventTitle` | `"Resume Workshop"` | Required |
| `host` | `"Career Services"` | Employer or office running it |
| `date` | `"2026-10-20"` | `YYYY-MM-DD` |
| `time` | `"15:00"` | 24-hour `HH:mm` |
| `location` | `"Horizon Hall"` | |
| `tag` | `"Workshop"` | `Internal`, `Career Fair`, `Workshop`, `Employer Info Session`, `Networking`, or `Panel` |
| `registerLink` | URL | Optional |
| `capacity` | `30` | Max seats; `0` = unlimited |
| `registeredCount` | `12` | Set by the server |

Full request/response details are in the **Interface Contract v2** page on the Wiki.

---

## Team workflow

- **Branches:** each teammate works on their own branch (`Cindy`, `Jean`, `Trang`, `Yasmin`) and opens a **pull request into `main`**.
- **Before starting work,** pull the latest `main` into your branch so you're not working on old code:

  ```bash
  git checkout <your-branch>
  git pull origin main
  ```

- **Don't commit** IDE files (`.idea/`, `*.iml`), `node_modules/`, or `backend/target/`.

---

## Team reminder: AI logging

1. Create a log file in `docs/ai-log/` for each commit with AI-assisted work, named `MM-DD-topic.md` (e.g. `10-12-register-button-frontend.md`).
2. Use the required 3-section format:
   - **Asked:** the prompt or goal given to the AI.
   - **Produced:** the code or solution the AI generated.
   - **Changed or rejected:** what you kept, edited, or threw out, **with a reason for each**.
