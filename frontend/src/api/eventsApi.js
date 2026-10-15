// The ONE place the frontend talks to the backend.
// Pages never call fetch() themselves; they call these functions.
// That way, if a URL or the error format changes, only this file changes.
//
// Every function here is async, so it returns a Promise.
// Pages use them with `await getEvents()` or `getEvents().then(...)`.

// "/api" is caught by the proxy in vite.config.js and sent to Spring Boot.
const BASE_URL = "/api";

import * as mock from "./mockEventsApi";
//Run the frontend with VITE_USE_MOCK=true npm run dev and every function below uses localStorage instead of the server
const USE_MOCK = import.meta.env.VITE_USE_MOCK === "true";

// Helper used by every function: send a request, then either return the JSON body or throw an error message
async function request(path, options = {}) {
  let response;
  try {
    // fetch() only fails here if the request never got an answer at all.
    response = await fetch(BASE_URL + path, options);
  } catch {
    throw new Error("Can't reach the server. Is the backend running?");
  }

// Read the body as text first, then try to turn it into JSON.
  // (An empty body or an HTML error page would crash response.json().)
  const text = await response.text();
  let body;
  try {
    body = text ? JSON.parse(text) : null;
  } catch {
    body = null;
  }

  // response.ok is true for status 200-299.
  if (!response.ok) {
    // Our team's error format (SOW §6): {"error": "...", "code": "...", "detail": "..."}
    // If the backend is down, the proxy answers 500/502 with no JSON body.
    const message =
      body?.detail ||
      body?.error ||
      (response.status >= 500
        ? "Can't reach the server. Is the backend running?"
        : `Request failed (${response.status})`);

    const error = new Error(message);
    error.status = response.status; // e.g. 404, so pages can react to it
    error.code = body?.code;         // e.g. "NOT_FOUND"
    throw error;
  }

  return body;
}
//const STORAGE_KEY = "careerEvents";

//GET /events -> { items: [Event, ...], page, pageSize, totalCount }
export async function getEvents( { page = 1, pageSize = 20 } = {}) {
    if (USE_MOCK) return mock.getEvents({ page, pageSize });

    //const events = localStorage.getItem(STORAGE_KEY);

    // URLSearchParams builds "page=1&pageSize=20" safely for us.
    const params = new URLSearchParams({ page, pageSize });
    return request(`/events?${params}`);

    //if (events) return JSON.parse(events);
}

//POST /events -> the saved events, now with an id
export function createEvent(event) {
    if (USE_MOCK) return mock.createEvent(event);

    //const events = getEvents();
    //events.push(event);
    //localStorage.setItem(STORAGE_KEY, JSON.stringify(events));

    return request("/events", {
    method: "POST",
    headers: { "Content-Type": "application/json" }, // tells Spring Boot "this is JSON"
    body: JSON.stringify(event),                    // converts the JavaScript object -> JSON text
  });
}

//GET /events/{id} -> one Event (with its id). Throws with status 404 if it doesn't exist.
export async function getEventById(id) {
  if (USE_MOCK) return mock.getEventById(id);

  //encodeURIComponent() built-in function makes sure a weird id can't break the URL.
  return request(`/events/${encodeURIComponent(id)}`);
}