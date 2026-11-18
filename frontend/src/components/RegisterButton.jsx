// The "Register" button inside the event pop-up (EventDetailModal).
//
// Props:
  //   event        = the event being shown (needs id, capacity, registeredCount)
  //   onRegistered = called after registering, so the pop-up adds 1 to the spot count
  //   onCancelled  = called after cancelling, so the pop-up takes 1 away
  //
  // MASON-18: when the event is full, the button becomes "Join waitlist".
  // While waiting it shows "On the waitlist · #2". If a seat opens (someone cancels
  // or staff raise the capacity) the backend registers the student automatically,
  // and the next time the pop-up opens it shows "You're registered ✓".

import { useEffect, useState } from "react";
import { registerForEvent, cancelRegistration, getMyRegistration } from "../api/eventsApi";
import { joinWaitlist, leaveWaitlist } from "../api/waitlistApi";
import "./RegisterButton.css";

 // ---- State ----
  // checking:   true until the server tells us whether we're registered
  // registered: true once this user has a spot
  // busy:       true while a register or cancel request is on its way (blocks double-clicks)
  // confirming: true while the "Are you sure?" question is showing
  // message:    a short note under the buttons, e.g. "Your registration was cancelled."
  // isError:    true = show the message in red (and announce it as an alert)
  // waitlistPosition: MASON-18. null = not waiting; 1 = next in line for a seat
  // filledUp:   MASON-18. true once the server said EVENT_FULL, even if our copy of the count is old
export default function RegisterButton({ event, onRegistered, onCancelled }) { 
  const [checking, setChecking] = useState(true);
  const [registered, setRegistered] = useState(false);
  const [busy, setBusy] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [message, setMessage] = useState(null);
  const [isError, setIsError] = useState(false);
  const [waitlistPosition, setWaitlistPosition] = useState(null);
  const [filledUp, setFilledUp] = useState(false);

  const isFull = filledUp || (event.capacity > 0 && (event.registeredCount ?? 0) >= event.capacity);

// Set every message in the same way.
  function showMessage(text, error) {
    setMessage(text);
    setIsError(error);
  }

  /** Copy the server's answer from GET /events/{id}/register into our state. */
  function applyStatus(status) {
    setRegistered(status.registered);
    setWaitlistPosition(status.waitlisted ? status.waitlistPosition : null);
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
        if (!ignore) applyStatus(status);
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
        setFilledUp(true);
        showMessage("Sorry, this event just filled up. You can join the waitlist instead.", true);
      } else if (err.status === 401) {
        showMessage("Please log in to register.", true);
      } else {
        showMessage(err.message, true);
      }
    } finally {
      setBusy(false);
    }
  }

  // ---- MASON-18: join the waitlist ----
  // SPOTS_AVAILABLE = a seat opened since the pop-up loaded, so just register instead.
  async function handleJoinWaitlist() {
    setBusy(true);
    setMessage(null);
    try {
      const status = await joinWaitlist(event.id);
      applyStatus(status);
      showMessage("You're on the waitlist. If a spot opens, you'll be registered automatically.", false);
    } catch (err) {
      if (err.code === "SPOTS_AVAILABLE") {
        setFilledUp(false);
        setBusy(false);
        await handleRegister();
        return;
      } else if (err.code === "ALREADY_WAITLISTED" || err.code === "ALREADY_REGISTERED") {
        applyStatus(await getMyRegistration(event.id));
      } else if (err.status === 401) {
        showMessage("Please log in to join the waitlist.", true);
      } else {
        showMessage(err.message, true);
      }
    } finally {
      setBusy(false);
    }
  }

  // ---- MASON-18: leave the waitlist ----
  // NOT_WAITLISTED usually means good news: a seat opened and they were moved in.
  async function handleLeaveWaitlist() {
    setBusy(true);
    setMessage(null);
    try {
      await leaveWaitlist(event.id);
      setWaitlistPosition(null);
      showMessage("You left the waitlist.", false);
    } catch (err) {
      if (err.code === "NOT_WAITLISTED") {
        const status = await getMyRegistration(event.id);
        applyStatus(status);
        if (status.registered) {
          onRegistered();
          showMessage("Good news: a spot opened and you're registered!", false);
        }
      } else if (err.status === 401) {
        showMessage("Please log in to leave the waitlist.", true);
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
  // registered + confirming: Before confirming cancellation, show confirming question 
  // to prevent losing a seat by a slip of the mouse and "Keep my spot" is the easy way back out.
  //
  // registered: Shows the status, can't be clicked. Only registered events can be cancelled.
  //
  // waitlisted (MASON-18): shows their place in line, plus a "Leave waitlist" link.
  //
  // not registered: Register button, or "Join waitlist" when the event is full
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
        <button type="button" className="register-button" disabled>
          You're registered ✓
        </button>
      
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
  } else if (waitlistPosition !== null) {
    buttons = (
      <>
        <button type="button" className="register-button" disabled>
          On the waitlist · #{waitlistPosition}
        </button>
        <p className="waitlist-note">
          If a spot opens up, you'll be registered automatically.
        </p>
        <button type="button" className="cancel-link" onClick={handleLeaveWaitlist} disabled={busy}>
          {busy ? "Leaving…" : "Leave waitlist"}
        </button>
      </>
    );
  } else if (isFull) {
    buttons = (
      <>
        <button type="button" className="register-button" onClick={handleJoinWaitlist} disabled={busy}>
          {busy ? "Joining…" : "Join waitlist"}
        </button>
        <p className="waitlist-note">This event is full. Join the waitlist to get the next open spot.</p>
      </>
    );
  } else {
    buttons = (
      <button
        type="button"
        className="register-button"
        onClick={handleRegister}
        disabled={busy}
      >
        {busy ? "Registering…" : "Register"}
      </button>
    );
  }

  return (
    <div className="register-area">
      {buttons}

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
