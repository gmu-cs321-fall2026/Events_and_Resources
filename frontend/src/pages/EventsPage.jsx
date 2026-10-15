//The list of upcoming events, loaded from GET /events
//Clicking an event opens its detail pop-up

import { useEffect, useState } from "react";
import { getEvents } from "../api/eventsApi";
import EventCard from "../components/EventCard";
import EventDetailModal from "../components/EventDetailModal";

// Props: refreshKey = a number from App.jsx that changes after an event is created.
export default function EventsPage({ refreshKey }) {
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // The vent pop-up is open when this contains an event ID.
  // null means no pop-up is open.
  const [selectedEventId, setSelectedEventId] = useState(null);

  // Run this after the page shows up.
  // Run it again whenever refreshKey changes.
  useEffect(() => { // protects against an old, slow request finishing AFTER a newer one and overwriting the list. React's StrictMode also runs effects twice in development, so you'd see two requests in the Network tab; this keeps only the latest answer.
  let ignore = false;

  getEvents()
     .then((data) => {
        if (ignore) return;
        setEvents(data.items); //the backend wraps the list: { items, page, pageSize, totalCount }
        setError(null);
     })

     .catch((err) => {
        if (!ignore) setError(err.message);
     })

     .finally(() => {
        if (!ignore) setLoading(false);
     });

   //The "cleanup" function: React runs it before the effect runs again.
   return () => {
     ignore = true;
   };
  }, [refreshKey]);

  if (loading) return <p>Loading events…</p>;
  if (error) return <p className="error">Could not load events: {error}</p>;
  if (events.length === 0) return <p>No upcoming events.</p>;

  return (
    <div>
      <h1>Upcoming Career Events</h1>
      <ul className="event-list">
      {/* key = event.id now, since the server gives every event a unique id */}
       {events.map((event) => (
          <EventCard key={event.id} event={event} onSelect={() => setSelectedEventId(event.id)}/>
        ))}
      </ul>

      {/* Only show the pop-up when an event is picked.
          key={selectedEventId} = a brand-new pop-up for each event, so it never
          flashes the previous event's details while the new one loads. */}
      {selectedEventId !== null && (
        <EventDetailModal
          key={selectedEventId}
          eventId={selectedEventId}
          onClose={() => setSelectedEventId(null)}
        />
      )}

    </div>
  );
}