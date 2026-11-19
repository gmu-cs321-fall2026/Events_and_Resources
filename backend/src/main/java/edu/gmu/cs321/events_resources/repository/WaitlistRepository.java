package edu.gmu.cs321.events_resources.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import edu.gmu.cs321.events_resources.model.EventSeats;
import edu.gmu.cs321.events_resources.model.WaitlistEntry;

/**
 * WaitlistRepository: the STORAGE layer for the waitlist (MASON-18 / MASON-19).
 *
 *   Controller  ->  WaitlistService (rules)  ->  WaitlistRepository (SQL, this file)  ->  PostgreSQL
 *
 * Like RegistrationRepository, every method is one small database step and never
 * decides yes/no. The rules (who may join, when seats get handed out) live in WaitlistService.
 */
@Repository
public class WaitlistRepository {

    private final JdbcTemplate jdbc;

    public WaitlistRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }
    // Reading and locking the event's seat numbers
    // -----------------------------------------------------------------------

    /** The event's capacity and registered_count, or empty if the event doesn't exist. */
    public Optional<EventSeats> findSeats(long eventId) {
        return jdbc.query(
                "SELECT capacity, registered_count FROM events WHERE id = ?",
                SEATS_MAPPER, eventId).stream().findFirst();
    }

    /**
     * Same as findSeats, but LOCKS the event row until the transaction ends.
     *
     * "FOR UPDATE" makes any other request that wants to change this event's seats
     * (someone registering, cancelling, or another capacity change) wait until we're done.
     * That way "how many seats are open?" stays true while we hand those seats out,
     * and two requests can never give the same open seat to two people.
     * Only call this inside a @Transactional method.
     */
    public Optional<EventSeats> lockSeats(long eventId) {
        return jdbc.query(
                "SELECT capacity, registered_count FROM events WHERE id = ? FOR UPDATE",
                SEATS_MAPPER, eventId).stream().findFirst();
    }

    /**
     * Sets a new capacity. The table's CHECK rule (capacity = 0 OR registered_count <= capacity)
     * still applies, so the database itself refuses a capacity below the people already registered.
     */
    public void setCapacity(long eventId, int capacity) {
        jdbc.update("UPDATE events SET capacity = ? WHERE id = ?", capacity, eventId);
    }

    // -----------------------------------------------------------------------
    // Joining, leaving, and reading the line
    // -----------------------------------------------------------------------

    /** Puts the user at the END of the line. Throws DuplicateKeyException if they're already in it. */
    public void add(long eventId, UUID userId) {
        jdbc.update("INSERT INTO waitlist (event_id, user_id) VALUES (?, ?)", eventId, userId);
    }

    /** Takes the user out of the line. true = they were in it, false = nothing to remove. */
    public boolean remove(long eventId, UUID userId) {
        return jdbc.update("DELETE FROM waitlist WHERE event_id = ? AND user_id = ?", eventId, userId) == 1;
    }

    /**
     * Where this user is in line: 1 = next to get a seat. Empty if they aren't on the waitlist.
     *
     * HOW: count everyone in this event's line whose id is <= this user's id.
     * If the user isn't in line, the inner SELECT finds nothing (NULL), "id <= NULL" is never
     * true, and the count is 0.
     */
    public Optional<Integer> position(long eventId, UUID userId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM waitlist "
                        + "WHERE event_id = ? "
                        + "  AND id <= (SELECT id FROM waitlist WHERE event_id = ? AND user_id = ?)",
                Integer.class, eventId, eventId, userId);
        return (count == null || count == 0) ? Optional.empty() : Optional.of(count);
    }

    /**
     * The whole line for one event, first in line first.
     * ROW_NUMBER() OVER (ORDER BY id) numbers the rows 1, 2, 3... in joining order.
     */
    public List<WaitlistEntry> list(long eventId) {
        return jdbc.query(
                "SELECT user_id, created_at, ROW_NUMBER() OVER (ORDER BY id) AS position "
                        + "FROM waitlist WHERE event_id = ? ORDER BY id",
                ENTRY_MAPPER, eventId);
    }

    /** The user ids of the first {@code howMany} people in line. */
    public List<UUID> firstInLine(long eventId, int howMany) {
        return jdbc.query(
                "SELECT user_id FROM waitlist WHERE event_id = ? ORDER BY id LIMIT ?",
                (rs, rowNum) -> rs.getObject("user_id", UUID.class),
                eventId, howMany);
    }

    // -----------------------------------------------------------------------
    // Moving someone from the waitlist into a seat
    // -----------------------------------------------------------------------

    /**
     * Moves one person off the waitlist and into registrations, in ONE statement
     * (same idea as RegistrationRepository.cancel, but in the other direction):
     *
     *   moved:  DELETE their waitlist row, hand back (event_id, user_id)
     *   added:  INSERT that pair into registrations
     *           (ON CONFLICT DO NOTHING: if they were somehow already registered, skip it)
     *   UPDATE: registered_count + 1, but only if "added" really added a row
     *
     * true = they now have a seat. The caller must have checked there IS an open seat
     * (WaitlistService does, while holding lockSeats).
     */
    public boolean moveToRegistrations(long eventId, UUID userId) {
        int rowsChanged = jdbc.update(
                "WITH moved AS ("
                        + "    DELETE FROM waitlist WHERE event_id = ? AND user_id = ? "
                        + "    RETURNING event_id, user_id"
                        + "), added AS ("
                        + "    INSERT INTO registrations (event_id, user_id) "
                        + "    SELECT event_id, user_id FROM moved "
                        + "    ON CONFLICT (event_id, user_id) DO NOTHING "
                        + "    RETURNING event_id"
                        + ") "
                        + "UPDATE events SET registered_count = registered_count + 1 "
                        + "WHERE id IN (SELECT event_id FROM added)",
                eventId, userId);
        return rowsChanged == 1;
    }

    // -----------------------------------------------------------------------
    // Row mappers: how to turn one result row into a Java record
    // -----------------------------------------------------------------------

    private static final RowMapper<EventSeats> SEATS_MAPPER = (rs, rowNum) -> new EventSeats(
            rs.getInt("capacity"),
            rs.getInt("registered_count"));

    private static final RowMapper<WaitlistEntry> ENTRY_MAPPER = (rs, rowNum) -> new WaitlistEntry(
            rs.getInt("position"),
            rs.getObject("user_id", UUID.class),
            rs.getTimestamp("created_at").toLocalDateTime());
}
