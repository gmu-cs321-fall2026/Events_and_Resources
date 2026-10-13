import process from 'node:process'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// Where the Spring Boot backend is running.
// - On your laptop (npm run dev): Spring Boot is on http://localhost:8080
// - In Docker: "localhost" means *inside the container*, so pass the real address in, e.g. docker run -e BACKEND_URL=http://host.docker.internal:8080 ...

const BACKEND_URL = process.env.BACKEND_URL || 'http://localhost:8080'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // The proxy: any request the browser sends to /api/... goes to the Vite
    // dev server first, and Vite forwards it to Spring Boot with "/api" removed.
    //   browser:  GET http://localhost:5173/api/events?page=1
    //   backend:  GET http://localhost:8080/events?page=1
    // Because the browser only ever talks to port 5173, there is no CORS
    // error, so the backend doesn't need any CORS setup.
    proxy: {
      '/api': {
        target: BACKEND_URL,
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
})
