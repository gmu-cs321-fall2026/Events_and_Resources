// Stand-in for the backend while GET/POST /events aren't merged yet.
// Turned on with VITE_USE_MOCK=true (see eventsApi.js).
// It saves events in the browser's localStorage, but returns the SAME shapes the real backend will (interface contract v1), so pages don't need to change when we switch to the real server.

const STORAGE_KEY = "careerEvents";

function loadAll() {
  const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || "[]");
  // Events saved in Sprint 1 have no id, so give each one an id based on its position.
  return saved.map((event, index) => ({ id: index + 1, ...event }));
}

function saveAll(events) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(events));
}

export async function getEvents({ page = 1, pageSize = 20 } = {}) {
  // Earliest first, same as the contract: by date, then time.
  const all = loadAll().sort((a, b) =>
    (a.date + a.time).localeCompare(b.date + b.time)
  );
  const start = (page - 1) * pageSize;
  return {
    items: all.slice(start, start + pageSize),
    page,
    pageSize,
    totalCount: all.length,
  };
}

export async function getEventById(id) {
  const event = loadAll().find((e) => String(e.id) === String(id));
  if (!event) {
    const error = new Error(`No event with id ${id}`);
    error.status = 404;
    error.code = "NOT_FOUND";
    throw error;
  }
  return event;
}
// Pretend registration, mirroring POST /events/{id}/register on the real backend:
// same success shape, same error codes. Remembers which events "you" registered for
// in localStorage, so registering twice gives ALREADY_REGISTERED like the server would.
const REGISTERED_KEY = "mockRegisteredEventIds";
const MOCK_USER_ID = 7; // same id as the backend's "student-token"

function mockError(status, code, message) {
  const error = new Error(message);
  error.status = status;
  error.code = code;
  return error;
}

export async function registerForEvent(eventId) {
  const all = loadAll();
  const event = all.find((e) => String(e.id) === String(eventId));
  if (!event) throw mockError(404, "NOT_FOUND", `Event ${eventId} not found`);

  const registeredIds = JSON.parse(localStorage.getItem(REGISTERED_KEY) || "[]");
  if (registeredIds.includes(event.id)) {
    throw mockError(409, "ALREADY_REGISTERED", "You're already registered for this event");
  }

  const capacity = event.capacity ?? 0; // events from the form have no capacity yet = unlimited
  const count = event.registeredCount ?? 0;
  if (capacity > 0 && count >= capacity) {
    throw mockError(409, "EVENT_FULL", "This event is full");
  }

  event.registeredCount = count + 1; // "event" is inside "all", so saving "all" saves this too
  saveAll(all);
  localStorage.setItem(REGISTERED_KEY, JSON.stringify([...registeredIds, event.id]));

  return { id: Date.now(), eventId: event.id, userId: MOCK_USER_ID, createdAt: new Date().toISOString() };
}
export async function createEvent(event) {
  const all = loadAll();
  const saved = { ...event, id: all.length + 1 };
  saveAll([...all, saved]);
  return saved;
}