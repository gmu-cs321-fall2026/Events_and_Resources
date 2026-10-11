\## Asked

Asked ChatGPT to create a temporary mock that run VITE\_USE\_MOCK=true npm run dev and everything works from localStorage, using the same data shapes as the real server.  Stand-in for the backend while GET/POST /events aren't merged yet.



\## Produced

An mockEventsApi.js that that saves events in the browser's localStorage, but returns the same shapes the real backend will, including functions loadAll() that gives each event an ID based on its position, saveAll(), getEvents() by date and time with default page 1 and pageSize 20 (if none found), getEventById() and createEvent()



\## Changed or rejected

Had loadAll() private instead of public, changed sorting as date + time format instead of date parsing, added error handling for getEventById not found

