// Browse events calendar.
// How it works:
//   1. 'view' (month/week/day) + 'anchor' (the date we're looking at) decide which
//      days are on screen (getVisibleRange in utils/calendarDates.js).
//   2. Ask the backend for every event in those days: GET /events?from=&to=
//   3. Drop each event into its day's cell.
//   4. Clicking an event opens the SAME pop-up the list uses (EventDetailModal),
//      which already has the Register button.

// useState  = remember a value; changing it redraws the page.
// useEffect = run code (like loading data) after the page draws.
// useMemo   = remember a calculated value until its inputs change (skips redoing work).
import { useEffect, useMemo, useState } from "react";
import { getEventsInRange } from "../api/eventsApi";
import EventDetailModal from "../components/EventDetailModal";
import {
  VIEWS,
  WEEKDAY_NAMES,
  daysBetween,
  formatTime,
  formatTitle,
  getVisibleRange,
  groupEventsByDate,
  isSameDay,
  shiftAnchor,
  toISODate,
} from "../utils/calendarDates";
import "./CalendarPage.css";

// How many events fit in a month cell before we show "+N more".
const MAX_PER_MONTH_CELL = 3;

// Props: refreshKey = number from App.jsx that changes after an event is created,
// so a new event shows up on the calendar without refreshing the browser.
export default function CalendarPage({ refreshKey }) {
  const [view, setView] = useState("month");          // "month" | "week" | "day"
  const [anchor, setAnchor] = useState(() => new Date()); // the day we're looking at; starts at today
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedEventId, setSelectedEventId] = useState(null); // which pop-up is open (null = none)
  // Goes up by 1 when the pop-up closes, to reload the calendar.
  // That way the "x / y registered" counts are fresh after someone registers.
  const [reloadKey, setReloadKey] = useState(0);

  // The first and last day on screen, e.g. Nov 1 – Dec 5 for November's month view.
  const { start, end } = getVisibleRange(view, anchor);
  // As strings, because the effect below should re-run when the DAYS change.
  // (Two Date objects for the same day are different objects, which would re-run it forever.)
  const from = toISODate(start);
  const to = toISODate(end);

  // Load the events for the days on screen. Re-runs when the range changes,
  // after an event is created (refreshKey), or after the pop-up closes (reloadKey).
  useEffect(() => {
    let ignore = false; // ignore an old answer that arrives after a newer request (same trick as EventsPage)
    setLoading(true);
    getEventsInRange(from, to)
      .then((items) => {
        if (ignore) return;
        setEvents(items);
        setError(null);
      })
      .catch((err) => {
        if (!ignore) setError(err.message);
      })
      .finally(() => {
        if (!ignore) setLoading(false);
      });
    return () => {
      ignore = true;
    };
  }, [from, to, refreshKey, reloadKey]);

  // { "2026-11-05": [events...] }, only recalculated when `events` changes.
  const eventsByDate = useMemo(() => groupEventsByDate(events), [events]);

  const today = new Date();
  const days = daysBetween(start, end);

  // Clicking a day number (or "+N more") in month/week view zooms into that day.
  function openDay(date) {
    setAnchor(date);
    setView("day");
  }

  return (
    <section className="calendar">
      {/* ---------- toolbar: Today, previous/next, title, view switch ---------- */}
      <div className="calendar-toolbar">
        <div className="calendar-nav">
          <button type="button" onClick={() => setAnchor(new Date())}>Today</button>
          <button type="button" onClick={() => setAnchor(shiftAnchor(view, anchor, -1))} aria-label={`Previous ${view}`}>‹</button>
          <button type="button" onClick={() => setAnchor(shiftAnchor(view, anchor, 1))} aria-label={`Next ${view}`}>›</button>
        </div>

        {/* aria-live: screen readers announce the new title when it changes */}
        <h2 className="calendar-title" aria-live="polite">{formatTitle(view, anchor)}</h2>

        {/* aria-pressed tells screen readers which view button is "on" */}
        <div className="calendar-views" role="group" aria-label="Calendar view">
          {VIEWS.map((v) => (
            <button
              key={v}
              type="button"
              aria-pressed={view === v}
              className={view === v ? "is-active" : ""}
              onClick={() => setView(v)}
            >
              {/* "month" -> "Month" */}
              {v[0].toUpperCase() + v.slice(1)}
            </button>
          ))}
        </div>
      </div>

      {/* Status line: always visible, so the grid doesn't jump around while loading (Nielsen #1) */}
      {error ? (
        <p className="error" role="alert">Could not load events: {error}</p>
      ) : (
        <p className="calendar-status">
          {loading ? "Loading events…" : `${events.length} event${events.length === 1 ? "" : "s"} shown`}
        </p>
      )}

      {view === "month" && (
        <MonthView days={days} anchor={anchor} today={today} eventsByDate={eventsByDate} onOpenDay={openDay} onSelect={setSelectedEventId} />
      )}
      {view === "week" && (
        <WeekView days={days} today={today} eventsByDate={eventsByDate} onOpenDay={openDay} onSelect={setSelectedEventId} />
      )}
      {view === "day" && (
        <DayView dayEvents={eventsByDate[toISODate(anchor)] ?? []} onSelect={setSelectedEventId} />
      )}

      {/* The same pop-up the list uses, with the Register button inside. */}
      {selectedEventId !== null && (
        <EventDetailModal
          key={selectedEventId}
          eventId={selectedEventId}
          onClose={() => {
            setSelectedEventId(null);
            setReloadKey((n) => n + 1); // refresh counts in case the user registered
          }}
        />
      )}
    </section>
  );
}

