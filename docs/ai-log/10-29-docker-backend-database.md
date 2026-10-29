AI prompt log: Docker with backend + database

Date: 2026-10-28 Author: Yasmin Mata Tool: Claude (Opus 5.5) Files: Dockerfile, docker-compose.yml, .dockerignore, frontend/src/api/eventsApi.js (BASE_URL only)

Asked

Our Dockerfile only ran the frontend dev server, but the SOW (§6) requires the Dockerfile to build and run the whole service. I asked how long it would take to fix and for help writing the files so docker build / docker run runs the Spring Boot backend, the React frontend, and the PostgreSQL database.

Produced
A multi-stage Dockerfile: Node builds the React app → Maven builds the Spring Boot jar with the React files in static/ → a small Java 17 runtime runs the jar on port 8080. Tests are skipped in the image build because they need a database; CI still runs them.
docker-compose.yml that starts PostgreSQL 17 and our app together, waiting for the database to be ready.
.dockerignore so node_modules, target, .git and docs aren't copied into the build.
A change to BASE_URL in eventsApi.js: "/api" in development (Vite proxy) and "" in the production build (same server, no proxy).

Changed/Rejected
At first, claude gave me a very standard dockerfile that didn't even touch or container so no image would have been created. I had to prompt claude better.