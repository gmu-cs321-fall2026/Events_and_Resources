package edu.gmu.cs321.events_resources;

import org.junit.jupiter.api.Test;
// Static imports = use assertEquals(...) instead of Assertions.assertEquals(...).
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Java's date and time types (the fake repository receives these)
import java.time.LocalDate;
import java.time.LocalTime;

// Named status codes: BAD_REQUEST = 400
import org.springframework.http.HttpStatus;

// The real classes we're testing / using.
import edu.gmu.cs321.events_resources.common.ApiException;
import edu.gmu.cs321.events_resources.model.Event;
import edu.gmu.cs321.events_resources.repository.EventRepository;
import edu.gmu.cs321.events_resources.service.EventService; 

/**
 * Unit tests for EventService: the POST /events rules from Interface Contract v3.
 *
 * "Unit test" = we test this ONE class by itself: no Spring app, no database.
 * Instead of the real EventRepository we pass in a FAKE one (below) that just
 * remembers what it was asked to save. So these tests are fast and need no PostgreSQL.
 */
class EventServiceTest {

    /**
     * A fake repository. "extends EventRepository" = it IS an EventRepository,
     * so EventService accepts it, but we replace insert() so it never touches a database.
     * null = no JdbcTemplate needed, because our insert() never uses it.
     */
    static class FakeEventRepository extends EventRepository {
        Event saved;          // what the service asked us to save (null = nothing saved)

        FakeEventRepository() {
            super(null);
        }

        @Override // "replace the parent's version of this method with mine"
        public Event insert(Event event, LocalDate date, LocalTime time) {
            saved = event;
            // Pretend the database gave it id 42, like INSERT ... RETURNING would.
            return new Event(42L, event.eventTitle(), event.host(), event.date(), event.time(),
                    event.location(), event.category(), event.audience(), event.format(),
                    event.registerLink(), event.capacity(), event.registeredCount());
        }
    }

    private final FakeEventRepository repository = new FakeEventRepository();
    private final EventService service = new EventService(repository);

    /** Helper: the exact event the Create Event page sends (see CreateEventPage.jsx). */
    private Event validEvent() {
        return new Event(null, "Resume Workshop", "Career Services", "2026-10-20", "15:00",
                "Horizon Hall 2400", "Workshop", "All Students", "In-person", null, 30, 0);
    }

    @Test
    void validEventIsSavedAndReturnedWithId() {
        Event result = service.createEvent(validEvent());

        assertEquals(Long.valueOf(42), result.id());    // the id the "database" picked (id is a Long, so compare to a Long)
        assertEquals("Workshop", result.category());
        assertEquals(0, result.registeredCount());      // nobody registered yet
    }

    @Test
    void spacesAreTrimmedAndBlankLinkBecomesNull() {
        Event messy = new Event(null, "  Resume Workshop  ", "Career Services", "2026-10-20", "15:00",
                "Horizon Hall", "Workshop", "All Students", "Virtual", "   ", 0, 0);

        service.createEvent(messy);

        assertEquals("Resume Workshop", repository.saved.eventTitle());
        assertNull(repository.saved.registerLink());
    }

    @Test
    void callerCannotFakeIdOrRegisteredCount() {
        Event sneaky = new Event(999L, "Panel Night", "Alumni", "2026-11-01", "18:00",
                "JC", "Panel", "All Students", "In-person", null, 10, 10);

        service.createEvent(sneaky);

        assertNull(repository.saved.id());              // the database picks the id, not the caller
        assertEquals(0, repository.saved.registeredCount());
    }

    @Test
    void missingFieldsAreAllReportedAtOnce() {
        Event empty = new Event(null, "", null, null, null, " ", null, "", null, null, 0, 0);

        // assertThrows = "this code MUST throw an ApiException"; it hands us the exception to inspect.
        ApiException e = assertThrows(ApiException.class, () -> service.createEvent(empty));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        assertEquals("VALIDATION_ERROR", e.getCode());
        // Every bad field is named in detail.
        for (String field : new String[] {"eventTitle", "host", "date", "time",
                                          "location", "category", "audience", "format"}) {
            assertTrue(e.getDetail().contains(field + ": required"), "detail should mention " + field);
        }
        assertNull(repository.saved);                   // nothing was saved
    }

    @Test
    void categoryAndFormatMustMatchTheDropdowns() {
        Event wrong = new Event(null, "Resume Workshop", "Career Services", "2026-10-20", "15:00",
                "Horizon Hall", "Party", "All Students", "in-person", null, 0, 0); // lowercase "in-person" isn't allowed

        ApiException e = assertThrows(ApiException.class, () -> service.createEvent(wrong));

        assertTrue(e.getDetail().contains("category: must be one of"));
        assertTrue(e.getDetail().contains("format: must be one of"));
    }

    @Test
    void impossibleDateAndTimeAreRejected() {
        Event wrong = new Event(null, "Resume Workshop", "Career Services", "2026-02-30", "25:00",
                "Horizon Hall", "Workshop", "All Students", "Virtual", null, 0, 0);

        ApiException e = assertThrows(ApiException.class, () -> service.createEvent(wrong));

        assertTrue(e.getDetail().contains("date:"));
        assertTrue(e.getDetail().contains("time:"));
    }

    @Test
    void negativeCapacityIsRejected() {
        Event wrong = new Event(null, "Resume Workshop", "Career Services", "2026-10-20", "15:00",
                "Horizon Hall", "Workshop", "All Students", "Virtual", null, -5, 0);

        ApiException e = assertThrows(ApiException.class, () -> service.createEvent(wrong));

        assertTrue(e.getDetail().contains("capacity:"));
    }
}