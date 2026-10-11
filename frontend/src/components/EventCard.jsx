// Shows one event. Clicking it opens the detail pop-up
// Props:
//   event    = the event to show
//   onSelect = function EventsPage gives me; I call it when the card is clicked

import "./EventCard.css";
export default function EventCard({ event, onSelect }) {
  return (
    <li>
     <button type="button" className="event-card" onClick={onSelect}>
      <strong>{event.eventTitle}</strong> — {event.date} {event.time}
      <br />
      {event.location} · {event.tag}
     </button>
    </li>
  );
}

