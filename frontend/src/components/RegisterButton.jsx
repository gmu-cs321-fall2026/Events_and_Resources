// The "Register" button inside the event pop-up (EventDetailModal).
//
// Props:
//   event        = the event being shown (needs id, capacity, registeredCount)
//   onRegistered = function the pop-up gives me; I call it after a successful
//                  registration so it can update the "x / y registered" count
//   onCancelled  = function the pop-up gives me; I call it after
//
// What the student can see:
//   1. Checking          -> "Checking…" (disabled) while we ask the server "am I registered?"
//   2. Not registered    -> "Register", or "Event full" (disabled) if there are no seats
//   3. Registering       -> "Registering…" (disabled, so a double-click can't send twice)
//   4. Registered        -> "You're registered ✓" + a "Cancel registration" button
//   5. Confirming        -> "Cancel your spot?" + "Yes, cancel" / "Keep my spot"
//   6. Cancelling        -> "Cancelling…" (disabled)

import { useState } from "react";
import { registerForEvent } from "../api/eventsApi";
import "./RegisterButton.css";

 // ---- State ----
  // checking:   true until the server tells us whether we're registered
  // registered: true once this user has a spot
  // busy:       true while a register or cancel request is on its way (blocks double-clicks)
  // confirming: true while the "Are you sure?" question is showing
  // message:    a short note under the buttons, e.g. "Your registration was cancelled."
  // isError:    true = show the message in red (and announce it as an alert)
export default function RegisterButton({ event, onRegistered, onCanceled }) { 
  const [checking, setChecking] = useState(true);
  const [registered, setRegistered] = useState(false);
  const [busy, setBusy] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [message, setMessage] = useState(null);
  const [isError, setIsError] = useState(false);

  const isFull = event.capacity > 0 && (event.registeredCount ?? 0) >= event.capacity;

// Set every message in the same way.
  function showMessage(text, error) {
    setMessage(text);
    setIsError(error);
  }

  // ---- When the pop-up opens: "am I already registered?" ----
  // Button used to start as "Register", even if you had a spot,
  // so you'd only find out after clicking. Now we ask first.
  // Run again only if the pop-up switches to a different event.
  // If the check fails (e.g. 401 not logged in), just show "Register".
  // Clicking it will then show the real reason ("Please log in to register.").
  useEffect(() => {
    let ignore = false;
    getMyRegistration(event.id)
      .then((status) => {
        if (!ignore) setRegistered(status.registered);
      })
      .catch(() => {
      })
      .finally(() => {
        if (!ignore) setChecking(false);
      });
    return () => {
      ignore = true;
    };
  }, [event.id]);

  // ---- Register ----
  async function handleRegister() {
    setBusy(true);
    setMessage(null);
    try {
      await registerForEvent(event.id);
      setRegistered(true);
      onRegistered();
    } catch (err) {
      if (err.code === "ALREADY_REGISTERED") {
        setRegistered(true);
      } else if (err.code === "EVENT_FULL") {
        showMessage("Sorry, this event just filled up.", true);
      } else if (err.status === 401) {
        showMessage("Please log in to register.", true);
      } else {
        showMessage(err.message, true);
      }
    } finally {
      setBusy(false);
    }
  }

  // ---- Cancel: step 2 (after "Yes, cancel") ----
  async function handleCancel() {
    setBusy(true);
    setMessage(null);
    try {
      await cancelRegistration(event.id);
      setRegistered(false);
      onCancelled();
      showMessage("Your registration was cancelled.", false);
    } catch (err) {
      if (err.code === "NOT_REGISTERED") {
        setRegistered(false);
        showMessage("You weren't registered for this event anymore.", false);
      } else if (err.status === 401) {
        showMessage("Please log in to cancel your registration.", true);
      } else {
        showMessage(err.message, true);
      }
    } finally {
      setBusy(false);
      setConfirming(false);
    }
  }

  // ---- What to draw ----
  // checking: Still asking the server. Disabled, so nobody registers twice by accident.
  //
  // registered + confirming: Before confirming cancellation, show corfirming question 
  // to prevent losing a seat by a slip of the mouse and "Keep my spot" is the easy way back out.
  //
  // registered: Shows the status, can't be clicked. Only registered events can be cancelled.
  //
  // not registered: Only show Register button
  let buttons;

  if (checking) {
    buttons = (
      <button type="button" className="register-button" disabled>
        Checking…
      </button>
    );
  } else if (registered && confirming) {
    buttons = (
      <div className="cancel-confirm">
        <p className="cancel-confirm-question">
          Cancel your spot? {event.capacity > 0 && "Someone else may take it."}
        </p>
        <div className="cancel-confirm-buttons">
          <button type="button" className="cancel-button" onClick={handleCancel} disabled={busy}>
            {busy ? "Cancelling…" : "Yes, cancel"}
          </button>
          {}
          <button
            type="button"
            className="keep-button"
            onClick={() => setConfirming(false)}
            disabled={busy}
            autoFocus
          >
            Keep my spot
          </button>
        </div>
      </div>
    );
  } else if (registered) {
    buttons = (
      <>
        {}
        <button type="button" className="register-button" disabled>
          You're registered ✓
        </button>
        {}
        <button
          type="button"
          className="cancel-link"
          onClick={() => {
            setMessage(null);
            setConfirming(true);
          }}
        >
          Cancel registration
        </button>
      </>
    );
  } else {
    let label = "Register";
    if (busy) label = "Registering…";
    else if (isFull) label = "Event full";
    buttons = (
      <button
        type="button"
        className="register-button"
        onClick={handleRegister}
        disabled={busy || isFull}
      >
        {label}
      </button>
    );
  }

  return (
    <div className="register-area">
      {buttons}
      {}
      {message && (
        <p
          className={isError ? "register-message" : "register-message register-message-ok"}
          role={isError ? "alert" : "status"}
        >
          {message}
        </p>
      )}
    </div>
  );
}
