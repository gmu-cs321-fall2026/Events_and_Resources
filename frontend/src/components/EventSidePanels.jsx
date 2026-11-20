/**
 * EventSidePanels: the two cards on the right side of the Events tab.
 *
 *   NextEventsPanel = "Coming up": the next 3 events from today that match the page's
 *                     filters (search, type chip, format pill). GET /events?from=today&...
 *   MyEventsPanel   = "Your events": the next 3 UPCOMING events (today or later) the
 *                     logged-in user registered for. Past ones are in the calendar's
 *                     "My events" view.
 *                     GET /users/me/events/registered (through myEventsApi.js)
 *
 * They look like the "From the calendar" panel on the Resources page (SidePanels.css),
 * but each row is a <button> that opens the event pop-up instead of a link.
 *
 * Both use the same loading pattern as the rest of the site: remember which request
 * the answer belongs to ("key"), and an ignore flag drops an old answer that arrives
 * after a newer one.
 */
import { useEffect, useState } from "react";
import { getEvents } from "../api/eventsApi";
import { getMyRegisteredEventsInRange } from "../api/myEventsApi";
import { useAuth } from "../auth/useAuth";
import { describeFilters, hasFilters } from "../utils/eventSearch";
import "./SidePanels.css";

/** How many events each card shows. */
const HOW_MANY = 3;

const MONTHS = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];

/** Today as "YYYY-MM-DD" in the student's own time zone. */
function todayISO() {
  return new Date().toLocaleDateString("en-CA");
}

/** "15:00" -> "3:00 PM" */
function formatTime(hhmm) {
  const [h, m] = hhmm.split(":").map(Number);
  return `${((h + 11) % 12) + 1}:${String(m).padStart(2, "0")} ${h >= 12 ? "PM" : "AM"}`;
}

/**
 * Seats line for one event: "12 seats left", "Full · waitlist", or nothing when the
 * event has no limit (capacity 0 = unlimited).
 */
function seatsText(event) {
  if (!event.capacity) return null;
  const left = event.capacity - (event.registeredCount ?? 0);
  if (left <= 0) return { text: "Full · waitlist", full: true };
  return { text: `${left} seat${left === 1 ? "" : "s"} left`, full: false };
}

/**
 * One event row: date block, title, time + location, category and seats.
 * Props: event, onOpen = called with the event's id to open the pop-up.
 */
function EventRow({ event, onOpen }) {
  const [, month, day] = event.date.split("-");
  const seats = seatsText(event);
  return (
    <li>
      <button type="button" className="event-row event-row--button" onClick={() => onOpen(event.id)}>
        <span className="event-row__date">
          <span className="event-row__month">{MONTHS[Number(month) - 1]}</span>
          <span className="event-row__day">{Number(day)}</span>
        </span>
        <span className="event-row__text">
          <span className="event-row__title">{event.eventTitle}</span>
          <span className="event-row__when">{formatTime(event.time)} · {event.location}</span>
          <span className="event-row__category">{event.category}</span>
          {seats && (
            <span className={seats.full ? "event-row__seats event-row__seats--full" : "event-row__seats"}>
              {seats.text}
            </span>
          )}
        </span>
      </button>
    </li>
  );
}

/**
 * "Coming up" card.
 *
 * Props:
 *   filters   = { q, category, format } from EventsHubPage ("" = not filtered)
 *   reloadKey = changes after an event is created or the pop-up closes, so seats stay fresh
 *   onOpen    = opens the event pop-up
 *   onSeeAll  = switches the page to List view
 */
export function NextEventsPanel({ filters = {}, reloadKey = 0, onOpen, onSeeAll }) {
  const { q = "", category = "", format = "" } = filters;
  const requestKey = `${q}|${category}|${format}|${reloadKey}`;
  const [result, setResult] = useState({ key: null, items: [], error: null });

  useEffect(() => {
    let ignore = false;
    getEvents({ from: todayISO(), pageSize: HOW_MANY, q, category, format })
      .then((page) => {
        if (!ignore) setResult({ key: requestKey, items: page.items, error: null });
      })
      .catch((err) => {
        if (!ignore) setResult({ key: requestKey, items: [], error: err.message });
      });
    return () => {
      ignore = true;
    };
  }, [requestKey, q, category, format]);

  const loading = result.key !== requestKey;
  const filtered = hasFilters(filters);

  return (
    <section className="side-panel" aria-labelledby="next-events-heading">
      <span className="side-panel__eyebrow">Coming up</span>
      <h2 id="next-events-heading" className="side-panel__title">
        {filtered ? "Next matching events" : "Next events"}
      </h2>
      {filtered && <p className="side-panel__note">Matching {describeFilters(filters)}</p>}

      {loading && <p className="side-panel__note">Loading events…</p>}
      {!loading && result.error && (
        <p className="side-panel__note side-panel__note--error">Couldn't load events: {result.error}</p>
      )}
      {!loading && !result.error && result.items.length === 0 && (
        <p className="side-panel__note">
          {filtered ? "No upcoming events match. Try another word or type." : "No upcoming events yet."}
        </p>
      )}
      {!loading && !result.error && result.items.length > 0 && (
        <ul className="side-panel__list">
          {result.items.map((event) => <EventRow key={event.id} event={event} onOpen={onOpen} />)}
        </ul>
      )}

      {onSeeAll && (
        <button type="button" className="side-panel__button side-panel__button--gold" onClick={onSeeAll}>
          See all in List view
        </button>
      )}
    </section>
  );
}

/**
 * "Your events" card. Only asks the backend when someone is logged in; otherwise it
 * says to log in (the login bar is at the top of every page).
 *
 * Props:
 *   reloadKey = changes after the pop-up closes, so a new registration shows up
 *   onOpen    = opens the event pop-up
 */
export function MyEventsPanel({ reloadKey = 0, onOpen }) {
  const { user } = useAuth();
  const loggedIn = Boolean(user);
  const requestKey = `${user?.userId ?? ""}|${reloadKey}`;
  const [result, setResult] = useState({ key: null, items: [], error: null });

  useEffect(() => {
    if (!loggedIn) return;
    let ignore = false;
    getMyRegisteredEventsInRange(todayISO(), "9999-12-31")
      .then((items) => {
        if (!ignore) setResult({ key: requestKey, items: items.slice(0, HOW_MANY), error: null });
      })
      .catch((err) => {
        if (!ignore) setResult({ key: requestKey, items: [], error: err.message });
      });
    return () => {
      ignore = true;
    };
  }, [loggedIn, requestKey]);

  const loading = loggedIn && result.key !== requestKey;

  return (
    <section className="side-panel" aria-labelledby="my-events-heading">
      <span className="side-panel__eyebrow">Your events</span>
      <h2 id="my-events-heading" className="side-panel__title">Your upcoming events</h2>

      {!loggedIn && <p className="side-panel__note">Log in to see the events you registered for.</p>}
      {loading && <p className="side-panel__note">Loading your events…</p>}
      {loggedIn && !loading && result.error && (
        <p className="side-panel__note side-panel__note--error">Couldn't load your events: {result.error}</p>
      )}
      {loggedIn && !loading && !result.error && result.items.length === 0 && (
        <p className="side-panel__note">
          No upcoming registrations. Open an event to register, or switch the calendar to
          "My events" to see past ones.
        </p>
      )}
      {loggedIn && !loading && !result.error && result.items.length > 0 && (
        <ul className="side-panel__list">
          {result.items.map((event) => <EventRow key={event.id} event={event} onOpen={onOpen} />)}
        </ul>
      )}
    </section>
  );
}
