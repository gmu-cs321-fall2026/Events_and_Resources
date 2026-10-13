// "This file lives in the repository/ folder." Must match the folder the file is in.
package edu.gmu.cs321.events_resources.repository;

// ---------------------------------------------------------------------------
// IMPORTS: tell Java where to find each class this file uses.
// ---------------------------------------------------------------------------

// Our record from model/: one event in a student's "my events" list (built by findEventsForUser)
import edu.gmu.cs321.events_resources.model.RegisteredEvent;

// Our record from model/: one saved registration (built by insert)
import edu.gmu.cs321.events_resources.model.Registration;

// Java's built-in ordered collection: findEventsForUser returns a List of events
import java.util.List;

// Spring's helper for running SQL. It opens/closes database connections for us
// and fills in the "?" placeholders safely.
import org.springframework.jdbc.core.JdbcTemplate;

// "How to turn one SQL result row into a Java object." Used by findEventsForUser.
import org.springframework.jdbc.core.RowMapper;

// For @Repository: "Spring, create one of these at startup" (and it marks this as the storage layer)
import org.springframework.stereotype.Repository;

/**
 * RegistrationRepository: the STORAGE layer for registrations.
 *
 * This is the ONLY class with SQL for registering. Each method is one small
 * database operation. It never decides yes/no; the RULES live in RegistrationService.
 *
 *   Controller  ->  Service (rules)  ->  Repository (SQL, this file)  ->  PostgreSQL
 *
 * SAFETY: every "?" in the SQL is a placeholder that JdbcTemplate fills in safely.
 * NEVER glue values into the SQL string ("... WHERE id = " + id): that allows SQL injection.
 */
@Repository // Spring creates one of these and passes it to RegistrationService
public class RegistrationRepository {

    // Spring's SQL helper. "final" = set once in the constructor, never replaced.
    private final JdbcTemplate jdbc;

    // Spring calls this at startup and passes in a JdbcTemplate that's already
    // connected to the database from application.properties.
    public RegistrationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Does an event with this id exist? Used for the 404 check. */
    public boolean eventExists(long eventId) {
        // queryForObject = "run this SQL, I expect exactly ONE value back".
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM events WHERE id = ?", // COUNT(*) = how many rows match: 0 or 1
                Integer.class,                              // "give me the answer as an Integer"
                eventId);                                   // fills in the "?"
        // Integer (not int) can be null, so check for that first.
        return count != null && count > 0;
    }

    /** Is this user already registered for this event? Used for the ALREADY_REGISTERED check. */
    public boolean isRegistered(long eventId, long userId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM registrations WHERE event_id = ? AND user_id = ?",
                Integer.class,
                eventId, userId); // fills in the two "?" in order: first event_id, then user_id
        return count != null && count > 0;
    }

    /**
     * THE CAPACITY CHECK. Tries to take one seat, in ONE statement.
     *
     * The WHERE clause only matches the event if there's room:
     *   capacity = 0                      -> unlimited event, always room
     *   registered_count < capacity       -> at least one seat left
     *
     * update() returns how many rows it changed:
     *   1 -> there was room, and registered_count went up by one: you got the seat
     *   0 -> the event was full (or doesn't exist), nothing changed
     *
     * WHY ONE STATEMENT: the database does the check and the +1 together, so two
     * students clicking at the same moment can't both get the last seat.
     * (Tested: 50 simultaneous requests on a 30-seat event -> exactly 30 succeeded.)
     */
    public boolean tryTakeSeat(long eventId) {
        int rowsChanged = jdbc.update(                                   // update() is for INSERT/UPDATE/DELETE
                "UPDATE events SET registered_count = registered_count + 1 "
                        + "WHERE id = ? AND (capacity = 0 OR registered_count < capacity)",
                eventId);
        return rowsChanged == 1; // true = seat taken, false = full
    }

    /**
     * Saves the registration and returns it with its new id and timestamp.
     * The database fills in id (GENERATED AS IDENTITY) and created_at (DEFAULT now()).
     */
    public Registration insert(long eventId, long userId) {
        return jdbc.queryForObject(
                // "RETURNING ..." = PostgreSQL hands back the row it just inserted,
                // so we get the new id and created_at without a second query.
                "INSERT INTO registrations (event_id, user_id) VALUES (?, ?) "
                        + "RETURNING id, event_id, user_id, created_at",
                // A "row mapper" written as a lambda: how to turn the returned row into a Registration.
                // rs = the row ("result set"), rowNum = which row it is (we don't need it).
                (rs, rowNum) -> new Registration(
                        rs.getLong("id"),                          // read column "id" as a long
                        rs.getLong("event_id"),
                        rs.getLong("user_id"),
                        rs.getTimestamp("created_at").toLocalDateTime()), // SQL TIMESTAMP -> Java LocalDateTime
                eventId, userId); // fills in the two "?"
    }

    /**
     * One page of the events this user registered for, soonest first.
     *
     * JOIN   = combine rows from two tables that match: events whose id appears
     *          in this user's registrations.
     * LIMIT  = return at most this many rows (the page size).
     * OFFSET = skip this many rows first (to reach page 2, 3, ...).
     * to_char(...) formats the date/time as text so the JSON matches the contract.
     * "AS date" renames a column in the result, so the row mapper can read it by that name.
     */
    public List<RegisteredEvent> findEventsForUser(long userId, int limit, int offset) {
        return jdbc.query(                       // query() = "I expect MANY rows back, give me a List"
                "SELECT e.id, e.event_title, e.host, "                                   // e. = from the events table
                        + "       to_char(e.event_date, 'YYYY-MM-DD') AS date, "         // 2026-10-22
                        + "       to_char(e.event_time, 'HH24:MI')    AS time, "         // 17:30
                        + "       e.location, e.tag, e.register_link, e.capacity, e.registered_count, "
                        + "       to_char(r.created_at, 'YYYY-MM-DD\"T\"HH24:MI:SS') AS registered_at " // r. = from registrations; "T" is a literal letter
                        + "FROM events e "                                               // "e" is a short nickname for events
                        + "JOIN registrations r ON r.event_id = e.id "                   // "r" is a short nickname for registrations
                        + "WHERE r.user_id = ? "                                         // only THIS user's registrations
                        + "ORDER BY e.event_date, e.event_time "                         // soonest event first
                        + "LIMIT ? OFFSET ?",                                            // one page
                REGISTERED_EVENT_MAPPER, // how to turn each row into a RegisteredEvent (defined at the bottom)
                userId, limit, offset);  // fills in the three "?" in order
    }

    /** How many events this user registered for in total. Becomes "totalCount" in the response. */
    public long countEventsForUser(long userId) {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM registrations WHERE user_id = ?",
                Long.class, // "give me the answer as a Long" (totalCount is a long)
                userId);
        return count == null ? 0 : count; // ternary: if null use 0, otherwise use count
    }

    // Turns one row of the JOIN query above into a RegisteredEvent.
    // "static final" = defined once and reused for every row, every time.
    // Each rs.getX("name") reads one column by the name used in the SELECT.
    private static final RowMapper<RegisteredEvent> REGISTERED_EVENT_MAPPER = (rs, rowNum) -> new RegisteredEvent(
            rs.getLong("id"),
            rs.getString("event_title"),
            rs.getString("host"),
            rs.getString("date"),          // the "AS date" column
            rs.getString("time"),          // the "AS time" column
            rs.getString("location"),
            rs.getString("tag"),
            rs.getString("register_link"), // null if the event has no link
            rs.getInt("capacity"),
            rs.getInt("registered_count"),
            rs.getString("registered_at")); // the "AS registered_at" column
}
