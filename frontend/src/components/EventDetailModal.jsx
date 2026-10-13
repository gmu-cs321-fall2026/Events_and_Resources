// The pop-up that shows one event's full details.
// It loads the event itself with GET /events/{id}, so it always shows the
// latest data (e.g. how many spots are left), not a stale copy from the list.
//
// Props:
//   eventId = which event to show
//   onClose = function EventsPage gives me; calling it closes the pop-up
import { useEffect, useState } from "react";
import { getEventById } from "../api/eventsApi";
import "./EventDetailModal.css";


// ----------------------------------------------------------------
// Helpers: turn "2026-10-20" and "15:00" into "Tuesday, October 20, 2026" and "3:00 PM".
// ----------------------------------------------------------------
function formatDate(date) {
  // Adding "T00:00" makes JavaScript read it as LOCAL midnight. Without it,
  // new Date("2026-10-20") is read as UTC and shows as Oct 19 in Virginia.
  const d = new Date(`${date}T00:00`);
  if (isNaN(d)) return date; // bad data -> just show what we got
  return d.toLocaleDateString(undefined, {
    weekday: "long", month: "long", day: "numeric", year: "numeric",
  });
}

function formatTime(time) {
  const d = new Date(`2000-01-01T${time}`); // the day doesn't matter, only the time
  if (isNaN(d)) return time;
  return d.toLocaleTimeString(undefined, { hour: "numeric", minute: "2-digit" });
}


export default function EventDetailModal({ eventId, onClose }) {
  // event = the details once they arrive. null = still loading.
  const [event, setEvent] = useState(null);
  const [error, setError] = useState(null);

  // Effect 1: load the event when the pop-up opens.
  useEffect(() => {
    let ignore = false; // ignore a stale answer
    getEventById(eventId)
      .then((data) => {
        if (!ignore) setEvent(data);
      })
      .catch((err) => {
        if (ignore) return;
        // 404 = the id doesn't exist (e.g. a staff member deleted it).
        setError(err.status === 404 ? "This event no longer exists." : err.message);
      });
    return () => {
      ignore = true;
    };
  }, [eventId]);

  // Effect 2: close the pop-up when the user presses Escape.
  // We listen on the whole window, and the cleanup removes the listener
  // when the pop-up closes, so we don't leave listeners piling up.
  useEffect(() => {
    function handleKeyDown(e) {
      if (e.key === "Escape") onClose();
    }
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [onClose]);


  // Decide what goes inside the box: loading, error, or the details.
  let content;
  if (error) {
    content = <p className="error">{error}</p>;
  } else if (!event) {
    content = <p>Loading event…</p>;
  } else {
    content = (
      <>
        {/* <> </> is a "fragment": groups elements without adding an extra <div> */}
        <span className="event-detail-tag">{event.tag}</span>
        <h2 id="event-detail-title">{event.eventTitle}</h2>

        {/* <dl> = a "description list": pairs of <dt> (label) and <dd> (value) */}
        <dl className="event-detail-fields">
          <dt>Host</dt>
          <dd>{event.host}</dd>

          <dt>Date</dt>
          <dd>{formatDate(event.date)}</dd>

          <dt>Time</dt>
          <dd>{formatTime(event.time)}</dd>

          <dt>Location</dt>
          <dd>{event.location}</dd>

          {/* only appear once the backend starts sending them. */}
          {event.format && (
            <>
              <dt>Format</dt>
              <dd>{event.format}</dd>
            </>
          )}
          {event.audience && (
            <>
              <dt>Audience</dt>
              <dd>{event.audience}</dd>
            </>
          )}
          {/* capacity 0 = unlimited, so only show spots for limited events */}
          {event.capacity > 0 && (
            <>
              <dt>Spots</dt>
              <dd>
                {event.registeredCount ?? 0} / {event.capacity} registered
              </dd>
            </>
          )}
        </dl>

        {event.description && <p>{event.description}</p>}

        {/* The Register button goes here. */}
      </>
    );
  }

  return (
    // The dark background behind the box. Clicking it closes the pop-up.
    <div className="modal-backdrop" onClick={onClose}>
      {/* The white box. role/aria-* tell screen readers "this is a pop-up dialog".
          stopPropagation: a click INSIDE the box would otherwise "bubble up"
          to the backdrop and close the pop-up. This stops that. */}
      <div
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="event-detail-title"
        onClick={(e) => e.stopPropagation()}
      >
        <button
          type="button"
          className="modal-close"
          onClick={onClose}
          aria-label="Close"
          autoFocus
        >
          ×
        </button>
        {content}
      </div>
    </div>
  );
}