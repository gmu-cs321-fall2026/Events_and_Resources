
package edu.gmu.cs321.events_resources.repository;

import edu.gmu.cs321.events_resources.model.Event;
import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

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
}