# AI Log — Schema: categories, audiences and event_audiences table
- **Date:** 10/23/2026 (updated 10/25) · **Author:** Yasmin · **AI tool:** Claude (claude.ai)
- **Files:** `backend/src/main/resources/schema.sql`

## Asked
- Do my part of the schema split: add the `categories` and `audiences` tables from the ER diagram, link `events` to them, and keep `events` and `registrations` as they are on `main`.
- 10/25: Trang pushed sample rows in `data.sql` that use `name`/`description` columns and an `event_audiences` table. Update my schema so it works with her file.
- Explain the many-to-many `event_audiences` table line by line, and help write comments for teammates who are new to SQL.

## Produced
- `categories` (id, name, description) and `audiences` (id, name, description), with `name` UNIQUE.
- `events.category` now `REFERENCES categories(name) ON UPDATE CASCADE`, so the database rejects any category that isn't on the list.
- `event_audiences` (event_id, audience_id): a many-to-many table linking each event to its audiences, primary key on the pair.
- Tables ordered so every table is created after the tables it points at.

## Changed or rejected
- **Linked `events.category` to the category name, not the id.** Pointing at the id would mean changing the column to a number and rewriting every events query and the JSON. Pointing at the name keeps "Workshop" in the column, so registration, Cindy's insert and `findAll`/`findById` need no changes.
- **First version (10/23) used a column called `type` and put the six categories in `schema.sql`.** Rejected on 10/25: Trang's `data.sql` uses `name` and adds a description for each category, so I renamed the column and removed my category rows. Otherwise my rows would load first without descriptions and hers would be skipped.
- **Kept `events.audience` as plain text** because Contract v3 says audience is a string (e.g. "Juniors, Seniors"). The real links go in `event_audiences` instead of a `REFERENCES` on that column.
- **Kept `description` columns** for a possible tooltip or filter later; they're optional (can be empty) and marked as safe to remove.
- **Wrote the file myself** from the AI's explanations, including the SQL cheat sheet and beginner comments. Testing caught two mistakes I fixed: a missing comma after `location`, and `type` still needing to be `name` for the `REFERENCES` to work.
- **Did not commit Trang's `data.sql`** even though my schema needs it to start. I only borrowed it locally to test (`git checkout origin/Trang -- data.sql`, then `git restore`), so her work stays under her name. Our two changes need to merge together.
- **How I verified** (on my Mac, Docker PostgreSQL + Spring Boot, with Trang's `data.sql` borrowed):
  - With the old `data.sql`, startup failed with `Key (category)=(Career Fair) is not present in table "categories"`. That proves the category link works.
  - With Trang's `data.sql`, the backend started cleanly.
  - `POST /events/{id}/register`: first register → 201; again → `ALREADY_REGISTERED`; second student on the 30-seat event → 201; second student on the 1-seat event → `EVENT_FULL`.
  - `GET /users/7/events/registered` returned the right event with `registeredCount: 2`.
  - The AI also ran the schema with Trang's `data.sql` in PostgreSQL 16: 6 categories, 12 audiences, 7 events, 12 links; running it twice made no duplicates.
- **Found a bug in someone else's code (not fixed by me):** Create Event fails with `INVALID_JSON` because the form doesn't send `registeredCount` and `Event.java` declares it as `int`. Reported to Cindy (owner of `POST /events`); suggested changing it to `Integer`.