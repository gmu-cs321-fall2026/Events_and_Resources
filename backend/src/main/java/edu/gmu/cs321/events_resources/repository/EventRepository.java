package edu.gmu.cs321.events_resources.repository;

import edu.gmu.cs321.events_resources.model.Event;
// Our record from model/: the checked filters for GET /events (null = "don't filter on this")
import edu.gmu.cs321.events_resources.model.EventFilter;
import java.time.LocalDate;
import java.time.LocalTime;
// Java's built-in lists: ArrayList is a list we can add to; List is the general type
import java.util.ArrayList;
import java.util.List;
// A box that holds either one value or nothing. findById returns one, so the
// caller must handle "no event with that id" instead of getting a surprise null.
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
// @Transactional: "run everything in this method as ONE database transaction":
// if any step fails, every step is undone, so we never save half an event.
import org.springframework.transaction.annotation.Transactional;

@Repository
public class EventRepository {

    private final JdbcTemplate jdbc;

    public EventRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

	// @Transactional because this method now runs TWO SQL statements (save the event,
	// then link its audiences). Either both happen, or neither does.
	@Transactional
	public Event insert(Event event, LocalDate date, LocalTime time) {
        Event saved = jdbc.queryForObject(
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

		linkAudiences(saved.id(), saved.audience());
		return saved;
    }

	/**
	 * Links a new event to its audiences in event_audiences, so ?audience= filters can find it.
	 *
	 * The Create Event form sends audience as ONE piece of text, e.g. "CS Majors, IT Majors".
	 * This turns it into one event_audiences row per audience name:
	 *   string_to_array(?, ',')  "CS Majors, IT Majors" -> {"CS Majors", " IT Majors"}  (split at commas)
	 *   unnest(...)              turns that array into rows, one name per row
	 *   trim / lower             ignore extra spaces and upper/lower case: " it majors" matches "IT Majors"
	 * Names that aren't in the audiences table are skipped (the event still saves;
	 * it just won't show up under that audience filter).
	 * ON CONFLICT DO NOTHING = if a pair is already linked, skip it instead of erroring.
	 */
	private void linkAudiences(long eventId, String audienceText) {
		jdbc.update(                                    // update() = run SQL that changes rows, no result needed
				"INSERT INTO event_audiences (event_id, audience_id) "
						+ "SELECT ?, a.id FROM audiences a "      // the new event's id + each matching audience's id
						+ "WHERE lower(a.name) IN ("
						+ "    SELECT lower(trim(part)) FROM unnest(string_to_array(?, ',')) AS part) "
						+ "ON CONFLICT DO NOTHING",
				eventId, audienceText);                 // fills in the two "?" in order
	}

	// -----------------------------------------------------------------------
	// READING EVENTS: GET /events and GET /events/{id}
	// -----------------------------------------------------------------------

	/**
	 * One page of events that match the filter, soonest first.
	 *
	 * ORDER BY event_date, event_time, id = by day, then time; id breaks ties so two
	 *   events at the same time always come back in the same order (pages don't shuffle).
	 * LIMIT  = return at most this many rows (the page size).
	 * OFFSET = skip this many rows first (to reach page 2, 3, ...).
	 */
	public List<Event> findPage(EventFilter filter, int limit, int offset) {
		Where where = buildWhere(filter);               // e.g. " WHERE category = ? AND format = ?"
		List<Object> params = new ArrayList<>(where.params()); // copy the filter values...
		params.add(limit);                              // ...then add the values for LIMIT ?
		params.add(offset);                             //    and OFFSET ?, in the same order as the SQL

		return jdbc.query(                              // query() = "I expect MANY rows back, give me a List"
				"SELECT " + EVENT_COLUMNS + " FROM events"
						+ where.sql()
						+ " ORDER BY event_date, event_time, id"
						+ " LIMIT ? OFFSET ?",
				EVENT_MAPPER,
				params.toArray());                      // toArray() = hand JdbcTemplate the values for every "?"
	}

	/** How many events match the filter in total (across ALL pages). Becomes "totalCount". */
	public long count(EventFilter filter) {
		Where where = buildWhere(filter);               // the SAME conditions as findPage, so the numbers agree
		Long count = jdbc.queryForObject(
				"SELECT COUNT(*) FROM events" + where.sql(),
				Long.class,                             // "give me the answer as a Long"
				where.params().toArray());
		return count == null ? 0 : count;               // ternary: if null use 0, otherwise use count
	}

	/**
	 * One event by its id. Returns an empty Optional if there is no such event.
	 * We use query() (a List) instead of queryForObject() because queryForObject()
	 * throws an exception when there are zero rows; an empty list is easier to handle.
	 */
	public Optional<Event> findById(long id) {
		List<Event> rows = jdbc.query(
				"SELECT " + EVENT_COLUMNS + " FROM events WHERE id = ?",
				EVENT_MAPPER,
				id);
		return rows.stream().findFirst();               // the first row if there is one, otherwise empty
	}

	// -----------------------------------------------------------------------
	// BUILDING THE WHERE CLAUSE
	// -----------------------------------------------------------------------

	/**
	 * A piece of SQL (" WHERE ... AND ...") plus the values for its "?" placeholders, in order.
	 * A record inside a class = a small helper type only this file uses.
	 */
	private record Where(String sql, List<Object> params) {}

	/**
	 * Turns the filter into SQL conditions. Each filter that's set adds ONE condition
	 * and ONE value; filters that are null add nothing.
	 *
	 * SAFETY: the user's values NEVER go into the SQL text. They only go into params,
	 * and JdbcTemplate puts them in the "?" spots safely. The SQL text itself only ever
	 * contains the fixed strings written below, so SQL injection isn't possible.
	 *
	 * Example: from=2026-11-01, category=Workshop, the rest null ->
	 *   sql    = " WHERE event_date >= ? AND category = ?"
	 *   params = [2026-11-01, "Workshop"]
	 */
	private Where buildWhere(EventFilter f) {
		List<String> conditions = new ArrayList<>();
		List<Object> params = new ArrayList<>();

		if (f.from() != null) {                         // on or after this day
			conditions.add("event_date >= ?");
			params.add(f.from());
		}
		if (f.to() != null) {                           // on or before this day
			conditions.add("event_date <= ?");
			params.add(f.to());
		}
		if (f.category() != null) {
			conditions.add("category = ?");
			params.add(f.category());
		}
		if (f.format() != null) {
			conditions.add("format = ?");
			params.add(f.format());
		}
		if (f.audience() != null) {
			// EXISTS (...) = "keep this event if the inner query finds at least one row":
			// is there an event_audiences link from THIS event (events.id) to an
			// audience with this name? lower() on both sides = ignore upper/lower case.
			conditions.add("EXISTS (SELECT 1 FROM event_audiences ea "
					+ "JOIN audiences a ON a.id = ea.audience_id "
					+ "WHERE ea.event_id = events.id AND lower(a.name) = lower(?))");
			params.add(f.audience());
		}

		// No filters at all -> no WHERE clause, so every event matches.
		// Otherwise glue the conditions together with AND: every one must be true.
		String sql = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
		return new Where(sql, params);
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
}