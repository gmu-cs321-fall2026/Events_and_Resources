\## Asked

Asked ChatGPT to write a proxy where Spring Boot backend is running. The browser sends requests to /api/events, and Vite forwards them from browser GET http://localhost:5173/api/events?page=1 to backend GET http://localhost:8080/events?page=1, because the browser only ever talks to port 5173, there is no CORS error, so the backend does not need any CORS setup



\## Produced

A vite.config.js file including a default defineConfig() method



\## Changed or rejected

Kept the file, added explicitly constant BACKEND\_URL to put in target and plugins from react app

