# Events_and_Resources
 
**Subsystem 3 of MasonCareerLaunch: Career Events & Resources**
 
The discovery layer of the platform: students find upcoming career fairs, workshops, and info sessions, register for them, and browse curated prep resources. Career Services staff publish the content; students use it.
 
📘 **Full documentation is in the** [**Wiki**](../../wiki): architecture, interface contract, sprint plans, and acceptance criteria.
 
📄 **Current interface contract:** [**Interface Contract v9**](../../wiki/Interface-Contract-v9) (updated 2026-11-24)
 
---
 
## Current status (Sprint 5, Nov 24)
 
Every Sprint 5 feature is merged into `main`. Since code complete on Nov 20, only bug fixes, tests and docs go in.
 
The website has a **navigation bar** with four tabs: **Home** (one search box for events, jobs and resources), **Events** (calendar / list), **Resources** and **Jobs & Internships**. The tab comes from the `#` part of the URL (`#home`, `#events`, `#resources`, `#jobs`), so refresh and Back/Forward work.
 
**Done**
 
| Feature | Status |
|---|---|
| Navigation bar + Home page with one search for events, jobs and resources (MASON-50, MASON-10) | ✅ Done |
| Events tab: green hero with search and category chips, calendar/list, "Coming up" and "Your events" side cards | ✅ Done |
| Login bar on the page (`POST /session/login`, `GET /session/me`); staff buttons follow your real roles | ✅ Done |
| Create Event page (form) | ✅ Saves to the real backend (staff or admin) |
| Events calendar (day / week / month) + list view | ✅ Done |
| **My events** switch on the calendar (`GET /users/me/events/registered`, MASON-9) | ✅ Done |
| Event pop-up with **Register** / **Cancel registration** | ✅ Done |
| **Registration confirmation** sent through Notifications (also when someone gets a seat from the waitlist). If Notifications is down, registering still works | ✅ Done |
| **Remind me the day before** toggle (`PUT /events/{id}/register/reminder`, MASON-24) | ✅ Done. Notifications reads `GET /registrations/reminders` and sends it |
| **Waitlist** for full events: join / leave, place in line (MASON-18) | ✅ Done |
| Staff/admin waitlist panel + **change capacity**; open seats go to the waitlist automatically (MASON-19) | ✅ Done |
| Jobs & internships page: cards, pop-up, staff post / edit / delete (`/opportunities`, MASON-13) | ✅ Done |
| Resources directory from the real database (`GET /resources`: search, major and type filters, MASON-12) | ✅ Done |
| **Share a resource** + **My submissions** with a status badge; edit/delete while pending (MASON-7) | ✅ Done |
| Staff/admin **review queue**: approve, deny or put on hold, with a note the student sees (MASON-11) | ✅ Done |
| `GET /events` with filters `from`, `to`, `category`, `audience`, `format`, keyword `q` (MASON-10), and `GET /events/{id}` | ✅ Done |
| `POST /events` (staff or admin) | ✅ Done, returns the saved event |
| `POST` / `DELETE` / `GET /events/{id}/register`, `GET /users/{userId}/events/registered` | ✅ Done (capacity checked in one SQL statement, so two students can't take the last seat) |
| User ids are Shared Core's **UUIDs**; roles are a lowercase list (`student`, `mentor`, `staff`, `admin`) | ✅ Done |
| Real Shared Core login (`AUTH_MODE=shared-core`); test users are still the default | ✅ Done |
| Standard error format `{"error", "code", "detail"}` | ✅ Done |
| Docker: API + page + PostgreSQL with `docker compose up --build` | ✅ Done |
| CI (`lint-and-test`): backend tests, ESLint, Vite build, Vitest, Docker build, **and the tests again inside the Docker image** | ✅ Runs on every PR into `main` |
 
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
 
Running other subsystems on your laptop without the integration compose? We expect **Shared Core on 8081** (`SHARED_CORE_URL`) and **Notifications on 8082** (`NOTIFICATIONS_URL`).
 
| Command | What it does |
|---|---|
| `docker compose down` | Stop (keeps the data) |
| `docker compose down -v` | Stop and wipe the database (do this after a `schema.sql` change) |
| `AUTH_MODE=shared-core docker compose up --build` | Real Shared Core logins (Shared Core running on your laptop's port 8081) |
 
The page starts **logged out**: use the login bar with one of the [test users](#test-users) below.
 
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
 
> ⚠️ **After pulling a change to `schema.sql`** that changes an existing table (like Sprint 4's switch to UUID user ids), recreate your database once. `CREATE TABLE IF NOT EXISTS` won't change a table that already exists. (Sprint 5's new `waitlist` table, `remind` column and resource columns are added automatically, so no reset is needed for those.)
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
| `VITE_DEV_TOKEN=staff-token npm run dev` | Starts already logged in as staff (otherwise use the login bar) |
| `npm run build` | Builds the production version into `frontend/dist/` |
| `npm run lint` | Checks code style with ESLint |
| `npm test` | Runs the frontend tests (Vitest) |
 
**Backend tests:** from `backend/`, run `./mvnw verify` (needs the database running).
 
**Try the API quickly** (backend running):
 
```bash
curl "http://localhost:8080/events?category=Workshop&format=In-person"
curl "http://localhost:8080/events?from=2026-11-01&to=2026-11-30"
curl "http://localhost:8080/events?q=resume"
curl -X POST   http://localhost:8080/events/3/register -H "Authorization: Bearer student-token"
curl           http://localhost:8080/events/3/register -H "Authorization: Bearer student-token"
curl -X DELETE http://localhost:8080/events/3/register -H "Authorization: Bearer student-token"
curl http://localhost:8080/users/00000000-0000-0000-0000-000000000003/events/registered -H "Authorization: Bearer student-token"
curl           http://localhost:8080/users/me/events/registered -H "Authorization: Bearer student-token"
 
# Log in, reminders, waitlist, jobs
curl -X POST http://localhost:8080/session/login -H "Content-Type: application/json" -d '{"email":"staff@gmu.edu","password":"ChangeMe123!"}'
curl -X PUT  http://localhost:8080/events/3/register/reminder -H "Authorization: Bearer student-token" -H "Content-Type: application/json" -d '{"remind":true}'
curl         http://localhost:8080/registrations/reminders -H "Authorization: Bearer staff-token"
curl -X POST http://localhost:8080/events/3/waitlist -H "Authorization: Bearer student2-token"   # only works when the event is full
curl         http://localhost:8080/events/3/waitlist -H "Authorization: Bearer staff-token"
curl -X PUT  http://localhost:8080/events/3/capacity -H "Authorization: Bearer staff-token" -H "Content-Type: application/json" -d '{"capacity":35}'
curl         http://localhost:8080/opportunities
 
# Resources: share, see my submissions, review (staff), browse
curl -X POST http://localhost:8080/resources -H "Authorization: Bearer student-token" -H "Content-Type: application/json" -d '{"title":"My interview tips","type":"Interview Questions","majors":["CS Majors"],"link":"https://example.com"}'
curl         http://localhost:8080/users/me/resources -H "Authorization: Bearer student-token"
curl         "http://localhost:8080/resources?status=pending" -H "Authorization: Bearer staff-token"
curl -X PUT  http://localhost:8080/resources/1/status -H "Authorization: Bearer staff-token" -H "Content-Type: application/json" -d '{"status":"approved"}'
curl         "http://localhost:8080/resources?major=CS%20Majors&q=interview"
```
 
### Test users
 
`AUTH_MODE=fake`. Same UUIDs, roles and password as Shared Core's seed users. Log in on the page with the email + password, or send the token as `Authorization: Bearer <token>`.
 
| Email | Password | Token | `userId` | Roles |
|---|---|---|---|---|
| jdoe@gmu.edu | `ChangeMe123!` | `student-token` | `00000000-0000-0000-0000-000000000003` | `student` |
| grad@gmu.edu | `ChangeMe123!` | `student2-token` | `00000000-0000-0000-0000-000000000004` | `student`, `mentor` |
| staff@gmu.edu | `ChangeMe123!` | `staff-token` | `00000000-0000-0000-0000-000000000002` | `staff` |
| admin@gmu.edu | `ChangeMe123!` | `admin-token` | `00000000-0000-0000-0000-000000000001` | `admin` |
 
---
 
## Project structure
 
```
Events_and_Resources/
├── Dockerfile                     # 3 stages: build frontend -> build backend -> run app.jar on :8080
├── docker-compose.yml             # app + PostgreSQL ("docker compose up --build")
├── .github/workflows/ci.yml       # CI: the "lint-and-test" check on every PR
├── .github/workflows/publish.yml  # Publishes our Docker image to GHCR on every merge to main
├── frontend/                      # React app (Vite)
│   └── src/
│       ├── App.jsx                # NavBar + LoginBar, then one page at a time from the URL's "#":
│       │                          #   Home, Events, Resources (+ share / My submissions / review), Jobs
│       ├── api/
│       │   ├── auth.js            # Login, the saved token, GET /session/me
│       │   ├── currentUser.js     # Old dev-token user (VITE_DEV_TOKEN)
│       │   ├── eventsApi.js       # Events + registration + reminder calls (fetch + token)
│       │   ├── myEventsApi.js     # "My events" for the calendar
│       │   ├── waitlistApi.js     # Join / leave waitlist, see waitlist, change capacity
│       │   ├── opportunitiesApi.js  # Jobs & internships (/opportunities)
│       │   ├── resourcesApi.js    # Browse, share, my submissions, review queue (/resources)
│       │   ├── mockEventsApi.js   # localStorage version, used with VITE_USE_MOCK=true
│       │   └── *.test.js          # Vitest tests
│       ├── auth/
│       │   ├── AuthProvider.jsx   # Keeps who is logged in for the whole page
│       │   └── useAuth.js         # useAuth(): who is logged in, isStaff
│       ├── components/
│       │   ├── NavBar.jsx         # Top bar with the page tabs
│       │   ├── LoginBar.jsx       # Email + password form / "Logged in as ..."
│       │   ├── EventCard.jsx      # One event in the list
│       │   ├── EventDetailModal.jsx  # Event pop-up (details + spots + Register / Cancel)
│       │   ├── EventSidePanels.jsx   # Events tab side cards: "Coming up" + "Your events"
│       │   ├── RegisterButton.jsx # Register / Cancel / Join waitlist and their states
│       │   ├── ReminderToggle.jsx # "Remind me the day before"
│       │   ├── WaitlistPanel.jsx  # Staff box in the pop-up: the line + change capacity
│       │   ├── JobCard.jsx, JobDetailModal.jsx, JobForm.jsx  # Jobs page pieces
│       │   ├── JobsPanel.jsx      # Jobs & internships side panel
│       │   ├── ResourceCard.jsx   # One resource on the Resources page
│       │   ├── ShareResourceModal.jsx  # "Share a resource" pop-up (MASON-7)
│       │   ├── MyResourcesPanel.jsx    # "My submissions" with status badges (MASON-7)
│       │   ├── ResourceReviewPanel.jsx # Staff review queue: approve / deny / hold (MASON-11)
│       │   └── UpcomingEventsPanel.jsx # Next events for the chosen major
│       ├── models/
│       │   └── Event.js           # The Event object and its fields
│       ├── pages/
│       │   ├── HomePage.jsx         # Home: one search box + previews
│       │   ├── EventsHubPage.jsx    # Events tab: hero, search, calendar/list, side cards, Create Event
│       │   ├── CreateEventPage.jsx  # Form for creating an event (staff/admin)
│       │   ├── CalendarPage.jsx     # Day / week / month calendar + "My events" switch
│       │   ├── EventsPage.jsx       # List of upcoming events
│       │   ├── JobsPage.jsx         # Jobs & internships
│       │   └── ResourcesPage.jsx    # Resource directory
│       └── utils/
│           ├── calendarDates.js   # Date math for the calendar
│           ├── eventSearch.js     # Keyword matching for the Events tab search
│           └── hashRoute.js       # Which page to show from the URL (#home, #events, #resources, #jobs)
├── backend/                       # Spring Boot (Java 17, Maven), layered architecture
│   └── src/
│       ├── main/java/edu/gmu/cs321/events_resources/
│       │   ├── controller/        # HTTP endpoints: Event, Registration, Waitlist, Session, Opportunity, Resource
│       │   ├── service/           # Business rules: Event, Registration (+ RegistrationConfirmation), Waitlist,
│       │                          #   Opportunity, Resource
│       │   ├── repository/        # SQL (JdbcTemplate): Event, Registration, Waitlist, Opportunity, Resource
│       │   ├── model/             # Data shapes (records)
│       │   ├── common/            # Error format, auth check, paging
│       │   └── integration/       # Other subsystems: Shared Core token check + login (real and fake),
│       │                          #   NotificationsClient
│       ├── main/resources/
│       │   ├── application.properties  # Database, AUTH_MODE, SHARED_CORE_URL, NOTIFICATIONS_URL
│       │   ├── schema.sql         # Tables: categories, audiences, events, event_audiences, registrations,
│       │   │                      #   waitlist, resource, opportunity, resource_audiences, opportunity_audiences
│       │   └── data.sql           # Sample categories, audiences, events, and resources
│       └── test/                  # JUnit: EventService, OpportunityService, WaitlistService,
│                                  #   ResourceService, RegistrationConfirmation, registration cancel +
│                                  #   reminders, AuthInterceptor,
│                                  #   Shared Core token check + login, NotificationsClient, app startup
└── docs/
    ├── architecture/              # ER diagram
    └── ai-log/                    # AI usage logs (see below)
```
 
---
## Run Subsystem 3 in the integration docker-compose

Our image is published to GHCR on every merge to `main`:
`ghcr.io/gmu-cs321-fall2026/events_and_resources:latest`
(or pin an exact version with a `sha-xxxxxxx` tag from the Packages page).

   services:
      events-resources:
        image: ghcr.io/gmu-cs321-fall2026/events_and_resources:latest
        ports:
          - "8080:8080"        # left number = your laptop's port; change it if 8080 is taken
        environment:
          DB_URL: jdbc:postgresql://events-db:5432/events
          DB_USER: postgres
          DB_PASSWORD: dev
          AUTH_MODE: shared-core
          # Use the service names from the integration compose file.
          # Inside compose, every service listens on its own 8080.
          SHARED_CORE_URL: http://shared-core:8080
          NOTIFICATIONS_URL: http://notifications:8080
        depends_on:
          events-db:
            condition: service_healthy
      events-db:
        image: postgres:17
        environment:
          POSTGRES_PASSWORD: dev
          POSTGRES_DB: events
        healthcheck:
          test: ["CMD-SHELL", "pg_isready -U postgres"]
          interval: 5s
          timeout: 5s
          retries: 10

   No team needs to change its port: inside compose, services reach each other by service name on 8080. Only the left side of `ports:` has to be unique on your laptop. `shared-core` and `notifications` are placeholders: replace them with the service names in the integration compose.
          
## Event fields (Interface Contract v9)
 
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
 
Full request/response details (registration, reminders, waitlist, login, opportunities, resources) and every error code are in **[Interface Contract v9](../../wiki/Interface-Contract-v9)** on the Wiki.
 
---
 
## Team workflow
 
* **Branches:** each teammate works on their own branch (`Cindy`, `Jean`, `Trang`, `Yasmin`) and opens a **pull request into `main`**.
* **Before starting work,** pull the latest `main` into your branch so you're not working on old code:
```bash
  git checkout <your-branch>
  git pull origin main
```
 
* **Merge conflicts:** if GitHub says your PR has conflicts, keep **both** sides when they're different features. Check `App.jsx` especially, since everyone's pages meet there. After merging, run the site once (`docker compose up --build`) before you push.
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