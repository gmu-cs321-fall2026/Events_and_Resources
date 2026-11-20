/**
 * EventsPage: the List view of the Events tab, loaded from GET /events.
 * Clicking an event opens its detail pop-up.
 *
 * The heading and the filters (search box, category chips, format pills) live in
 * EventsHubPage; this file only shows the matching events.
 *
 * Props:
 *   refreshKey = a number from EventsHubPage that changes after an event is created
 *   filters    = { q, category, format } (MASON-10); "" = not filtered
 *   onClear    = clears every filter (used by the "No events match" box)
 *   onModalClose = called when the event pop-up closes, so the side cards can reload
 */
import { useEffect, useState } from "react";
import { getEvents } from "../api/eventsApi";
import EventCard from "../components/EventCard";
import EventDetailModal from "../components/EventDetailModal";
import { describeFilters, hasFilters } from "../utils/eventSearch";

export default function EventsPage({ refreshKey, filters = {}, onClear, onModalClose }) {
  const [events, setEvents] = useState([]);
  const [totalCount, setTotalCount] = useState(0); // all matches on the server, not just this page
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // The event pop-up is open when this contains an event ID.
  // null means no pop-up is open.
  const [selectedEventId, setSelectedEventId] = useState(null);

  const { q = "", category = "", format = "" } = filters;

  /**
   * Loads the list after the page shows up, and again whenever an event is created
   * (refreshKey) or a filter changes. The ignore flag drops an old, slow answer that
   * arrives after a newer one.
   */
  useEffect(() => {
    let ignore = false;

    getEvents({ q, category, format })
      .then((data) => {
        if (ignore) return;
        setEvents(data.items); // the backend wraps the list: { items, page, pageSize, totalCount }
        setTotalCount(data.totalCount);
        setError(null);
      })
      .catch((err) => {
        if (!ignore) setError(err.message);
      })
      .finally(() => {
        if (!ignore) setLoading(false);
      });

    return () => {
      ignore = true;
    };
  }, [refreshKey, q, category, format]);

  if (loading) return <p>Loading events…</p>;
  if (error) return <p className="resources-error" role="alert">Couldn't load events: {error}</p>;

  if (events.length === 0) {
    return hasFilters(filters) ? (
      <div className="resources-empty" role="status">
        <h3>No events match {describeFilters(filters)}</h3>
        <p>Try another word, category or format, or clear your filters.</p>
        {onClear && <button type="button" className="clear-button" onClick={onClear}>Clear filters</button>}
      </div>
    ) : (
      <div className="resources-empty">
        <h3>No upcoming events yet</h3>
        <p>Career Services posts new fairs and workshops often. Check back soon.</p>
      </div>
    );
  }

  return (
    <div>
      {hasFilters(filters) && (
        <p className="events-filter-summary" role="status">
          {totalCount === 1 ? "1 event matches" : `${totalCount} events match`} {describeFilters(filters)}
        </p>
      )}
      <ul className="event-list">
        {events.map((event) => (
          <EventCard key={event.id} event={event} onSelect={() => setSelectedEventId(event.id)} />
        ))}
      </ul>

      {/* key={selectedEventId} = a brand-new pop-up for each event, so it never
          flashes the previous event's details while the new one loads. */}
      {selectedEventId !== null && (
        <EventDetailModal
          key={selectedEventId}
          eventId={selectedEventId}
          onClose={() => {
            setSelectedEventId(null);
            onModalClose?.();
          }}
        />
      )}
    </div>
  );
}
