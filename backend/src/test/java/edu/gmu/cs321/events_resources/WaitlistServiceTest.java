package edu.gmu.cs321.events_resources;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import edu.gmu.cs321.events_resources.common.ApiException;
import edu.gmu.cs321.events_resources.integration.FakeTokenValidator;
import edu.gmu.cs321.events_resources.model.CapacityRequest;
import edu.gmu.cs321.events_resources.model.CurrentUser;
import edu.gmu.cs321.events_resources.model.EventSeats;
import edu.gmu.cs321.events_resources.model.RegistrationStatus;
import edu.gmu.cs321.events_resources.model.WaitlistEntry;
import edu.gmu.cs321.events_resources.model.WaitlistView;
import edu.gmu.cs321.events_resources.repository.RegistrationRepository;
import edu.gmu.cs321.events_resources.repository.WaitlistRepository;
import edu.gmu.cs321.events_resources.service.WaitlistService;

/**
 * Unit tests for WaitlistService.
 *
 * Like RegistrationServiceCancelTest, FAKE repositories stand in for the real ones, so
 * these tests check the RULES only, with no database. The real SQL was checked by hand
 * against PostgreSQL (see docs/ai-log/11-17-waitlist.md).
 *
 * The fake world has ONE event (id 3). Each test sets its capacity and fills its seats.
 *
 * The main story being checked (the one from the ticket):
 *   capacity 30, 30 registered, a 31st student joins the waitlist,
 *   staff raise the capacity to 35 -> that student is registered automatically.
 */
class WaitlistServiceTest {

    private static final CurrentUser STUDENT =
            new CurrentUser(FakeTokenValidator.STUDENT_ID, List.of(CurrentUser.STUDENT));
    private static final CurrentUser STUDENT2 =
            new CurrentUser(FakeTokenValidator.STUDENT2_ID, List.of(CurrentUser.STUDENT, CurrentUser.MENTOR));
    private static final CurrentUser STAFF =
            new CurrentUser(UUID.fromString("00000000-0000-0000-0000-000000000002"), List.of(CurrentUser.STAFF));

    /** The fake event 3: its capacity, who has a seat, and who is waiting (first in line first). */
    static class FakeEvent {
        int capacity = 30;
        final List<UUID> registered = new ArrayList<>();
        final List<UUID> line = new ArrayList<>();

        /** Fills the event with {@code howMany} made-up students. */
        void fillWithStrangers(int howMany) {
            for (int i = 0; i < howMany; i++) {
                registered.add(UUID.randomUUID());
            }
        }
    }

    /** Fake waitlist storage: reads and changes the FakeEvent instead of a database. */
    static class FakeWaitlistRepository extends WaitlistRepository {
        final FakeEvent event;

        FakeWaitlistRepository(FakeEvent event) {
            super(null);
            this.event = event;
        }

        @Override
        public Optional<EventSeats> findSeats(long eventId) {
            return eventId == 3
                    ? Optional.of(new EventSeats(event.capacity, event.registered.size()))
                    : Optional.empty();
        }

        @Override
        public Optional<EventSeats> lockSeats(long eventId) {
            return findSeats(eventId);
        }

        @Override
        public void setCapacity(long eventId, int capacity) {
            event.capacity = capacity;
        }

        @Override
        public void add(long eventId, UUID userId) {
            event.line.add(userId);
        }

        @Override
        public boolean remove(long eventId, UUID userId) {
            return event.line.remove(userId);
        }

        @Override
        public Optional<Integer> position(long eventId, UUID userId) {
            int index = event.line.indexOf(userId);
            return index < 0 ? Optional.empty() : Optional.of(index + 1);
        }

        @Override
        public List<WaitlistEntry> list(long eventId) {
            List<WaitlistEntry> entries = new ArrayList<>();
            for (int i = 0; i < event.line.size(); i++) {
                entries.add(new WaitlistEntry(i + 1, event.line.get(i), LocalDateTime.now()));
            }
            return entries;
        }

        @Override
        public List<UUID> firstInLine(long eventId, int howMany) {
            return List.copyOf(event.line.subList(0, Math.min(howMany, event.line.size())));
        }

        @Override
        public boolean moveToRegistrations(long eventId, UUID userId) {
            if (!event.line.remove(userId)) {
                return false;
            }
            event.registered.add(userId);
            return true;
        }
    }

    /** Fake registration storage: only the two methods WaitlistService uses. */
    static class FakeRegistrationRepository extends RegistrationRepository {
        final FakeEvent event;

        FakeRegistrationRepository(FakeEvent event) {
            super(null);
            this.event = event;
        }

        @Override
        public boolean eventExists(long eventId) {
            return eventId == 3;
        }

        @Override
        public boolean isRegistered(long eventId, UUID userId) {
            return event.registered.contains(userId);
        }
    }

    private final FakeEvent event = new FakeEvent();
    private final WaitlistService service = new WaitlistService(
            new FakeWaitlistRepository(event), new FakeRegistrationRepository(event));

    // =======================================================================
    // The ticket's example: 30 seats, a 31st student, capacity raised to 35
    // =======================================================================

    @Test
    void the31stStudentIsWaitlistedThenAutoAcceptedWhenCapacityGoesTo35() {
        event.fillWithStrangers(30);

        RegistrationStatus joined = service.join(3, STUDENT);
        assertTrue(joined.waitlisted());
        assertEquals(1, joined.waitlistPosition());

        WaitlistView after = service.changeCapacity(3, new CapacityRequest(35), STAFF);

        assertEquals(35, after.capacity());
        assertEquals(31, after.registeredCount());
        assertEquals(List.of(STUDENT.id()), after.justAccepted());
        assertTrue(after.waitlist().isEmpty());
        assertTrue(event.registered.contains(STUDENT.id()));
    }

