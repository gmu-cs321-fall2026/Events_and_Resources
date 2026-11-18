/**
 * waitlistApi.js (MASON-18 / MASON-19): the frontend's calls for the event waitlist.
 *
 *   joinWaitlist(eventId)               POST   /events/{id}/waitlist  -> { eventId, registered, waitlisted, waitlistPosition, ... }
 *   leaveWaitlist(eventId)              DELETE /events/{id}/waitlist  -> null (204)
 *   getWaitlist(eventId)                GET    /events/{id}/waitlist  -> WaitlistView (staff)
 *   changeCapacity(eventId, capacity)   PUT    /events/{id}/capacity  -> WaitlistView (staff)
 *
 * WaitlistView = { eventId, capacity, registeredCount,
 *                  waitlist: [{ position, userId, joinedAt }], justAccepted: [userId, ...] }
 *
 * A student's place in line comes from the existing getMyRegistration(eventId) in eventsApi.js,
 * which now also returns { waitlisted, waitlistPosition }.
 *
 * Kept in its own file (like myEventsApi.js) so it can't collide with teammates' edits to eventsApi.js.
 * It reuses eventsApi's request(), so the same login token and error format apply.
 */
import { request } from "./eventsApi";

const USE_MOCK = import.meta.env.VITE_USE_MOCK === "true";

/** The waitlist only exists on the real backend; mock mode explains that instead of failing silently. */
function mockNotSupported() {
  const error = new Error("The waitlist needs the real backend (run without VITE_USE_MOCK=true).");
  error.code = "MOCK_UNSUPPORTED";
  return Promise.reject(error);
}

/**
 * Join the waitlist of a full event.
 * Throws with error.code: "SPOTS_AVAILABLE" (a seat is open, register instead),
 * "ALREADY_WAITLISTED", "ALREADY_REGISTERED", "NOT_FOUND", or status 401.
 */
export function joinWaitlist(eventId) {
  if (USE_MOCK) return mockNotSupported();
  return request(`/events/${encodeURIComponent(eventId)}/waitlist`, { method: "POST" });
}

/**
 * Leave the waitlist.
 * Throws with error.code "NOT_WAITLISTED" (404) if you aren't in line, which also
 * happens when you were just given a seat.
 */
export function leaveWaitlist(eventId) {
  if (USE_MOCK) return mockNotSupported();
  return request(`/events/${encodeURIComponent(eventId)}/waitlist`, { method: "DELETE" });
}

/** Staff: the event's waitlist, first in line first. 403 for students. */
export function getWaitlist(eventId) {
  if (USE_MOCK) return mockNotSupported();
  return request(`/events/${encodeURIComponent(eventId)}/waitlist`);
}

/**
 * Staff: set a new capacity (0 = unlimited). The backend then moves people from
 * the waitlist into any seats that opened; they're listed in "justAccepted".
 * Throws with error.code "VALIDATION_ERROR" (400) if it's below the people already registered.
 */
export function changeCapacity(eventId, capacity) {
  if (USE_MOCK) return mockNotSupported();
  return request(`/events/${encodeURIComponent(eventId)}/capacity`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ capacity }),
  });
}
