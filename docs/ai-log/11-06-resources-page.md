# AI Log: MASON-12 Browse resources directory (frontend) and Github workflow ci.yml testing

**AI tool:** Claude
**Date:** 11-06

## Asked
Asked for a nice-looking Resources landing page: search by keyword and by major, cards that show who shared each resource, plus spots for teammates' parts (calendar, jobs, share form, navigation) without editing their files. I also ask for help with the ci format.

## Produced
- Design mockup first (canvas), then the React page built from it.
- `api/resourcesApi.js`: `getResources({ q, major, type })` using SAMPLE data (only `approved` shown) behind a `USE_SAMPLE_RESOURCES` switch, plus `getUpcomingEvents(major)` reading the real `GET /events?from=&audience=`.
- `pages/ResourcesPage.jsx/.css`: hero with search, major chips, type pills, results grid, empty state.
- `components/ResourceCard`, `UpcomingEventsPanel` (real events), `JobsPanel` (placeholder for MASON-13).
- Connection points as props: `onShare` (Jean MASON-7), `calendarHref` (Trang MASON-3), `jobsHref` (Cindy MASON-13). No top nav on the page (Cindy MASON-50 mounts it).
- `api/resourcesApi.test.js`: filter rules (major + "All Students", keyword, type, approved only, shows who shared).

## Changed or rejected
- Kept resource data as sample data: the resource table belongs to Jean's MASON-7; switch to GET /resources once it exists, adjusting field names in `toCard()`.

