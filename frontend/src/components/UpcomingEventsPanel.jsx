/**
 * UpcomingEventsPanel: "From the calendar", the next few events for the chosen major.
 * Uses REAL data: GET /events?from=today&audience=<major> (read only).
 *
 * Props:
 *   major         = the major chip that's selected ("All majors", "CS Majors", ...)
 *   calendarHref  = where "Open in calendar" goes. "#" until Trang's calendar (MASON-3) is pushed.
 *
 * useEffect runs again whenever major changes. The "ignore" flag drops an old answer
 * that arrives after a newer one (same pattern as EventsPage).
 */
import { useEffect, useState } from "react";
import { getUpcomingEvents } from "../api/resourcesApi";
import "./SidePanels.css";

const MONTHS = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];

function formatTime(hhmm) {
  const [h, m] = hhmm.split(":").map(Number);
  const suffix = h >= 12 ? "PM" : "AM";
  return `${((h + 11) % 12) + 1}:${String(m).padStart(2, "0")} ${suffix}`;
}

export default function UpcomingEventsPanel({ major, calendarHref = "#" }) {
    const [result, setResult] = useState({ major: null, items: [], error: null });

  useEffect(() => {
    let ignore = false;
    getUpcomingEvents(major)
      .then((items) => {
        if (!ignore) setResult({ major, items, error: null });
      })
      .catch((err) => {
        if (!ignore) setResult({ major, items: [], error: err.message });
      });
    return () => {
      ignore = true;
    };
  }, [major]);

  const loading = result.major !== major;
  const events = result.items;
  const error = result.error;
  const forWho = major === "All majors" ? "all majors" : major;

  return (
    <section className="side-panel" aria-labelledby="upcoming-heading">
      <span className="side-panel__eyebrow">From the calendar</span>
      <h2 id="upcoming-heading" className="side-panel__title">Upcoming for {forWho}</h2>

      {loading && <p className="side-panel__note">Loading events…</p>}
      {!loading && error && <p className="side-panel__note side-panel__note--error">Couldn't load events: {error}</p>}
      {!loading && !error && events.length === 0 && <p className="side-panel__note">No upcoming events for {forWho} yet.</p>}

      {!loading && !error && events.length > 0 && (
        <ul className="side-panel__list">
          {events.map((event) => {
            const [, month, day] = event.date.split("-");
            return (
              <li key={event.id}>
                <a className="event-row" href={calendarHref}>
                  <span className="event-row__date">
                    <span className="event-row__month">{MONTHS[Number(month) - 1]}</span>
                    <span className="event-row__day">{Number(day)}</span>
                  </span>
                  <span className="event-row__text">
                    <span className="event-row__title">{event.eventTitle}</span>
                    <span className="event-row__when">{formatTime(event.time)} · {event.location}</span>
                    <span className="event-row__category">{event.category}</span>
                  </span>
                </a>
              </li>
            );
          })}
        </ul>
      )}

      <a className="side-panel__button side-panel__button--gold" href={calendarHref}>
        <svg aria-hidden="true" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><rect x="3" y="5" width="18" height="16" rx="2" /><path d="M3 10h18M8 3v4M16 3v4" /></svg>
        Open in calendar
      </a>
    </section>
  );
}
