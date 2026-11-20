/**
 * eventSearch (MASON-10 Search bar, Events tab): checks if one event matches the
 * Events page filters: a search keyword, a category chip and a format pill.
 *
 * The backend does the real filtering (GET /events?q=&category=&format=). These helpers
 * are only for lists the backend can't filter for us:
 *   - the calendar's "My events" view (GET /users/me/events/registered has no filters)
 *   - mock mode (VITE_USE_MOCK=true), where there is no backend at all
 *
 * The keyword checks the SAME fields as EventRepository.java (title, host, location,
 * audience, category) and ignores upper/lower case, so both ways give the same answer.
 */

/** The event fields the keyword search looks in, same list as the backend. */
const SEARCHED_FIELDS = ["eventTitle", "host", "location", "audience", "category"];

/** The "show everything" choices for the category chips and the format pills. */
export const ALL_CATEGORIES = "All events";
export const ALL_FORMATS = "All formats";

/** Category chips in the hero. Same list as CATEGORIES in CreateEventPage.jsx and EventService.java. */
export const CATEGORY_CHIPS = [
  ALL_CATEGORIES,
  "Career Fair",
  "Workshop",
  "Employer Info Session",
  "Networking",
  "Panel",
  "Internal",
];

/** Format pills above the calendar. Same list as FORMATS in CreateEventPage.jsx. */
export const FORMAT_PILLS = [ALL_FORMATS, "In-person", "Virtual"];

/**
 * True when the keyword appears in any searched field. An empty keyword matches everything.
 *
 * @param event   an Event from the API
 * @param keyword what the student typed, e.g. "resume"
 */
export function matchesKeyword(event, keyword) {
  const needle = (keyword ?? "").trim().toLowerCase();
  if (needle === "") return true;

  return SEARCHED_FIELDS.some((field) => {
    const value = event[field];
    if (value == null) return false;
    const text = Array.isArray(value) ? value.join(" ") : String(value);
    return text.toLowerCase().includes(needle);
  });
}

/** Same-text check that ignores upper/lower case, like the backend. Empty = no filter. */
function sameText(value, wanted) {
  if (!wanted) return true;
  return String(value ?? "").toLowerCase() === wanted.toLowerCase();
}

/**
 * True when the event passes every filter. Filters left empty are skipped.
 *
 * @param event   an Event from the API
 * @param filters { q, category, format }, the same names the backend uses
 */
export function matchesFilters(event, { q = "", category = "", format = "" } = {}) {
  return matchesKeyword(event, q) && sameText(event.category, category) && sameText(event.format, format);
}

/**
 * Turns the page's choices into the filters the API wants: the "All ..." choices
 * become "" so they're left out of the URL.
 */
export function toApiFilters({ keyword, category, format }) {
  return {
    q: keyword,
    category: category === ALL_CATEGORIES ? "" : category,
    format: format === ALL_FORMATS ? "" : format,
  };
}

/** True when any filter is on. */
export function hasFilters({ q, category, format }) {
  return Boolean(q || category || format);
}

/**
 * A short description of the filters for status lines, e.g. '"resume" · Workshop · Virtual'.
 */
export function describeFilters({ q, category, format }) {
  return [q && `"${q}"`, category, format].filter(Boolean).join(" · ");
}
