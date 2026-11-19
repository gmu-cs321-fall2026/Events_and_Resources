package edu.gmu.cs321.events_resources.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import edu.gmu.cs321.events_resources.common.ApiException;
import edu.gmu.cs321.events_resources.common.PagedResponse;
import edu.gmu.cs321.events_resources.integration.FakeTokenValidator;
import edu.gmu.cs321.events_resources.model.DueReminder;
import edu.gmu.cs321.events_resources.repository.RegistrationRepository;

/**
 * Unit tests for RegistrationService.upcomingReminders(...), the list behind
 * GET /registrations/reminders that the Notifications subsystem reads.
 *
 * A FAKE repository records the window and page it was asked for, and a fixed
 * Clock makes "now" always Nov 20, 2026 at 9:00:30 in GMU time. No database.
 *
 * Rules being checked:
 *   - no from/to = the next 24 hours, starting this minute
 *   - from/to are passed through as given; to without from = from now
 *   - bad from/to, to before from, or a bad page -> 400 VALIDATION_ERROR
 *   - the answer is one page plus the total count
 */
class RegistrationServiceUpcomingRemindersTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 20, 9, 0, 30);
    private static final Clock CLOCK =
            Clock.fixed(NOW.atZone(RegistrationService.GMU_TIME).toInstant(), RegistrationService.GMU_TIME);

    private static final DueReminder RESUME_WORKSHOP = new DueReminder(
            41L, FakeTokenValidator.STUDENT_ID, 3L, "Resume Workshop",
            "2026-11-21", "08:30", "Horizon Hall", null);

    /** Fake repository: remembers what it was asked, and returns one reminder out of 7. */
    static class FakeRegistrationRepository extends RegistrationRepository {
        LocalDateTime askedFrom;
        LocalDateTime askedUntil;
        int askedLimit;
        int askedOffset;

        FakeRegistrationRepository() {
            super(null);
        }

        @Override
        public List<DueReminder> findUpcomingReminders(LocalDateTime from, LocalDateTime until, int limit, int offset) {
            askedFrom = from;
            askedUntil = until;
            askedLimit = limit;
            askedOffset = offset;
            return List.of(RESUME_WORKSHOP);
        }

        @Override
        public long countUpcomingReminders(LocalDateTime from, LocalDateTime until) {
            return 7;
        }

        @Override
        public boolean setReminder(long eventId, UUID userId, boolean remind) {
            return true;
        }
    }

    private final FakeRegistrationRepository repo = new FakeRegistrationRepository();
    private final RegistrationService service = new RegistrationService(repo, CLOCK);

    @Test
    void noWindowMeansTheNext24HoursFromThisMinute() {
        service.upcomingReminders(null, null, 1, 20);

        assertEquals(LocalDateTime.of(2026, 11, 20, 9, 0), repo.askedFrom);   // seconds dropped
        assertEquals(LocalDateTime.of(2026, 11, 21, 9, 0), repo.askedUntil);
    }

    @Test
    void aGivenWindowIsUsedAsIs() {
        service.upcomingReminders("2026-11-25T00:00", "2026-11-26T12:30", 1, 20);

        assertEquals(LocalDateTime.of(2026, 11, 25, 0, 0), repo.askedFrom);
        assertEquals(LocalDateTime.of(2026, 11, 26, 12, 30), repo.askedUntil);
    }

    @Test
    void fromAloneMeansOneDayFromThere() {
        service.upcomingReminders("2026-11-25T08:00", null, 1, 20);

        assertEquals(LocalDateTime.of(2026, 11, 26, 8, 0), repo.askedUntil);
    }

    @Test
    void returnsOnePageAndTheTotal() {
        PagedResponse<DueReminder> answer = service.upcomingReminders(null, null, 2, 5);

        assertEquals(List.of(RESUME_WORKSHOP), answer.items());
        assertEquals(7, answer.totalCount());
        assertEquals(5, repo.askedLimit);
        assertEquals(5, repo.askedOffset); // page 2 of 5 skips the first 5
    }

    @Test
    void aBadDateTimeIs400() {
        ApiException error = assertThrows(ApiException.class,
                () -> service.upcomingReminders("tomorrow", "2026-11-21", 1, 20));

        assertEquals("VALIDATION_ERROR", error.getCode());
        assertTrue(error.getDetail().contains("from:"));
        assertTrue(error.getDetail().contains("to:")); // both problems reported at once
    }

    @Test
    void toBeforeFromIs400() {
        ApiException error = assertThrows(ApiException.class,
                () -> service.upcomingReminders("2026-11-25T09:00", "2026-11-24T09:00", 1, 20));

        assertEquals("VALIDATION_ERROR", error.getCode());
    }

    @Test
    void aBadPageIs400() {
        ApiException error = assertThrows(ApiException.class,
                () -> service.upcomingReminders(null, null, 0, 20));

        assertEquals("VALIDATION_ERROR", error.getCode());
    }
}