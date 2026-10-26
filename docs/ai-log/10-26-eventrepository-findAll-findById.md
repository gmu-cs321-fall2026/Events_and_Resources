# AI Log — EventRepository findAll/findById, and Event.registeredCount fix

- **Date:** 10/26/2026 · **Author:** Yasmin · **AI tool:** Claude (claude.ai)
- **Commits:** `9730f8e` "Add EventRepository.findAll and findById for GET /events", `d4f09ac` "Event.registeredCount int -> Integer..."
- **Jira:** supports MASON-41 (Cindy: connect controller/service for GET /events and GET /events/{id})
- **Files:** `repository/EventRepository.java`, `model/Event.java`

## Asked
- Add `findAll` and `findById` to `EventRepository` (my part of the team split) so Cindy can build `GET /events` and `GET /events/{id}`.
- Explain both methods in a beginner-friendly way: what `?` placeholders, `LIMIT`/`OFFSET`, `Optional` and `query()` vs `queryForObject()` do, why we need `offset` at all, and a worked example of paging with our sample events.
- Figure out why the Create Event form failed with `INVALID_JSON`.

## Produced
- `findAll(limit, offset)`: returns one page of events sorted by `event_date, event_time, id`, using `LIMIT ? OFFSET ?`. No events → empty list (becomes `"items": []`).
- `findById(id)`: returns `Optional<Event>`; empty when no event has that id (the service turns that into a 404).
- Both reuse Cindy's `EVENT_COLUMNS` and `EVENT_MAPPER`, so every method returns events in the same shape.
- Diagnosis of the Create Event bug: the form doesn't send `registeredCount`, and `Event.java` declared it as `int`, which can't be empty, so Spring Boot 4's JSON reader rejected the request.

## Changed or rejected
- **Added my methods to Cindy's existing file instead of making a new class**, because the team split assigned "EventRepository with findAll and findById" to me and her `insert` already lives there. I didn't change any of her code (`insert`, `EVENT_COLUMNS`, `EVENT_MAPPER`); git shows my methods under my name and hers under hers. VS Code reordered the imports, which moved her `import Event` line.
- **Added `id` as a tiebreaker in `ORDER BY`** so two events at the exact same date and time always come back in the same order and can't jump between pages.
- **Used `query()` + `findFirst()` for `findById` instead of `queryForObject()`**, because `queryForObject()` throws an exception when zero rows come back; an empty `Optional` is easier to turn into a 404.
- **Rejected a longer version of the file** with comments added around Cindy's code, to avoid adding to her section. Kept comments on my own methods only, and added the `offset = (page − 1) × pageSize` note myself.
- **Changed `int registeredCount` → `Integer` in Cindy's `Event.java` only after asking her** (she approved), in its own commit so it's clearly separate from my repository work. `EventService` already sets it to 0 when saving, so nothing else needed to change.
- **Did not add `countAll()`** (needed for `totalCount`); left that for whoever builds the `GET /events` response with Cindy.
- **How I verified:**
  - `./mvnw -q compile` passed with no errors before committing.
  - Confirmed `git status` showed only `Event.java` and `EventRepository.java` before committing, so Trang's borrowed `data.sql` wasn't included.
  - The AI ran the same SQL in PostgreSQL with our sample data: pages with `LIMIT 3 OFFSET 0/3/6` returned events in date-then-time order, a page past the end returned an empty list, and an unknown id returned no rows.