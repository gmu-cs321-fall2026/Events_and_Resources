// Date math for the event calendar.
//
// Two rules used everywhere in this file:
//   1. Dates from the backend are strings like "2026-11-05" (YYYY-MM-DD).
//   2. We always build JavaScript Dates in LOCAL time, never UTC.
//      new Date("2026-11-05") is read as UTC midnight, which is Nov 4 at 7 PM
//      in Virginia, so the event would land on the wrong day. new Date(2026, 10, 5)
//      (year, monthIndex, day) is local midnight, so it lands on the right day.

export const WEEKDAY_NAMES = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];

// The three views the story asks for. Used for the Month / Week / Day buttons.
export const VIEWS = ["month", "week", "day"];

export function parseISODate(text) {
  const [year, month, day] = text.split("-").map(Number);
  return new Date(year, month - 1, day);
}

// A Date -> "2026-11-05". This is the format the backend's ?from= and ?to= expect.
// padStart(2, "0") turns 5 into "05" so the string is always YYYY-MM-DD.
export function toISODate(date) {
  // padStart(2, "0") turns 5 into "05" so the string is always YYYY-MM-DD.
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${date.getFullYear()}-${month}-${day}`;
}

// Returns a NEW Date, 'days' later (or earlier if negative). Never changes 'date'.
// setDate() handles month ends: Oct 31 + 1 day = Nov 1.
export function addDays(date, days) {
  const result = new Date(date);
  result.setDate(result.getDate() + days);
  return result;
}

// Same idea for months. We jump to day 1 first, so Jan 31 + 1 month = Feb 1,
// not "Feb 31" (which JavaScript would roll over to early March).
export function addMonths(date, months) {
  return new Date(date.getFullYear(), date.getMonth() + months, 1);
}

// The Sunday on or before `date`.
export function startOfWeek(date) {
  return addDays(date, -date.getDay()); // getDay(): Sunday = 0 ... Saturday = 6
}

// True when two Dates fall on the same calendar day (time of day ignored).
export function isSameDay(a, b) {
  return toISODate(a) === toISODate(b);
}

// Which days are on screen for a view, as { start, end } Dates (both included).
//   day   -> just that day
//   week  -> Sunday through Saturday
//   month -> whole weeks covering the month, so the grid has no gaps.
//            e.g. November 2026 -> Sun Nov 1 to Sat Dec 5
// CalendarPage asks the backend for events in exactly this range.
export function getVisibleRange(view, anchor) {
  if (view === "day") return { start: anchor, end: anchor };

  if (view === "week") {
    const start = startOfWeek(anchor);
    return { start, end: addDays(start, 6) };
  }

  const firstOfMonth = new Date(anchor.getFullYear(), anchor.getMonth(), 1);
  const lastOfMonth = new Date(anchor.getFullYear(), anchor.getMonth() + 1, 0);
  return {
    start: startOfWeek(firstOfMonth),
    end: addDays(startOfWeek(lastOfMonth), 6),
  };
}

// Every day from start to end (both included), as an array of Dates.
export function daysBetween(start, end) {
  const days = [];
  for (let d = start; d <= end; d = addDays(d, 1)) days.push(d);
  return days;
}

// The "‹" and "›" buttons: move one month, one week, or one day.
// direction is +1 (next) or -1 (previous).
export function shiftAnchor(view, anchor, direction) {
  if (view === "month") return addMonths(anchor, direction);
  if (view === "week") return addDays(anchor, 7 * direction);
  return addDays(anchor, direction);
}

// The heading above the grid, e.g. "November 2026", "Nov 1 – 7, 2026",
// or "Thursday, November 5, 2026". toLocaleDateString() does the month names for us.
export function formatTitle(view, anchor) {
  if (view === "month") {
    return anchor.toLocaleDateString("en-US", { month: "long", year: "numeric" });
  }
  if (view === "day") {
    return anchor.toLocaleDateString("en-US", {
      weekday: "long", month: "long", day: "numeric", year: "numeric",
    });
  }

  const { start, end } = getVisibleRange("week", anchor);
  const short = { month: "short", day: "numeric" };
  return `${start.toLocaleDateString("en-US", short)} – ${end.toLocaleDateString("en-US", short)}, ${end.getFullYear()}`;
}

export function formatTime(time) {
  if (!time) return "";
  const [hours, minutes] = time.split(":").map(Number);
  if (Number.isNaN(hours) || Number.isNaN(minutes)) return time;
  const suffix = hours >= 12 ? "PM" : "AM";
  const hour12 = hours % 12 === 0 ? 12 : hours % 12; // 0 -> 12 AM, 13 -> 1 PM
  return `${hour12}:${String(minutes).padStart(2, "0")} ${suffix}`;
}

// Puts events into buckets by day, so each calendar cell can find its events fast:
//   { "2026-11-05": [event, event], "2026-11-12": [event] }
// Inside each day, events are sorted by time (earliest first).
export function groupEventsByDate(events) {
  const byDate = {};
  for (const event of events) {
    byDate[event.date] ??= [];
    byDate[event.date].push(event);
  }
  for (const list of Object.values(byDate)) {
    list.sort((a, b) => (a.time || "").localeCompare(b.time || ""));
  }
  return byDate;
}
