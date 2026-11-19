package edu.gmu.cs321.events_resources.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// The real classes we're testing / using.
import edu.gmu.cs321.events_resources.common.ApiException;
import edu.gmu.cs321.events_resources.integration.FakeTokenValidator;
import edu.gmu.cs321.events_resources.model.CurrentUser;
import edu.gmu.cs321.events_resources.model.Registration;
import edu.gmu.cs321.events_resources.model.RegistrationStatus;
import edu.gmu.cs321.events_resources.repository.RegistrationRepository;

/**
 * Unit tests for RegistrationService.cancel(...) and status(...).
 *
 * A FAKE repository stands in for the real one, The real SQL is checked by hand
 * against PostgreSQL
 *
 * Rules being checked:
 *   cancel: event must exist (404 NOT_FOUND), user must be registered (404 NOT_REGISTERED),
 *           otherwise the registration is removed
 *   status: event must exist (404 NOT_FOUND), then registered = true/false
 */
class RegistrationServiceCancelTest {

    private static final CurrentUser STUDENT =
            new CurrentUser(FakeTokenValidator.STUDENT_ID, List.of(CurrentUser.STUDENT));
    private static final CurrentUser OTHER_STUDENT =
            new CurrentUser(FakeTokenValidator.STUDENT2_ID, List.of(CurrentUser.STUDENT, CurrentUser.MENTOR));

    /**
     * A fake repository with ONE event (id 3) and ONE registration: STUDENT is
     * registered for event 3. It never touches a database.
     */
    static class FakeRegistrationRepository extends RegistrationRepository {
        boolean studentRegistered = true; // flips to false once the cancel goes through
        int cancelCalls = 0;              // how many times the service asked us to cancel

        FakeRegistrationRepository() {
            super(null);
        }

        @Override
        public boolean eventExists(long eventId) {
            return eventId == 3;
        }

        @Override
        public boolean cancel(long eventId, UUID userId) {
            cancelCalls++;
            if (eventId == 3 && userId.equals(STUDENT.id()) && studentRegistered) {
                studentRegistered = false;
                return true;
            }
            return false;
        }

        @Override
        public Optional<Registration> find(long eventId, UUID userId) {
            if (eventId == 3 && userId.equals(STUDENT.id()) && studentRegistered) {
                return Optional.of(new Registration(1L, 3L, userId,
                        LocalDateTime.of(2026, 10, 30, 17, 5)));
            }
            return Optional.empty();
        }
    }

    /**
     * A fake WaitlistService (MASON-18): nobody is waiting, except OTHER_STUDENT is
     * #2 in line for event 3. It counts how often cancel asks it to fill the freed seat.
     */
    static class FakeWaitlistService extends WaitlistService {
        int fillCalls = 0;

        FakeWaitlistService() {
            super(null, null);
        }

        @Override
        public List<UUID> fillOpenSeats(long eventId) {
            fillCalls++;
            return List.of();
        }

        @Override
        public Optional<Integer> position(long eventId, UUID userId) {
            return userId.equals(OTHER_STUDENT.id()) ? Optional.of(2) : Optional.empty();
        }
    }

    private final FakeRegistrationRepository repo = new FakeRegistrationRepository();
    private final FakeWaitlistService waitlist = new FakeWaitlistService();
    private final RegistrationService service = new RegistrationService(repo, waitlist);

    @Test
    void cancellingYourOwnRegistrationWorks() {
        service.cancel(3, STUDENT); // no exception = success (the controller turns this into 204)

        assertFalse(repo.studentRegistered); // the registration is gone
        assertEquals(1, repo.cancelCalls);
    }

    @Test
    void cancellingTwiceGivesNotRegisteredTheSecondTime() {
        service.cancel(3, STUDENT);

        // A double-click on "Cancel": the second request finds nothing to delete.
        ApiException error = assertThrows(ApiException.class, () -> service.cancel(3, STUDENT));

        assertEquals(404, error.getStatus().value());
        assertEquals("NOT_REGISTERED", error.getCode());
    }

    @Test
    void cancellingWhenYouWereNeverRegisteredIs404NotRegistered() {
        // OTHER_STUDENT has no registration for event 3. This also shows one student
        // can't cancel another student's spot: the id comes from the token, not the URL.
        ApiException error = assertThrows(ApiException.class, () -> service.cancel(3, OTHER_STUDENT));

        assertEquals("NOT_REGISTERED", error.getCode());
        assertTrue(repo.studentRegistered); // STUDENT's registration was not touched
    }

    @Test
    void cancellingForAnEventThatDoesNotExistIs404NotFound() {
        ApiException error = assertThrows(ApiException.class, () -> service.cancel(99, STUDENT));

        assertEquals(404, error.getStatus().value());
        assertEquals("NOT_FOUND", error.getCode());
        assertEquals(0, repo.cancelCalls); // stopped before trying to delete anything
    }

    @Test
    void statusSaysRegisteredWithTheTime() {
        RegistrationStatus status = service.status(3, STUDENT);

        assertEquals(3, status.eventId());
        assertTrue(status.registered());
        assertNotNull(status.registeredAt());
    }

    @Test
    void statusSaysNotRegisteredAfterCancelling() {
        service.cancel(3, STUDENT);

        RegistrationStatus status = service.status(3, STUDENT);

        assertFalse(status.registered());
        assertNull(status.registeredAt()); // no time when there's no registration
    }

    @Test
    void statusForAnEventThatDoesNotExistIs404() {
        ApiException error = assertThrows(ApiException.class, () -> service.status(99, STUDENT));

        assertEquals("NOT_FOUND", error.getCode());
    }

    /*
     * MASON-18: cancelling hands the seat to the waitlist, and status reports your place in line.
     */

    @Test
    void cancellingGivesTheFreedSeatToTheWaitlist() {
        service.cancel(3, STUDENT);

        assertEquals(1, waitlist.fillCalls);
    }

    @Test
    void aFailedCancelDoesNotTouchTheWaitlist() {
        assertThrows(ApiException.class, () -> service.cancel(3, OTHER_STUDENT));

        assertEquals(0, waitlist.fillCalls);
    }

    @Test
    void statusShowsYourPlaceOnTheWaitlist() {
        RegistrationStatus status = service.status(3, OTHER_STUDENT);

        assertFalse(status.registered());
        assertTrue(status.waitlisted());
        assertEquals(2, status.waitlistPosition());
    }

    @Test
    void statusSaysNotWaitlistedWhenYouAreRegistered() {
        RegistrationStatus status = service.status(3, STUDENT);

        assertFalse(status.waitlisted());
        assertNull(status.waitlistPosition());
    }
}
