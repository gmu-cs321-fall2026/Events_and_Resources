# AI Log: Create Event Page 

**AI tool:** Claude
**Dates:** 09-27 to 09-28

## Asked
- Build the React Create Event page for our Vite frontend: a form for event title, employer/host, date, time, location, and tag, with date and time as separate fields.
- Match the field names to Person 1's `Event.java` and hand the event to Person 2's API through `createEvent(event)`.
- Explain the code line by line and the React fundamentals behind it (components, JSX, props, state, `useState` as a Hook, events, controlled inputs, re-rendering, `async`/`await`, imports/exports).
- Explain how the other frontend files connect (`index.html` → `main.jsx` → `App.jsx` → pages) and how to tell the project uses Vite.
- Explain why a `fetch` approach from a YouTube tutorial wasn't used directly in the page.
- Explain the CSS choices and whether my styles could affect teammates' pages.
- Break the page into stages so I could build it myself, with a skeleton file and inline comments.

## Produced
- `frontend/src/pages/CreateEventPage.jsx`: a form component using `useState` for the form values and the success/error message, `handleChange` to update fields as the user types, and `handleSubmit` to build the event object and call `createEvent(event)`. Includes inline comments explaining each step.
- `frontend/src/pages/CreateEventPage.css`: styles scoped under `.create-event` so they don't affect other pages, with date and time side by side.
- A skeleton version of the page, split into 5 build stages.
- Temporary testing files: a placeholder `eventsApi.js` (stores events in an array with `createEvent` and `getEvents`) 

## Changed or rejected
- Renamed fields from `title`/`employer` to `eventTitle`/`host` to match `Event.java`.
- Rejected combining date and time into one `dateTime` field. They stay as separate `date` and `time` fields (make sure it works for person 1)).
- Removed the Registration Link field, since students register inside our platform.
- Added "Internal" to the tag options.
- Changed the import from `careerAPI.js` to our team's `eventsApi.js`.
- Chose not to call `fetch` in the page; server calls belong in `eventsApi.js` (Person 2's area).
- Kept class-prefixed CSS instead of switching to CSS Modules for now.
- Decided not to add the GMU logo for now.
- Did not commit the placeholder `eventsApi.js` or the testing `App.jsx`; those belong to teammates or are shared.
