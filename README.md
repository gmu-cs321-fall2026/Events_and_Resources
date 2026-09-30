# Events\_and\_Resources

**Subsystem 3 of MasonCareerLaunch: Career Events \& Resources**

The discovery layer of the platform: students find upcoming career fairs, workshops, and info sessions, and browse curated prep resources. Career Services staff publish the content; students use it.

📘 **Full documentation is in the** [**Wiki**](../../wiki): architecture, interface contract, sprint plans, and acceptance criteria.

\---

## Current status (Sprint 1)

|Feature|Status|
|-|-|
|Create Event page (form)|✅ Working|
|Events list page|✅ Working|
|Event model (`Event.js`)|✅ Done|
|Docker (frontend)|✅ Working|
|Java backend / HTTP API (`GET /events`, `POST /events`)|🚧 In progress|

> \\\*\\\*Note:\\\*\\\* For now, events are saved in your \\\*\\\*browser's localStorage\\\*\\\*, not a database. Events you create show up only in the browser you made them in, and clearing browser data deletes them. This changes once the backend API is connected.

\---

## Running the app

### Option 1: Docker (recommended)

You only need [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running.

```bash
docker build -t events-resources .
docker run -p 5173:5173 events-resources
```

Then open **http://localhost:5173**.

### Option 2: Without Docker

You need [Node.js](https://nodejs.org/) 22 or newer.

```bash
cd frontend
npm install
npm run dev
```

Then open the link Vite prints (usually **http://localhost:5173**).

|Command (inside `frontend/`)|What it does|
|-|-|
|`npm run dev`|Starts the dev server; the page reloads when you save a file|
|`npm run build`|Builds the production version into `frontend/dist/`|
|`npm run lint`|Checks code style with ESLint|

\---

## Project structure

```
Events\\\_and\\\_Resources/
├── Dockerfile                 # Builds and runs the React frontend
├── frontend/                  # React app (Vite)
│   └── src/
│       ├── App.jsx            # Main page: shows CreateEventPage + EventsPage
│       ├── api/
│       │   └── eventsApi.js   # getEvents() and createEvent(); localStorage for now
│       ├── components/
│       │   └── EventCard.jsx  # Shows one event in the list
│       ├── models/
│       │   └── Event.js       # The Event object and its fields
│       └── pages/
│           ├── CreateEventPage.jsx / .css   # Form for creating an event
│           └── EventsPage.jsx               # List of upcoming events
├── src/                       # Java backend (layered architecture, in progress)
│   ├── Main.java              # Entry point
│   ├── routers/               # HTTP endpoints
│   ├── services/              # Business logic
│   ├── models/                # Java data classes
│   ├── db/                    # Data storage
│   └── integrations/          # Calls to other subsystems (Shared Core, Notifications, Admin)
├── test/                      # Tests
└── docs/
    ├── architecture/          # Architecture doc and sketch
    └── ai-log/                # AI usage logs (see below)
```

\---

## Event fields

|Field|Example|Notes|
|-|-|-|
|`eventTitle`|`"Resume Workshop"`|Required|
|`host`|`"Career Services"`|Employer or office running it|
|`date`|`"2026-10-20"`|`YYYY-MM-DD`|
|`time`|`"15:00"`|24-hour `HH:mm`|
|`location`|`"Horizon Hall"`||
|`tag`|`"Workshop"`|`Internal`, `Career Fair`, `Workshop`, `Employer Info Session`, `Networking`, or `Panel`|
|`registerLink`|URL|Optional; not on the form yet|

Full request/response details are in the **Interface Contract v1** page on the Wiki.

\---

## Team workflow

* **Branches:** each teammate works on their own branch (`Cindy`, `Jean`, `Trang`, `Yasmin`) and opens a **pull request into `main`**.
* **Before starting work,** pull the latest `main` into your branch so you're not working on old code:

```bash
  git checkout <your-branch>
  git pull origin main
  ```

* **Don't commit IDE files** (`.idea/`, `\\\*.iml`) or `node\\\_modules/`.

\---

## Team reminder: AI logging

1. \[ ] Create a log file in `docs/ai-log/` for any AI-assisted work, named `MM-DD-topic.md` (e.g. `09-28-create-event-page.md`).
2. \[ ] Use the required 3-section format:

   * **Asked:** the prompt or goal given to the AI.
   * **Produced:** the code or solution the AI generated.
   * **Changed or rejected:** what you modified, added, or threw out.



