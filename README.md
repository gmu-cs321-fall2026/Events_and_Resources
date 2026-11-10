# Events_and_Resources
 
**Subsystem 3 of MasonCareerLaunch: Career Events & Resources**
 
The discovery layer of the platform: students find upcoming career fairs, workshops, and info sessions, register for them, and browse curated prep resources. Career Services staff publish the content; students use it.
 
📘 **Full documentation is in the** [**Wiki**](../../wiki): architecture, interface contract, sprint plans, and acceptance criteria.
 
📄 **Current interface contract:** [**Interface Contract v6**](../../wiki/Interface-Contract-v6) (updated 2026-11-10)
 
---
 
## Current status (Sprint 4)
 
**Done**
 
| Feature | Status |
|---|---|
| Create Event page (form) | ✅ Saves to the real backend (staff or admin) |
| Events calendar (day / week / month) + list view | ✅ Done (Sprint 4) |
| Event pop-up with **Register** / **Cancel registration** | ✅ Done (Sprint 4) |
| Resources page (search, major / class-year chips, resource cards, upcoming events panel) | ✅ Done; resources are sample data until `GET /resources` exists |
| `GET /events` with filters `from`, `to`, `category`, `audience`, `format`, and `GET /events/{id}` | ✅ Done |
| `POST /events` (staff or admin) | ✅ Done, returns the saved event |
| `POST /events/{id}/register`, `GET /users/{userId}/events/registered` | ✅ Done (capacity checked in one SQL statement, so two students can't take the last seat) |
| `DELETE /events/{id}/register` (cancel), `GET /events/{id}/register` (am I registered?) | ✅ Done (Sprint 4) |
| User ids are Shared Core's **UUIDs**; roles are a lowercase list (`student`, `mentor`, `staff`, `admin`) | ✅ Done (Sprint 4) |
| Real Shared Core login (`AUTH_MODE=shared-core`), test tokens still the default | ✅ Done (Sprint 4) |
| Standard error format `{"error", "code", "detail"}` | ✅ Done |
| Docker: API + page + PostgreSQL with `docker compose up --build` | ✅ Done |
| CI (`lint-and-test`): backend tests, ESLint, Vite build, Vitest, Docker build, **and the tests again inside the Docker image** | ✅ Runs on every PR into `main` |
 
**Still to do (Sprint 5)**
 
| Feature | Status |
|---|---|
| Keyword search `?q=` on `GET /events` | 🚧 In progress |
| `GET /opportunities` + Jobs panel (MASON-13) | 🚧 In progress; panel shows placeholders |
| `GET /resources`, submit a resource, staff approval | 📋 Planned |
| "My events" personal calendar (MASON-9) | 📋 Planned |
| Registration confirmation + reminders through Notifications | 📋 Planned |
| `PUT /events/{id}`, `DELETE /events/{id}` (staff or admin) | 📋 Planned |
| Waitlist for full events | 📋 Stretch |
 
> **Mock mode:** `VITE_USE_MOCK=true npm run dev` still works if you want to try the frontend without the backend. Events are saved in your **browser's localStorage** and only show up in that browser.
 
---
 
## Running the app
 
### Option 1: Docker (everything, no Java or Node needed)
 
You only need [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running. From the repo root:
 
```bash
docker compose up --build     # first build: 5–10 min, wait for "Started EventsResourcesApplication"
```
 
Then open **http://localhost:8080** (the page and the API are on the same port).
 
If 8080 is busy, change the left number in `docker-compose.yml`, e.g. `"8083:8080"`, and open `http://localhost:8083`.
 
| Command | What it does |
|---|---|
| `docker compose down` | Stop (keeps the data) |
| `docker compose down -v` | Stop and wipe the database (do this after a `schema.sql` change) |
| `AUTH_MODE=shared-core docker compose up --build` | Real Shared Core logins (Shared Core running on your laptop's port 8081) |
 
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
   The backend runs at **http://localhost:8080**. At startup, `schema.sql` creates the tables and `data.sql` adds the categories, audiences, and sample events.
 
> ⚠️ **After pulling a change to `schema.sql`** (like Sprint 4's switch to UUID user ids), recreate your database once. `CREATE TABLE IF NOT EXISTS` won't change a table that already exists:
> ```bash
> docker rm -f events-db
> docker run --name events-db -e POSTGRES_PASSWORD=dev -e POSTGRES_DB=events -p 5432:5432 -d postgres:17
> ```
 
**Login check (`AUTH_MODE`)**
 
| Run with | Tokens are checked by |
|---|---|
| `./mvnw spring-boot:run` | The 4 test tokens below (`AUTH_MODE=fake`, the default) |
| `AUTH_MODE=shared-core ./mvnw spring-boot:run` | Shared Core's `GET /auth/validate`. Start Shared Core first on port 8081: `SERVER_PORT=8081 ./mvnw spring-boot:run` in the Shared-Core folder |
 
Shared Core somewhere else? Set `SHARED_CORE_URL`, e.g. `SHARED_CORE_URL=http://localhost:9000`.
 
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
curl "http://localhost:8080/events?from=2026-11-01&to=2026-11-30"
curl -X POST   http://localhost:8080/events/3/register -H "Authorization: Bearer student-token"
curl           http://localhost:8080/events/3/register -H "Authorization: Bearer student-token"
curl -X DELETE http://localhost:8080/events/3/register -H "Authorization: Bearer student-token"
curl http://localhost:8080/users/00000000-0000-0000-0000-000000000003/events/registered -H "Authorization: Bearer student-token"
```
 
**Test tokens** (`AUTH_MODE=fake`). Same UUIDs and roles as Shared Core's seed users.
 
| Token | User | `userId` | Roles |
|---|---|---|---|
| `student-token` (default) | jdoe@gmu.edu | `00000000-0000-0000-0000-000000000003` | `student` |
| `student2-token` | grad@gmu.edu | `00000000-0000-0000-0000-000000000004` | `student`, `mentor` |
| `staff-token` | staff@gmu.edu | `00000000-0000-0000-0000-000000000002` | `staff` |
| `admin-token` | admin@gmu.edu | `00000000-0000-0000-0000-000000000001` | `admin` |
 
---
 
## Project structure
 
```
Events_and_Resources/
├── Dockerfile                     # 3 stages: build frontend -> build backend -> run app.jar on :8080
├── docker-compose.yml             # app + PostgreSQL ("docker compose up --build")
├── .github/workflows/ci.yml       # CI: the "lint-and-test" check on every PR
├── frontend/                      # React app (Vite)
│   └── src/
│       ├── App.jsx                # Main page: Resources, Create Event, Calendar / List switch
│       ├── api/
│       │   ├── eventsApi.js       # Calls the events + registration endpoints (fetch + token)
│       │   ├── resourcesApi.js    # Resources page data (sample resources until GET /resources)
│       │   ├── mockEventsApi.js   # localStorage version, used with VITE_USE_MOCK=true
│       │   └── *.test.js          # Vitest tests
│       ├── components/
│       │   ├── EventCard.jsx      # One event in the list
│       │   ├── EventDetailModal.jsx  # Event pop-up (details + spots + Register / Cancel)
│       │   ├── RegisterButton.jsx # Register / Cancel button and its states (full, registered, ...)
│       │   ├── ResourceCard.jsx   # One resource on the Resources page
│       │   ├── UpcomingEventsPanel.jsx  # Next events for the chosen major
│       │   └── JobsPanel.jsx      # Jobs & internships (placeholder until GET /opportunities)
│       ├── models/
│       │   └── Event.js           # The Event object and its fields
│       ├── pages/
│       │   ├── CreateEventPage.jsx  # Form for creating an event
│       │   ├── CalendarPage.jsx     # Day / week / month calendar
│       │   ├── EventsPage.jsx       # List of upcoming events
│       │   └── ResourcesPage.jsx    # Resource directory
│       └── utils/
│           └── calendarDates.js   # Date math for the calendar
├── backend/                       # Spring Boot (Java 17, Maven), layered architecture
│   └── src/
│       ├── main/java/edu/gmu/cs321/events_resources/
│       │   ├── controller/        # HTTP endpoints (EventController, RegistrationController)
│       │   ├── service/           # Business rules
│       │   ├── repository/        # SQL (JdbcTemplate)
│       │   ├── model/             # Data shapes (records)
│       │   ├── common/            # Error format, auth check, paging
│       │   └── integration/       # Other subsystems: Fake / SharedCore token validators
│       ├── main/resources/
│       │   ├── application.properties  # Database + AUTH_MODE settings
│       │   ├── schema.sql         # Tables: categories, audiences, events, event_audiences, registrations,
│       │   │                      #         resource, opportunity, resource_audiences, opportunity_audiences
│       │   └── data.sql           # Sample categories, audiences, and events
│       └── test/                  # JUnit: EventService, cancel registration, AuthInterceptor,
│                                  #        SharedCoreTokenValidator, app startup
└── docs/
    ├── architecture/              # Architecture doc, sketch, and ER diagram
    └── ai-log/                    # AI usage logs (see below)
```
 
---
 
## Event fields (Interface Contract v6)
 
| Field | Example | Notes |
|---|---|---|
| `eventTitle` | `"Resume Workshop"` | Required |
| `host` | `"Career Services"` | Required. Employer or office running it |
| `date` | `"2026-11-20"` | Required. `YYYY-MM-DD` |
| `time` | `"15:00"` | Required. 24-hour `HH:mm` |
| `location` | `"Horizon Hall 2400"` | Required |
| `category` | `"Workshop"` | Required. `Internal`, `Career Fair`, `Workshop`, `Employer Info Session`, `Networking`, or `Panel` (must be in the `categories` table) |
| `audience` | `"CS Majors, IT Majors"` | Required. Who the event is for. Names that match the `audiences` table are linked so `?audience=` can find the event |
| `format` | `"In-person"` | Required. `In-person` or `Virtual` |
| `registerLink` | URL | Optional; may be `null` |
| `capacity` | `30` | Optional. Max seats; `0` (or blank) = unlimited |
| `registeredCount` | `12` | Set by the server. Seats taken so far |
 
Full request/response details and every error code are in **[Interface Contract v6](../../wiki/Interface-Contract-v6)** on the Wiki.
 
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
* **Contract changes:** if your PR changes an endpoint, update the interface contract on the Wiki within 48 hours.
* **Don't commit IDE files** (`.idea/`, `*.iml`, `.vscode/`) or `node_modules/`.
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
