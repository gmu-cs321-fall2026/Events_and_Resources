// Shows one event. Sprint 1: plain text, no styling.
export default function EventCard({ event }) {
  return (
    <li>
      <strong>{event.title}</strong> — {formatDate(event.dateTime)}
      <br />
      {event.location} · {event.category} · {event.format}
      {event.capacity > 0 && ` · ${event.registeredCount}/${event.capacity} registered`}
    </li>
  );
}

function formatDate(iso) {
  return new Date(iso).toLocaleString(undefined, {
    weekday: "short", month: "short", day: "numeric", hour: "numeric", minute: "2-digit",
  });
}