// ===================================================================
// The pieces below are small components used by CalendarPage.
// They live OUTSIDE CalendarPage on purpose: a component defined inside
// another one is rebuilt from scratch on every redraw, which is slower and
// makes React forget things like which button had keyboard focus.
// They get everything they need through props instead.
// ===================================================================

// One event inside a calendar cell.
// A real <button>, so it works with the keyboard (Tab + Enter), not just the mouse.
// Props: event = the event, onSelect = called with the event's id to open its pop-up.
function EventChip({ event, onSelect }) {
  return (
    <button
      type="button"
      className="calendar-chip"
      onClick={() => onSelect(event.id)}
      title={`${formatTime(event.time)} ${event.eventTitle} · ${event.location}`} // full text on hover
    >
      <span className="calendar-chip-time">{formatTime(event.time)}</span>{" "}
      {event.eventTitle}
    </button>
  );
}

// Month view: a 7-column grid of days.
// Props: days (Dates on screen), anchor (to know which month is "this" month),
//        today, eventsByDate, onOpenDay (zoom into a day), onSelect (open an event).
function MonthView({ days, anchor, today, eventsByDate, onOpenDay, onSelect }) {
  return (
    <div className="calendar-grid calendar-month">
      {WEEKDAY_NAMES.map((name) => (
        <div key={name} className="calendar-weekday">{name}</div>
      ))}
      {days.map((day) => {
        const dayEvents = eventsByDate[toISODate(day)] ?? []; // ?? [] = no events that day
        const shown = dayEvents.slice(0, MAX_PER_MONTH_CELL);
        const hidden = dayEvents.length - shown.length;
        // CSS classes: days outside this month are faded, today is highlighted.
        const classes = ["calendar-cell"];
        if (day.getMonth() !== anchor.getMonth()) classes.push("is-other-month");
        if (isSameDay(day, today)) classes.push("is-today");
        return (
          <div key={toISODate(day)} className={classes.join(" ")}>
            <button
              type="button"
              className="calendar-day-number"
              onClick={() => onOpenDay(day)}
              aria-label={`Show ${day.toDateString()}`}
            >
              {day.getDate()}
            </button>
            {shown.map((event) => <EventChip key={event.id} event={event} onSelect={onSelect} />)}
            {hidden > 0 && (
              <button type="button" className="calendar-more" onClick={() => onOpenDay(day)}>
                +{hidden} more
              </button>
            )}
          </div>
        );
      })}
    </div>
  );
}

// Week view: 7 columns, every event listed by time.
function WeekView({ days, today, eventsByDate, onOpenDay, onSelect }) {
  return (
    <div className="calendar-grid calendar-week">
      {days.map((day) => {
        const dayEvents = eventsByDate[toISODate(day)] ?? [];
        return (
          <div key={toISODate(day)} className={`calendar-cell${isSameDay(day, today) ? " is-today" : ""}`}>
            <button type="button" className="calendar-week-heading" onClick={() => onOpenDay(day)}>
              {WEEKDAY_NAMES[day.getDay()]} <strong>{day.getDate()}</strong>
            </button>
            {dayEvents.length === 0 && <p className="calendar-empty-small">—</p>}
            {dayEvents.map((event) => <EventChip key={event.id} event={event} onSelect={onSelect} />)}
          </div>
        );
      })}
    </div>
  );
}

// Day view: a simple list with more detail per event.
// Props: dayEvents = that day's events (already sorted by time), onSelect.
function DayView({ dayEvents, onSelect }) {
  if (dayEvents.length === 0) {
    return <p className="calendar-empty">No events on this day.</p>;
  }
  return (
    <ul className="calendar-day-list">
      {dayEvents.map((event) => (
        <li key={event.id}>
          <button type="button" className="calendar-day-item" onClick={() => onSelect(event.id)}>
            <span className="calendar-day-time">{formatTime(event.time)}</span>
            <span>
              <strong>{event.eventTitle}</strong>
              <br />
              {event.host} · {event.location} · {event.format}
            </span>
            <span className="calendar-day-category">{event.category}</span>
          </button>
        </li>
      ))}
    </ul>
  );
}
