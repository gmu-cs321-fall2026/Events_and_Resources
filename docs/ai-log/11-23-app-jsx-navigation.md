# AI log: restore navigation in App.jsx (11-23)

**Asked:** App.jsx on `main` wasn't showing the navigation bar, Home page or new Events tab. Update App.jsx so the website uses them, and check the page works.

**Produced:** Claude found that PR #39 (MASON-7/MASON-11) brought back the old one-page App.jsx, replacing Cindy's navigation version from PR #36 (commit cea8cb6). It merged the two: Cindy's NavBar + `useHashRoute()` page switching (Home / Events / Resources / Jobs), plus Jean's resource pieces (Share a resource pop-up, My submissions, staff review queue) placed on the Resources page. Create Event is no longer in App.jsx because EventsHubPage already shows it to staff/admin. Claude bundled the frontend and opened every page in a headless browser with a stubbed API: all 4 tabs, `#calendar`, staff login, the share pop-up, the review queue and the Create Event button loaded with no JavaScript errors.

**Changed or rejected:** Removed the old `refreshKey`/`eventsView`/`jobsKey` state from App.jsx: EventsHubPage keeps its own, and only one page shows at a time, so the Resources jobs panel reloads anyway.