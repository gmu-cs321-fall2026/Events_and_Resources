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

export async function createEvent(event) {
  const all = loadAll();
  const saved = { ...event, id: all.length + 1 };
  saveAll([...all, saved]);
  return saved;
}