    @Test
    void raisingCapacityByOneOnlyAcceptsWhoeverWasFirstInLine() {
        event.fillWithStrangers(30);
        service.join(3, STUDENT);
        service.join(3, STUDENT2);

        WaitlistView after = service.changeCapacity(3, new CapacityRequest(31), STAFF);

        assertEquals(List.of(STUDENT.id()), after.justAccepted());
        assertEquals(1, after.waitlist().size());
        assertEquals(STUDENT2.id(), after.waitlist().get(0).userId());
        assertEquals(1, after.waitlist().get(0).position()); // STUDENT2 moved up to #1
    }

    @Test
    void capacityZeroMeansUnlimitedAndAcceptsEveryone() {
        event.fillWithStrangers(30);
        service.join(3, STUDENT);
        service.join(3, STUDENT2);

        WaitlistView after = service.changeCapacity(3, new CapacityRequest(0), STAFF);

        assertEquals(List.of(STUDENT.id(), STUDENT2.id()), after.justAccepted());
        assertEquals(32, after.registeredCount());
    }

    @Test
    void aCancelledSeatGoesToTheFirstPersonInLine() {
        event.fillWithStrangers(30);
        service.join(3, STUDENT);
        event.registered.remove(0); // someone cancelled: 29 / 30

        List<UUID> accepted = service.fillOpenSeats(3);

        assertEquals(List.of(STUDENT.id()), accepted);
        assertEquals(30, event.registered.size());
    }

    @Test
    void fillingWithNobodyWaitingChangesNothing() {
        event.fillWithStrangers(25);

        assertTrue(service.fillOpenSeats(3).isEmpty());
        assertEquals(25, event.registered.size());
    }

    // =======================================================================
    // MASON-18: joining and leaving
    // =======================================================================

    @Test
    void youCanOnlyJoinTheWaitlistWhenTheEventIsFull() {
        event.fillWithStrangers(29);

        ApiException error = assertThrows(ApiException.class, () -> service.join(3, STUDENT));

        assertEquals(409, error.getStatus().value());
        assertEquals("SPOTS_AVAILABLE", error.getCode());
    }

    @Test
    void joiningTwiceIs409AlreadyWaitlisted() {
        event.fillWithStrangers(30);
        service.join(3, STUDENT);

        ApiException error = assertThrows(ApiException.class, () -> service.join(3, STUDENT));

        assertEquals("ALREADY_WAITLISTED", error.getCode());
    }

    @Test
    void someoneAlreadyRegisteredCannotJoinTheWaitlist() {
        event.fillWithStrangers(29);
        event.registered.add(STUDENT.id());

        ApiException error = assertThrows(ApiException.class, () -> service.join(3, STUDENT));

        assertEquals("ALREADY_REGISTERED", error.getCode());
    }

    @Test
    void joiningAMissingEventIs404() {
        ApiException error = assertThrows(ApiException.class, () -> service.join(99, STUDENT));

        assertEquals("NOT_FOUND", error.getCode());
    }

    @Test
    void leavingTakesYouOutOfLine() {
        event.fillWithStrangers(30);
        service.join(3, STUDENT);

        service.leave(3, STUDENT);

        assertTrue(event.line.isEmpty());
    }

    @Test
    void leavingWhenYouWereNotInLineIs404NotWaitlisted() {
        ApiException error = assertThrows(ApiException.class, () -> service.leave(3, STUDENT));

        assertEquals(404, error.getStatus().value());
        assertEquals("NOT_WAITLISTED", error.getCode());
    }

    // =======================================================================
    // MASON-19: staff rules
    // =======================================================================

    @Test
    void staffSeeTheLineInOrder() {
        event.fillWithStrangers(30);
        service.join(3, STUDENT);
        service.join(3, STUDENT2);

        WaitlistView view = service.view(3, STAFF);

        assertEquals(30, view.capacity());
        assertEquals(30, view.registeredCount());
        assertEquals(STUDENT.id(), view.waitlist().get(0).userId());
        assertEquals(STUDENT2.id(), view.waitlist().get(1).userId());
    }

    @Test
    void studentsCannotSeeTheLineOrChangeCapacity() {
        ApiException seeing = assertThrows(ApiException.class, () -> service.view(3, STUDENT));
        ApiException changing = assertThrows(ApiException.class,
                () -> service.changeCapacity(3, new CapacityRequest(35), STUDENT));

        assertEquals("FORBIDDEN", seeing.getCode());
        assertEquals("FORBIDDEN", changing.getCode());
        assertEquals(30, event.capacity); // unchanged
    }

    @Test
    void capacityCannotGoBelowThePeopleAlreadyRegistered() {
        event.fillWithStrangers(30);

        ApiException error = assertThrows(ApiException.class,
                () -> service.changeCapacity(3, new CapacityRequest(29), STAFF));

        assertEquals(400, error.getStatus().value());
        assertEquals("capacity", error.getDetail());
        assertEquals(30, event.capacity);
    }

    @Test
    void missingOrNegativeCapacityIs400() {
        ApiException missing = assertThrows(ApiException.class,
                () -> service.changeCapacity(3, new CapacityRequest(null), STAFF));
        ApiException negative = assertThrows(ApiException.class,
                () -> service.changeCapacity(3, new CapacityRequest(-5), STAFF));

        assertEquals("VALIDATION_ERROR", missing.getCode());
        assertEquals("VALIDATION_ERROR", negative.getCode());
    }

    @Test
    void changingCapacityOfAMissingEventIs404() {
        ApiException error = assertThrows(ApiException.class,
                () -> service.changeCapacity(99, new CapacityRequest(35), STAFF));

        assertEquals("NOT_FOUND", error.getCode());
    }
}
