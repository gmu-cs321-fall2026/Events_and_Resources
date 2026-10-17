// The "Register" button inside the event pop-up (EventDetailModal).
//
// Props:
//   event        = the event being shown (needs id, capacity, registeredCount)
//   onRegistered = function the pop-up gives me; I call it after a successful
//                  registration so it can update the "x / y registered" count
//
// The button can be in 4 situations:
//   1. Ready         -> "Register"
//   2. Sending       -> "Registering…" (disabled, so a double-click can't send twice)
//   3. Registered    -> "You're registered ✓" (disabled)
//   4. Full          -> "Event full" (disabled)
import { useState } from "react";
import { registerForEvent } from "../api/eventsApi";
import "./RegisterButton.css";

export default function RegisterButton({ event, onRegistered }) {
  // State = values that, when they change, make React redraw the button.
  const [sending, setSending] = useState(false);       // true while waiting for the server
  const [registered, setRegistered] = useState(false); // true once this user has a spot
  const [message, setMessage] = useState(null);        // error text to show under the button

  // capacity 0 = unlimited, so it can only be full when capacity > 0.
  const isFull = event.capacity > 0 && (event.registeredCount ?? 0) >= event.capacity;

  async function handleClick() {
    setSending(true);
    setMessage(null);
    try {
      await registerForEvent(event.id); // POST /events/{id}/register
      setRegistered(true);
      onRegistered(); // tell the pop-up: one more person registered
    } catch (err) {
      // err.code comes from the backend's {"error", "code", "detail"} format.
      if (err.code === "ALREADY_REGISTERED") {
        setRegistered(true); // not really an error: you already have a spot
      } else if (err.code === "EVENT_FULL") {
        setMessage("Sorry, this event just filled up.");
      } else if (err.status === 401) {
        setMessage("Please log in to register.");
      } else {
        setMessage(err.message); // anything else: show what the server said
      }
    } finally {
      // "finally" runs whether it worked or failed: stop showing "Registering…".
      setSending(false);
    }
  }

  // Pick the label to show. Order matters: registered beats full
  // (if you took the last seat, you should see "registered", not "full").
  let label = "Register";
  if (registered) label = "You're registered ✓";
  else if (sending) label = "Registering…";
  else if (isFull) label = "Event full";

  return (
    <div className="register-area">
      <button
        type="button"
        className="register-button"
        onClick={handleClick}
        disabled={sending || registered || isFull}
      >
        {label}
      </button>
      {/* role="alert" makes screen readers announce the message when it appears */}
      {message && (
        <p className="register-message" role="alert">
          {message}
        </p>
      )}
    </div>
  );
}
