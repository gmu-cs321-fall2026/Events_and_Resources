/**
 *"View personal calendar".
 * Gets the events the logged-in student registered for, so the calendar can
 * show only "My events".
 *
 * It uses two endpoints the backend already has:
 *   GET /session/me                        -> { userId, roles, staff }   (who am I?)
 *   GET /users/{userId}/events/registered  -> { items, page, pageSize, totalCount }
 *
 * The registered-events endpoint has no from/to filter, so this file
 * downloads all of the student's registrations (a student has a handful,
 * not hundreds) and keeps only the ones inside the days on screen.
 */
import { request, getEventsInRange } from "./eventsApi";

const USE_MOCK = import.meta.env.VITE_USE_MOCK === "true";
const PAGE_SIZE = 50; // the most the backend allows per page (SOW §6)

/**
 * Every event the logged-in user registered for, across all pages, soonest first.
 * Same "keep asking for the next page" loop as getEventsInRange in eventsApi.js.
 * Throws with error.status 401 if nobody is logged in.
 */
export async function getAllMyRegisteredEvents() {
  const all = [];
  let page = 1;
  while (true) {
    const params = new URLSearchParams({ page, pageSize: PAGE_SIZE });
    const data = await request(`/users/me/events/registered?${params}`);
    all.push(...data.items);
    if (all.length >= data.totalCount || data.items.length === 0) return all;
    page += 1;
  }
}

/**
 * The calendar's "My events" loader: the logged-in user's registered events
 * between two dates, both included. Dates are "YYYY-MM-DD" strings, and
 * strings in that format sort the same way the dates do, so >= and <= work.
 * Returns a plain array of events, the same shape CalendarPage already uses.
 */
export async function getMyRegisteredEventsInRange(from, to) {
  if (USE_MOCK) return mockMyEventsInRange(from, to);

  const mine = await getAllMyRegisteredEvents();
  return mine.filter((event) => event.date >= from && event.date <= to);
}

/**
 * Mock mode (VITE_USE_MOCK=true): the events on screen that mockEventsApi.js
 * has marked as registered. Reads the same localStorage key that
 * mockEventsApi.js writes when you press Register.
 */
async function mockMyEventsInRange(from, to) {
  const events = await getEventsInRange(from, to);
  const registeredIds = JSON.parse(localStorage.getItem("mockRegisteredEventIds") || "[]");
  return events.filter((event) => registeredIds.includes(event.id));
}
