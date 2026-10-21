// ================================================================
// 1. IMPORTS: borrow things from other files
// ================================================================

// useState is a React "Hook" that gives my component memory.
// No "./" in the path = it comes from a package in node_modules.
import { useState } from "react";

// createEvent is a function that saves an event.
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
// The names match Event model, so everything connects.
const EMPTY_FORM = {
  eventTitle: "",   // e.g. "Capital One Tech Talk"
  host: "",         // the employer, e.g. "Capital One"
  date: "",         // e.g. "2026-10-20" (from the calendar picker)
  time: "",         // e.g. "15:00" (24-hour clock, from the time picker)
  location: "",     // e.g. "Horizon Hall"
  audience: "",     // e.g: "Undergrads", "Post-grads", "CS major"
  format: "",       // one of the FORMATS below
  category: "",     // one of the CATEGORIES below
  registerLink: "", // optional, e.g. "https://..."
  capacity: "",     // optional, e.g: 0 (or blank, as unlimited)
};

// The choices for the format dropdown. [] makes an array (a list).
const FORMATS = [ "Virtual", "In-person" ];

// The choices for the Category dropdown. [ ] makes an array (a list).
const CATEGORIES = [
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
//Props: onCreated = a function App.jsx gives me. I call it after an even is saved, so the events lists knows to reload
export default function CreateEventPage({ onCreated }) {

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
    // date, time, and category come from pickers, so they don't need it.
    const event = {
      eventTitle: form.eventTitle.trim(),
      host: form.host.trim(),
      date: form.date,
      time: form.time,
      location: form.location.trim(),
      audience: form.audience.trim(),
      format: form.format,
      category: form.category,
      registerLink: form.registerLink.trim() || null, // if blank, send null
      capacity: Number(form.capacity), // Number("") -> 0 means unlimited
    };

    // STEP 2:
    // try/catch: try this; if anything fails, jump to catch instead of crashing.
    try {
      // await = wait here until createEvent finishes saving.
      await createEvent(event);

      // STEP 3: success! Show a green message.
      // Backticks + ${ } = a template string that drops the title into the text.
      setMessage({ type: "success", text: `"${event.eventTitle}" was created!` });

      // STEP 4: clear every box so the next event can be typed.
      setForm(EMPTY_FORM);

      // STEP 5: tell App.jsx, so the list reloads. ?.() = only call it if it was passed in.
      onCreated?.();

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
      <h1> Create Event</h1>

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

        {/* Audience */}
        <label>
          Audience
          <input
            name="audience"
            value={form.audience}
            onChange={handleChange}
            placeholder="All Students"
            required
          />
        </label>

	{/* Format: a dropdown. <select> is the menu, <option> is each choice. */}
        <label>
          Format
          <select name="format" value={form.format} onChange={handleChange} required>
            {/* value="" = "nothing picked yet", so required blocks Submit */}
            <option value="">-- choose a format --</option>

            {/* .map = a loop: for each format in FORMATS, make one <option>.
                key = a unique ID React needs for every item in a list. */}
            {FORMATS.map((format) => (
              <option key={format} value={format}>
                {format}
              </option>
            ))}
          </select>
        </label>

        {/* Category: a dropdown. <select> is the menu, <option> is each choice. */}
        <label>
          Category
          <select name="category" value={form.category} onChange={handleChange} required>
            {/* value="" = "nothing picked yet", so required blocks Submit */}
            <option value="">-- choose a category --</option>

            {/* .map = a loop: for each category in CATEGORIES, make one <option>.
                key = a unique ID React needs for every item in a list. */}
            {CATEGORIES.map((category) => (
              <option key={category} value={category}>
                {category}
              </option>
            ))}
          </select>
        </label>

        {/* Register Link (optional: no "required") */}
        <label>
          Register Link (optional)
          {/* type="url" = the browser checks it looks like a web address */}
          <input
            type="url"
            name="registerLink"
            value={form.registerLink}
            onChange={handleChange}
            placeholder="https://example.com/register"
          />
        </label>

        {/* Capacity (optional). */}
        <label>
          Capacity (leave blank for unlimited)
          {/* type="number" + min="0" = the browser only allows whole numbers 0 or more */}
          <input
            type="number"
            name="capacity"
            value={form.capacity}
            onChange={handleChange}
            min="0"
            step="1"
            placeholder="30"
          />
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
