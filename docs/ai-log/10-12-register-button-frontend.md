# AI Log — Register button in the event pop-up

- **Date:** 10/12/2026 · **Author:** Yasmin · **AI tool:** Claude (claude.ai)
- **Commit:** "Add Register button to event pop-up (MASON-6)"
- **Jira:** MASON-6 (student clicks Register; registered if there's room, count goes up)
- **Files:** new `frontend/src/components/RegisterButton.jsx`, `RegisterButton.css`; edited Trang's `EventDetailModal.jsx`, `api/eventsApi.js`, `api/mockEventsApi.js`

## Asked
Add the Register button for MASON-6 inside Trang's event pop-up, connect it to my `POST /events/{id}/register` endpoint, and make it testable without the backend's `GET /events/{id}` (a teammate is still building it). Also asked for a mockup of how the pop-up would change, why Submit isn't the same as Register, and whether editing Trang's files would take credit away from her or affect her PR.

## Produced
- `RegisterButton.jsx` with four states (Register / Registering… / You're registered ✓ / Event full) and messages for `EVENT_FULL`, `ALREADY_REGISTERED`, and 401; `RegisterButton.css` using the team's color variables.
- Edits to Trang's files: import + `<RegisterButton>` in her placeholder in `EventDetailModal.jsx` (and +1 to the shown count after registering); in `eventsApi.js`, an `AUTH_TOKEN` constant, the `Authorization: Bearer` header added inside `request()`, and a `registerForEvent()` function; a mock `registerForEvent()` in `mockEventsApi.js` with the same error codes as the real backend.
- The AI first sent full replacement copies of Trang's three files.

## Changed or rejected
- **Rejected replacing Trang's files with the AI's full copies.** I edited her files by hand and only added the new lines, so the change is easy to review and her lines keep her name in `git blame`. Checked with `git diff`: only one of her lines changed (the `fetch(...)` call in `request()`); everything else is additions.
- **Fixed mistakes I made while hand-editing:** re-indenting `EventDetailModal.jsx` dropped the `);` and `}` after `</>` (error at `return`), and in `eventsApi.js` I had `const USE_MOCK` twice and an unclosed `fetch(` (missing `});`). Fixed all three.
- **Changed the AI's placement suggestion only in comments:** kept the token block above Trang's `import` line (works because imports are hoisted) and labeled it `// Added for registration (Yasmin)` instead of a line number, since line numbers shift.
- **Kept the token header inside the shared `request()` helper** instead of only on the register call, because Trang's comment says this file is "the ONE place the frontend talks to the backend," and every protected endpoint will need the token.
- **Kept the mock `registerForEvent()`** because `eventsApi.js` calls `mock.registerForEvent` in mock mode; without it the button breaks when testing without the backend.
- **Considered and rejected** the AI's alternative of moving my code into separate new files (`registrationApi.js`) to avoid touching Trang's files: it would duplicate her `request()` helper. Decided to tell Trang about the small additions instead.
- **How I verified before keeping it:** ran `VITE_USE_MOCK=true npm run dev`, created an event, opened its pop-up, clicked Register → "You're registered ✓"; closed and reopened the pop-up → it stayed registered (mock remembers). No red errors in the browser console or terminal.
