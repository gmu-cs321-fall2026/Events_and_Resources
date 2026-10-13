\## Asked

Asked ChatGPT to add methods in EventsPage.jsx to show the list of upcoming events, loaded from GET /events and a pop-up event when clicking with null as no pop-up. The useEffect will run after the page shows up and run it again whenever refreshKey changes. 



\## Produced

A complete EventsPage.jsx that show a list of events and pop-up of an event when clicking with refreshKey initializes to 0, key ID of EventCard is set to array indices



\## Changed or rejected

Kept the file. Changed the refreshKey to uninitialized state. Fixed useEffect() so that it loads from the server and handles the case when creating 2 events quickly that the older request come back last and overwrite the newer list with stale data -> keeps only the latest. Changed key in EventCard to event.id instead of using array index

