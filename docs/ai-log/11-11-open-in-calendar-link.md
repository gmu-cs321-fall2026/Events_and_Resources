# AI Log: Connect "Open in calendar" button

## Asked
The "Open in calendar" button on the Resources page didn't go anywhere. Asked how to connect it to the calendar.

## Produced
- App.jsx: pass `calendarHref="#calendar"` to `<ResourcesPage />`.
- App.jsx: wrap the Calendar/List switch and calendar in `<section id="calendar">` so the link scrolls to it.

## Changed or rejected
- Used a plain anchor link instead of React state, since the calendar is on the same page.
- Waited on Cindy's merge before pushing so we didn't conflict in App.jsx.
