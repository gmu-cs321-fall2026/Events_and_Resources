# AI Log: MASON-9 View personal calendar ("My events")

## Asked
Build MASON-9 by reusing the existing event calendar, filtered down to the events the logged-in student registered for, with cancelling still done from the event pop-up.

## Produced
- `frontend/src/api/myEventsApi.js` (new): `getMyUserId()` (GET /session/me), `getAllRegisteredEvents(userId)` (pages through GET /users/{userId}/events/registered), and `getMyRegisteredEventsInRange(from, to)`, which keeps only the events in the days on screen. Mock mode reads the registrations that mockEventsApi.js saves.
- `frontend/src/api/myEventsApi.test.js` (new): 5 tests (calls /session/me first, date range ends included, more than 50 registrations, empty list, 401).
- `frontend/src/pages/CalendarPage.jsx`: "All events / My events" switch, which picks the loader, adds a status line with an empty-state hint and shows a "Log in" message on 401.
- `frontend/src/api/eventsApi.js`: `request()` is now exported so the new file sends the same login token.

## Changed or rejected
- Rejected adding a new backend endpoint: `/session/me` and `/users/{userId}/events/registered` already give us everything, so the interface contract doesn't change.
- Rejected a separate "My events" page: reusing the calendar keeps the month/week/day views, and the pop-up's Cancel button already covers "edit registration".
- Put the new functions in their own file instead of eventsApi.js to avoid merge conflicts with teammates' edits.
- Date filtering happens in the browser because the registered-events endpoint has no from/to filter, and a student has only a few registrations.
