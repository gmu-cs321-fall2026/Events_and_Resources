# AI Log — Sprint 2 planning + backend restructure

- **Date:** 10/02/2026 – 10/06/2026
- **Author:** Yasmin
- **AI tool:** Claude (claude.ai)
- **Branch / PR:** `Yasmin` → `main`

## Purpose
In order to plan Sprint 2, we need to split the work across 4 team members, and clean up the repo so there is one clear backend (Spring Boot) instead of two (`backend/` and `src-backend/`).

## Prompts (summarized)
1. "What are next steps for sprint 2?
2.. "How would you suggest we delegate the tasks to 4 members?" → asked for Jira tickets, then a spreadsheet
3. "We are thinking of using PostgreSQL, JDBC, and Spring Boot."
4. "How would you recommend structuring this? Since we have two backends I want to make it clearer."
5. "Why was db changed to repository?" / "Could you break down the architecture layout?"
6. Asked what Professor Pettit's clarification means: all subsystem data is stored in a central database connected to Shared Core.
7. "Would you recommend deleting Spring Boot and redoing it inside [src-backend]?"
8. "Give me the commands to do this in terminal." Then pasted terminal output and an error for help debugging.

## What the AI produced
- Sprint 2 priorities: switch from localStorage to real endpoints, fix the Dockerfile, add filters/search, registration, and auth (401/403).
- A 4-person work split by layer, plus a task spreadsheet (`sprint2-task-delegation.xlsx`) and a Jira ticket CSV.
- Recommended keeping `backend/` (working Spring Boot project) and deleting `src-backend/` (only a hello-world `Main.java` and empty files).
- A layered package layout inside the Spring Boot project: `controller/`, `service/`, `repository/`, `model/`, `integration/`, `common/`.
- An explanation of the architecture with a central PostgreSQL database run by Shared Core. Only `repository/` and `application.properties` are affected.
- Terminal commands for the cleanup and a "Where code goes" table for the README.

## What we changed in the repo
- Deleted `src-backend/` and the empty root `test/`.
- Created the 6 packages in `backend/src/main/java/edu/gmu/cs321/events_resources/` (with `.gitkeep` placeholders).
- Moved `HelloController.java` into `controller/` and updated its `package` line.
- Added a "Where code goes" section to `README.md`.

## Problem hit and fix
- **Error:** `ConflictingBeanDefinitionException: 'helloController' ... conflicts with ... edu.gmu.cs321.events_resources.HelloController`
- **Cause:** an old compiled copy of `HelloController` was left in `backend/target/` after the move, so Spring found two classes with the same bean name.
- **Fix:** `./mvnw clean spring-boot:run`. `clean` deletes `target/` so everything recompiles fresh.

## How we verified it
- Backend started ("Started EventsResourcesApplication", port 8080).
- `http://localhost:8080/hello` returned "Spring Boot is running!"
- Reviewed `git status` before committing: the deletes, the move, the new packages, and the README change were all expected.

## Our decisions (not just AI suggestions)
- Kept `backend/` as the only backend and used package-by-layer to match our architecture doc and how we split work.
- Kept the `DataSourceAutoConfiguration` exclude in `application.properties` until Shared Core gives us the central DB details.
- Still open: confirm with Shared Core how we connect to the central DB, our table naming/schema, and the `users` table format.

## Note for teammates
After pulling this change, run `./mvnw clean spring-boot:run` once to avoid the same error.
