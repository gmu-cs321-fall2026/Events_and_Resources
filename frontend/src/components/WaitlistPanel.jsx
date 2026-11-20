/**
 * WaitlistPanel (MASON-19): the staff/admin box inside the event pop-up.
 *
 * Staff see who is waiting for a seat (first in line at the top) and can change the
 * event's capacity. Raising the capacity opens seats, and the backend moves people
 * off the waitlist into those seats automatically, first come first served.
 * Example: 30 seats, 30 registered, 1 waiting -> set capacity to 35 -> that 1 person is registered.
 *
 * Works for unlimited events too (capacity 0): there's no waitlist then, so the panel
 * only offers to set a limit. Typing 0 makes a limited event unlimited again.
 *
 * Only EventDetailModal shows this, and only when useAuth().isStaff is true (logged in as
 * staff or admin). The backend checks the role on every request too, so hiding it is not the
 * only protection.
 *
 * Props:
 *   event             = the event in the pop-up (needs id, capacity, registeredCount)
 *   onCapacityChanged = called with { capacity, registeredCount } after a change,
 *                       so the pop-up's "x / y registered" line updates right away
 */
import { useEffect, useState } from "react";
import { changeCapacity, getWaitlist } from "../api/waitlistApi";
import "./WaitlistPanel.css";

/** "2026-11-17T09:30:00" -> "Nov 17, 9:30 AM" */
function formatJoined(joinedAt) {
  const d = new Date(joinedAt);
  if (isNaN(d)) return joinedAt;
  return d.toLocaleString(undefined, { month: "short", day: "numeric", hour: "numeric", minute: "2-digit" });
}

/** Shared Core user ids are long UUIDs; the last block is enough to tell people apart on screen. */
function shortId(userId) {
  return "…" + String(userId).slice(-6);
}

export default function WaitlistPanel({ event, onCapacityChanged }) {
  /**
   * ---- State ----
   * view:        the server's WaitlistView, null while loading
   * newCapacity: what's typed in the capacity box (a string, because inputs hold text)
   * busy:        true while the capacity change is on its way
   * message:     a note under the form; isError = show it in red
   */
  const [view, setView] = useState(null);
  const [loadError, setLoadError] = useState(null);
  const [newCapacity, setNewCapacity] = useState(String(event.capacity));
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState(null);
  const [isError, setIsError] = useState(false);

  /** ---- Load the waitlist when the pop-up opens (or switches to another event) ---- */
  useEffect(() => {
    let ignore = false;
    getWaitlist(event.id)
      .then((data) => {
        if (ignore) return;
        setView(data);
        setNewCapacity(String(data.capacity));
      })
      .catch((err) => {
        if (!ignore) setLoadError(err.message);
      });
    return () => {
      ignore = true;
    };
  }, [event.id]);

  /** ---- Save a new capacity ---- */
  async function handleSubmit(e) {
    e.preventDefault();
    setBusy(true);
    setMessage(null);
    try {
      const updated = await changeCapacity(event.id, Number(newCapacity));
      setView(updated);
      setNewCapacity(String(updated.capacity));
      onCapacityChanged({ capacity: updated.capacity, registeredCount: updated.registeredCount });

      const accepted = updated.justAccepted.length;
      const limit = updated.capacity === 0 ? "unlimited" : updated.capacity;
      const people = accepted === 1 ? "1 person" : `${accepted} people`;
      setMessage(`Capacity is now ${limit}. ${accepted > 0 ? `Accepted ${people} from the waitlist.` : ""}`);
      setIsError(false);
    } catch (err) {
      setMessage(err.message);
      setIsError(true);
    } finally {
      setBusy(false);
    }
  }

  /** ---- What to draw ---- */
  if (loadError) {
    return (
      <section className="waitlist-panel">
        <h3>Waitlist</h3>
        <p className="waitlist-panel-message waitlist-panel-error" role="alert">{loadError}</p>
      </section>
    );
  }
  if (!view) {
    return (
      <section className="waitlist-panel">
        <h3>Waitlist</h3>
        <p>Loading waitlist…</p>
      </section>
    );
  }

  const unlimited = view.capacity === 0;
  const waiting = view.waitlist.length;
  // "Let everyone in" = exactly enough seats for the people registered plus everyone waiting.
  const fitsEveryone = view.registeredCount + waiting;

  return (
    <section className="waitlist-panel" aria-labelledby="waitlist-heading">
      <h3 id="waitlist-heading">
        {unlimited ? "Capacity" : "Waitlist"}{" "}
        <span className="waitlist-panel-count">{unlimited ? "unlimited" : `${waiting} waiting`}</span>
      </h3>

      {unlimited ? (
        <p className="waitlist-panel-empty">
          This event has no seat limit, so there's no waitlist. Set a number to limit seats.
        </p>
      ) : waiting === 0 ? (
        <p className="waitlist-panel-empty">Nobody is waiting for this event.</p>
      ) : (
        <ol className="waitlist-panel-list">
          {view.waitlist.map((person) => (
            <li key={person.userId}>
              <span className="waitlist-panel-position">#{person.position}</span>
              <span title={person.userId}>Student {shortId(person.userId)}</span>
              <span className="waitlist-panel-joined">joined {formatJoined(person.joinedAt)}</span>
            </li>
          ))}
        </ol>
      )}

      <form className="waitlist-panel-form" onSubmit={handleSubmit}>
        <label htmlFor="waitlist-capacity">Capacity</label>
        <input
          id="waitlist-capacity"
          type="number"
          min={0}
          value={newCapacity}
          onChange={(e) => setNewCapacity(e.target.value)}
          required
        />
        <button type="submit" disabled={busy || newCapacity === ""}>
          {busy ? "Saving…" : "Update capacity"}
        </button>
      </form>

      {waiting > 0 && (
        <button
          type="button"
          className="waitlist-panel-fit"
          onClick={() => setNewCapacity(String(fitsEveryone))}
          disabled={busy}
        >
          Set to {fitsEveryone} (room for everyone waiting)
        </button>
      )}

      <p className="waitlist-panel-hint">
        {view.registeredCount} registered. 0 = unlimited.{" "}
        {unlimited
          ? `A limit can't be below ${view.registeredCount}.`
          : `Raising the capacity registers people from the waitlist automatically, in order. It can't go below ${view.registeredCount}.`}
      </p>

      {message && (
        <p
          className={isError ? "waitlist-panel-message waitlist-panel-error" : "waitlist-panel-message"}
          role={isError ? "alert" : "status"}
        >
          {message}
        </p>
      )}
    </section>
  );
}
