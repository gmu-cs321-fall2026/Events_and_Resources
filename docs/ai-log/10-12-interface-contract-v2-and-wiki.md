# AI Log — Interface Contract v2, wiki Home page, PR description

- **Date:** 10/12/2026 · **Author:** Yasmin · **AI tool:** Claude (claude.ai)
- **Where:** GitHub wiki (`Home`, `Subsystem 3 — Interface Contract v2`) and the PR description for `Yasmin` → `main`
- **Why logged:** SOW §6 requires interface contracts on the wiki within 48 hours of a change; these docs describe the AI-assisted code from commits `fef1d5a` and `52475b6` and the Register button commit.

## Asked
- Write Interface Contract v2 for my new endpoints (`POST /events/{id}/register`, `GET /users/{userId}/events/registered`), the 401/403 auth rules, and the new Event fields `capacity` and `registeredCount`, building on our v1 contract.
- Write PR description notes for my branch.
- Help organize the wiki: the Home page held the v1 contract, and the sidebar order was confusing. Make Home a better landing page with a direct link to v2.
- Update the "Endpoints at a glance" table using our Sprint 2 Jira board (what other team members are working on).
- Then asked: "Are you sure that's how other teams would run it locally?"

## Produced
- Interface Contract v2 (Markdown): a "What changed since v1" table, an Authentication section (public / logged-in / staff-only, test tokens), the v2 Event object, full request/response/error tables for both new endpoints, `GET /events` and `POST /events` carried over from v1 with the new auth rule, and a list of all error codes.
- A PR description: summary, what's included, merge notes (includes Trang's branch, Postgres required, test tokens, shared `events` table and `PagedResponse`), how I tested, and what's not included.
- Wiki steps: move v1 off Home onto its own page marked "Superseded", turn Home into a landing page, and an optional custom sidebar to control page order.
- A new Home page: overview, a direct `[[...]]` link to v2, endpoints-at-a-glance table, notes for other teams, run instructions, test tokens, repo layout.
- An endpoints table rebuilt from the Jira board, with ticket numbers per row.

## Changed or rejected
- **Rejected the AI's first "Run it locally" section.** I asked whether that's really how other teams would run our subsystem. It isn't: SOW §6 says other teams and Q&I run our **container** with `docker build` / `docker run`, not our dev setup (Maven, npm, a separate Postgres command). Replaced it with two sections, "Other teams / Q&I (container)" and "Our team (development)", and added a **"Current limitation"** note, because our root `Dockerfile` only runs the frontend; the backend and database aren't containerized yet. Flagged that to the team rather than hiding it.
- **Changed the endpoints table to match the real Jira status**, not just my branch: `GET /events` filters (MASON-31), `POST /events` (MASON-28), and PostgreSQL (MASON-35) are marked in progress; my items (MASON-6, -33, -34) are marked done. Used Jira ticket numbers instead of names so the page stays correct if work is reassigned.
- **Did not mark backend `GET /events/{id}` as fully live** even though MASON-32 shows Done, because the AI could not find the backend endpoint on any pushed branch, only Trang's frontend pop-up. Need to confirm with the team before saying it's live.
- **Kept the v1 contract** as its own page labeled "Superseded" instead of deleting it, so other teams can see what changed and when.
- **Kept the test-token table on the wiki** because other teams and Q&I need it to call our protected endpoints until Shared Core's auth exists; labeled it clearly as a stand-in.
- **Flagged an overlap the AI noticed on the board:** MASON-35 (PostgreSQL) overlaps the database setup on my branch (`schema.sql`, connection settings). Will tell that teammate before they finish so we end up with one `events` table, not two.
- **How I checked:** compared every endpoint, status code, and field in the contract against my actual code and my curl test results (201 / 409 `ALREADY_REGISTERED` / 409 `EVENT_FULL` / 404 / 401 / 200 / 403); used the wiki's Preview tab to confirm tables render and the `[[...]]` links turn blue before saving.
