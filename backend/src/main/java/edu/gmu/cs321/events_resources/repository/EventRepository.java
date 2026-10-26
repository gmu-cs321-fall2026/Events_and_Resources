
package edu.gmu.cs321.events_resources.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import edu.gmu.cs321.events_resources.model.Event;

@Repository
public class EventRepository {

    private final JdbcTemplate jdbc;

    public EventRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

	public Event insert(Event event, LocalDate date, LocalTime time) {
        return jdbc.queryForObject( 
			"INSERT INTO events "
					+ "(event_title, host, event_date, event_time, location, "
					+ " category, audience, format, register_link, capacity) "
					+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " 
					+ "RETURNING " + EVENT_COLUMNS,
			EVENT_MAPPER,

			event.eventTitle(),
			event.host(),
			date,
			time, 
			event.location(),
			event.category(),
			event.audience(),
			event.format(),
			event.registerLink(),
			event.capacity());
    }
	
	private static final String EVENT_COLUMNS =
		"id, event_title, host, "
				+ "to_char(event_date, 'YYYY-MM-DD') AS date, "
				+ "to_char(event_time, 'HH24:MI') AS time, "
				+ "location, category, audience, format, register_link, capacity, registered_count";

	private static final RowMapper<Event> EVENT_MAPPER = (rs, rowNum) -> new Event(
		rs.getLong("id"),
		rs.getString("event_title"),
		rs.getString("host"),
		rs.getString("date"),          // the "AS date" column
		rs.getString("time"),          // the "AS time" column
		rs.getString("location"),
		rs.getString("category"),
		rs.getString("audience"),
		rs.getString("format"),
		rs.getString("register_link"), // null if the event has no link
		rs.getInt("capacity"),
		rs.getInt("registered_count"));

	/**
     * Our list of events from most recent. Used by GET /events?page=&pageSize=
     * ORDER BY event_date, event_time -> the contract says "sorted by date, then time".
     *          id is a tiebreaker: two events at the same date and time always come
     *          back in the same order, so an event can't jump between page 1 and page 2.
     * LIMIT  = return at most this many rows (the page size).
     * OFFSET = skip this many rows first (page 3 with pageSize 20 -> skip 40). makes NEXT PAGE POSSIBLE
	 * offset = (page − 1) × pageSize.
     * Uses EVENT_COLUMNS and EVENT_MAPPER (above, shared with insert),
     * so every method returns events in exactly the same shape.
     */
    public List<Event> findAll(int limit, int offset) {
        return jdbc.query(                          // query() = "I expect MANY rows back, give me a List"
                "SELECT " + EVENT_COLUMNS + " "
                        + "FROM events "
                        + "ORDER BY event_date, event_time, id "
                        + "LIMIT ? OFFSET ?",
                EVENT_MAPPER,                       // how to turn each row into an Event
                limit, offset);                     // fills in the two "?" in order
        // No events at all -> an empty List (never null), which becomes "items": []
    }

    /**
     * The one event with this id, or Optional.empty() if there isn't one.
     * Used by GET /events/{id}: empty -> 404 NOT_FOUND.
     * WHY query() AND NOT queryForObject()?
     *   queryForObject() throws an exception when zero rows come back.
     *   query() just returns an empty List, which is easier to turn into "not found".
     *   id is the PRIMARY KEY, so the List has at most one event in it.
     */
    public Optional<Event> findById(long id) {
        List<Event> rows = jdbc.query(
                "SELECT " + EVENT_COLUMNS + " FROM events WHERE id = ?",
                EVENT_MAPPER,
                id);                                // fills in the "?" (never glue id into the string)
        return rows.stream().findFirst();           // first event if there is one, otherwise empty
    }
}