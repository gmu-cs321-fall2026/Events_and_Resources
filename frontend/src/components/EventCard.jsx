// Shows one event. Sprint 1: plain text, no styling.
export default function EventCard({ event }) {
  return (
    <li>
      <strong>{event.eventTitle}</strong> — {event.date} {event.time}
      <br />
      {event.location} · {event.tag}
    </li>
  );
}

