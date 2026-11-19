package edu.gmu.cs321.events_resources.service;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import edu.gmu.cs321.events_resources.common.ApiException;
import edu.gmu.cs321.events_resources.integration.FakeTokenValidator;
import edu.gmu.cs321.events_resources.model.CurrentUser;
import edu.gmu.cs321.events_resources.model.Registration;
import edu.gmu.cs321.events_resources.model.RegistrationStatus;
import edu.gmu.cs321.events_resources.repository.RegistrationRepository;

/**
 * Unit tests for RegistrationService.setReminder(...).
 *
 * Same idea as RegistrationServiceCancelTest: a FAKE repository with ONE event (id 3),
 * where STUDENT is registered and OTHER_STUDENT is not. No database.
 *
 * Rules being checked:
 *   the event must exist (404 NOT_FOUND), the user must be registered (404 NOT_REGISTERED),
 *   otherwise the choice is saved and the returned status shows it
 */

class RegistrationServiceReminderTest {
    private static final CurrentUser STUDENT =
            new CurrentUser(FakeTokenValidator.STUDENT_ID, List.of(CurrentUser.STUDENT));
    private static final CurrentUser OTHER_STUDENT =
            new CurrentUser(FakeTokenValidator.STUDENT2_ID, List.of(CurrentUser.STUDENT));

    static class FakeRegistrationRepository extends RegistrationRepository {
        boolean studentRemind = false;
        int setReminderCalls = 0;

        FakeRegistrationRepository() {
            super(null);
        }

        @Override
        public boolean eventExists(long eventId) {
            return eventId == 3;
        }

        @Override
        public boolean setReminder(long eventId, UUID userId, boolean remind) {
            setReminderCalls++;
            if (eventId == 3 && userId.equals(STUDENT.id())) {
                studentRemind = remind;
                return true;
            }
            return false;
        }

        @Override
        public Optional<Registration> find(long eventId, UUID userId) {
            if (eventId == 3 && userId.equals(STUDENT.id())) {
                return Optional.of(new Registration(1L, 3L, userId,
                        LocalDateTime.of(2026, 10, 30, 17, 5), studentRemind));
            }
            return Optional.empty();
        }
    }

    private final FakeRegistrationRepository repo = new FakeRegistrationRepository();
    private final RegistrationService service = new RegistrationService(repo);

    @Test
    void turningTheReminderOnSavesItAndReturnsIt() {
        RegistrationStatus status = service.setReminder(3, STUDENT, true);

        assertTrue(repo.studentRemind);
        assertTrue(status.registered());
        assertTrue(status.remind());
    }

    @Test
    void turningTheReminderOffWorksToo() {
        service.setReminder(3, STUDENT, true);

        RegistrationStatus status = service.setReminder(3, STUDENT, false);

        assertFalse(repo.studentRemind);
        assertFalse(status.remind());
    }

    @Test
    void statusShowsTheReminder() {
        assertFalse(service.status(3, STUDENT).remind());

        repo.studentRemind = true;

        assertTrue(service.status(3, STUDENT).remind());
    }

    @Test
    void aReminderWithoutARegistrationIs404NotRegistered() {
        ApiException error = assertThrows(ApiException.class, () -> service.setReminder(3, OTHER_STUDENT, true));

        assertEquals(404, error.getStatus().value());
        assertEquals("NOT_REGISTERED", error.getCode());
        assertFalse(repo.studentRemind); // STUDENT's reminder was not touched
    }

    @Test
    void aReminderForAnEventThatDoesNotExistIs404NotFound() {
        ApiException error = assertThrows(ApiException.class, () -> service.setReminder(99, STUDENT, true));

        assertEquals("NOT_FOUND", error.getCode());
        assertEquals(0, repo.setReminderCalls); // stopped before changing anything
    }
}
