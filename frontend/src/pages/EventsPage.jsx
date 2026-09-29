// Sprint 1: a plain, unstyled list of events loaded from GET /events.
import { useEffect, useState } from "react";
import { getEvents } from "../api/eventsApi";
import EventCard from "../components/EventCard";

export default function EventsPage() {
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // useEffect(() => {
  //   getEvents()
  //     .then((data) => setEvents(data.items))
  //     .catch((err) => setError(err.message))
  //     .finally(() => setLoading(false));
  // }, []);


  // if (loading) return <p>Loading events…</p>;
  // if (error) return <p>Could not load events: {error}. Is the backend running on port 8080?</p>;
  // if (events.length === 0) return <p>No upcoming events.</p>;

    useEffect(() => {
    const data = getEvents();
    setEvents(data);
  }, []);

  if (events.length === 0) return <p>No upcoming events.</p>;

  return (
    <div>
      <h1>Upcoming Career Events</h1>
      <ul>
       {events.map((event, index) => (
          <EventCard key={index} event={event} />
        ))}
      </ul>
    </div>
  );
}