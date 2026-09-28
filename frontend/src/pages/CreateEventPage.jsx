// Yasmin job: show a form -> remember what the user types -> when they click
// Submit, pack it into an event object -> hand it to Person 2's API.

// ================================================================
// 1. IMPORTS: borrow things from other files
// ================================================================

// useState is a React "Hook" that gives my component memory.
// No "./" in the path = it comes from a package in node_modules.
import { useState } from "react";

// createEvent is Person 2's function that saves an event.
// "../" = go up one folder (pages -> src), then into api/eventsApi.js
import { createEvent } from "../api/eventsApi";

// Load my styles. No { } because I'm not borrowing a name,
// just applying the CSS. "./" = same folder as this file.
import "./CreateEventPage.css";


// ================================================================
// 2. CONSTANTS: values that never change (kept OUTSIDE the function
//    so they aren't rebuilt every time the page re-renders)
// ================================================================

// The blank form. One line per box on the page.
// The names match Person 1's Event model, so everything connects.
const EMPTY_FORM = {
  eventTitle: "",   // e.g. "Capital One Tech Talk"
  host: "",         // the employer, e.g. "Capital One"
  date: "",         // e.g. "2026-10-20" (from the calendar picker)
  time: "",         // e.g. "15:00" (24-hour clock, from the time picker)
  location: "",     // e.g. "Horizon Hall"
  tag: "",          // one of the TAGS below
};

// The choices for the Tag dropdown. [ ] makes an array (a list).
const TAGS = [
  "Internal",
  "Career Fair",
  "Workshop",
  "Employer Info Session",
  "Networking",
  "Panel",
];


// ================================================================
// 3. THE COMPONENT: a function that returns what shows on screen.
//    "export default" = this is the main thing this file shares,
//    so App.jsx can import it.
// ================================================================
export default function CreateEventPage() {

  // ---------------------------------------------------------------
  // a. STATE (memory)
  // useState gives back 2 things: [current value, function to change it].
  // When the setter is called, React re-runs this whole function and
  // updates the screen. (A normal variable would reset on every re-run.)
  // ---------------------------------------------------------------

  // form = what's typed in every box right now. Starts blank.
  const [form, setForm] = useState(EMPTY_FORM);

  // message = the green/red text after Submit. null = nothing to show yet.
  const [message, setMessage] = useState(null);


  // ---------------------------------------------------------------
  // b. FUNCTIONS
  // ---------------------------------------------------------------

  // Runs on EVERY keystroke in ANY box (hooked up with onChange below).
  // e = the "event" the browser reports (what happened, and where).
  function handleChange(e) {
    // e.target = the box that was typed in.
    // Grab its name (which field, e.g. "host") and value (what's in it).
    const { name, value } = e.target;

    // Make a NEW form object:
    //   ...form       -> copy every field from the old form
    //   [name]: value -> then overwrite just the one that changed
    // (Never change state directly; always give React a new copy.)
    setForm({ ...form, [name]: value });
  }

  // Runs when the form is submitted (Submit button or Enter key).
  // async = this function is allowed to "await" (wait for) things.
  async function handleSubmit(e) {
    // Browsers reload the page on submit by default. Stop that,
    // or everything the user typed would be lost.
    e.preventDefault();

    // STEP 1: pack the event (the "order ticket").
    // .trim() removes extra spaces at the start/end of typed text.
    // date, time, and tag come from pickers, so they don't need it.
    const event = {
      eventTitle: form.eventTitle.trim(),
      host: form.host.trim(),
      date: form.date,
      time: form.time,
      location: form.location.trim(),
      tag: form.tag,
    };

    // STEP 2: hand it to Person 2.
    // try/catch: try this; if anything fails, jump to catch instead of crashing.
    try {
      // await = wait here until createEvent finishes saving.
      await createEvent(event);

      // STEP 3: success! Show a green message.
      // Backticks + ${ } = a template string that drops the title into the text.
      setMessage({ type: "success", text: `"${event.eventTitle}" was created!` });

      // STEP 4: clear every box so the next event can be typed.
      setForm(EMPTY_FORM);
    } catch (err) {
      // Something went wrong: show a red message with the error's description.
      setMessage({ type: "error", text: "Could not create event: " + err.message });
    }
  }


  // ---------------------------------------------------------------
  // c. WHAT SHOWS ON SCREEN (JSX)
  // Looks like HTML, but it's JavaScript:
  //   - className instead of class
  //   - { } lets me drop JavaScript values into the page
  //   - comments inside JSX use {/* */} instead of //
  // ---------------------------------------------------------------
  return (
    // One outer box. className connects it to my CSS (.create-event).
    <div className="create-event">
      <h1> Create Event at GMU</h1>

      {/* When this form is submitted, run handleSubmit.
          No () after handleSubmit: I'm passing the function, not calling it. */}
      <form onSubmit={handleSubmit}>

        {/* EVERY box follows the same pattern:
              name        = which field in form it belongs to (must match EMPTY_FORM)
              value       = show what's saved in memory
              onChange    = run handleChange on every keystroke
              placeholder = gray example text
              required    = browser blocks Submit if it's empty */}

        {/* Event Title */}
        <label>
          Event Title
          <input
            name="eventTitle"
            value={form.eventTitle}
            onChange={handleChange}
            placeholder="Resume Workshop"
            required
          />
        </label>

        {/* Employer / Host */}
        <label>
          Employer / Host
          <input
            name="host"
            value={form.host}
            onChange={handleChange}
            placeholder="Career Services"
            required
          />
        </label>

        {/* Date and Time: two separate boxes, side by side.
            className="row" is the CSS that lines them up. */}
        <div className="row">
          <label>
            Date
            {/* type="date" = the browser shows a calendar picker */}
            <input
              type="date"
              name="date"
              value={form.date}
              onChange={handleChange}
              required
            />
          </label>
          <label>
            Time
            {/* type="time" = the browser shows a clock picker */}
            <input
              type="time"
              name="time"
              value={form.time}
              onChange={handleChange}
              required
            />
          </label>
        </div>

        {/* Location */}
        <label>
          Location
          <input
            name="location"
            value={form.location}
            onChange={handleChange}
            placeholder="Horizon Hall"
            required
          />
        </label>

        {/* Tag: a dropdown. <select> is the menu, <option> is each choice. */}
        <label>
          Tag
          <select name="tag" value={form.tag} onChange={handleChange} required>
            {/* value="" = "nothing picked yet", so required blocks Submit */}
            <option value="">-- choose a tag --</option>

            {/* .map = a loop: for each tag in TAGS, make one <option>.
                key = a unique ID React needs for every item in a list. */}
            {TAGS.map((tag) => (
              <option key={tag} value={tag}>
                {tag}
              </option>
            ))}
          </select>
        </label>

        {/* type="submit" = clicking this submits the form -> handleSubmit runs */}
        <button type="submit">Submit</button>
      </form>

      {/* Show the message ONLY if there is one.
          && means "if the left side exists, show the right side."
          className becomes "success" or "error" -> green or red in the CSS. */}
      {message && <p className={message.type}>{message.text}</p>}
    </div>
  );
}
