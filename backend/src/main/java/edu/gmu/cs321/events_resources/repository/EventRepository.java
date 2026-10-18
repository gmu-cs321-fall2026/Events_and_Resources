
package edu.gmu.cs321.events_resources.repository;

import edu.gmu.cs321.events_resources.model.Event;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EventRepository {

    private final JdbcTemplate jdbc;

    public EventRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long createEvent(Event event) {

        String sql = """
            INSERT INTO events
            (event_title, host, event_date, event_time,
             location, tag, register_link, capacity)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;

        Long id = jdbc.queryForObject(
            sql,
            Long.class,
            event.eventTitle(),
            event.host(),
            java.sql.Date.valueOf(event.date()),
            java.sql.Time.valueOf(event.time() + ":00"),
            event.location(),
            event.tag(),
            event.registerLink(),
            event.capacity()
        );

        if (id == null) {
            throw new IllegalStateException("Event ID was not returned");
        }

        return id;
    }
}
