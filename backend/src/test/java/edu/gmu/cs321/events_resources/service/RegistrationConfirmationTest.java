package edu.gmu.cs321.events_resources.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import edu.gmu.cs321.events_resources.common.ApiException;
import edu.gmu.cs321.events_resources.integration.FakeTokenValidator;
import edu.gmu.cs321.events_resources.integration.NotificationsClient;
import edu.gmu.cs321.events_resources.model.CurrentUser;
import edu.gmu.cs321.events_resources.model.Event;
import edu.gmu.cs321.events_resources.model.Registration;
import edu.gmu.cs321.events_resources.repository.EventRepository;
import edu.gmu.cs321.events_resources.repository.RegistrationRepository;

/**
 * Unit tests for the registration confirmation (RegistrationConfirmation, and the call
 * to it from RegistrationService.register).
 *
 * FAKES stand in for the database and for the Notifications subsystem, so no network
 * or PostgreSQL is needed. FakeNotifications records every message we ask it to send.
 *
 * Rules being checked:
 *   - a successful registration sends the "registration" template with title, date, time, link
 *   - the message waits until the transaction commits (nothing is sent before that)
 *   - a failed registration (event full) sends nothing
 *   - Notifications being down, or any other error, never throws
 */
class RegistrationConfirmationTest {

    private static final CurrentUser STUDENT =
            new CurrentUser(FakeTokenValidator.STUDENT_ID, List.of(CurrentUser.STUDENT));

    /** One event, id 3: Resume Workshop on Saturday Nov 21, 2026 at 08:30, no link. */
    static class FakeEventRepository extends EventRepository {
        boolean broken = false; // true = act like the database is down

        FakeEventRepository() {
            super(null);
        }

        @Override
        public Optional<Event> findById(long id) {
            if (broken) {
                throw new IllegalStateException("database is down");
            }
            if (id != 3) {
                return Optional.empty();
            }
            return Optional.of(new Event(3L, "Resume Workshop", "Career Services", "2026-11-21", "08:30",
                    "Horizon Hall", "Workshop", "All students", "In-person", null, 30, 5));
        }
    }

    /** One message we asked Notifications to send. */
    record Sent(String templateId, UUID userId, Map<String, String> params) {
    }

    /** Records each send() instead of calling the real Notifications subsystem. */
    static class FakeNotifications extends NotificationsClient {
        final List<Sent> sent = new ArrayList<>();
        boolean up = true; // false = act like Notifications is down

        FakeNotifications() {
            super("http://unused");
        }

        @Override
        public boolean send(String templateId, UUID userId, Map<String, String> params) {
            sent.add(new Sent(templateId, userId, params));
            return up;
        }
    }

    /** Event 3 exists. full = true makes tryTakeSeat say "no seats left". */
    static class FakeRegistrationRepository extends RegistrationRepository {
        boolean full = false;

        FakeRegistrationRepository() {
            super(null);
        }

        @Override
        public boolean eventExists(long eventId) {
            return eventId == 3;
        }

        @Override
        public boolean isRegistered(long eventId, UUID userId) {
            return false;
        }

        @Override
        public boolean tryTakeSeat(long eventId) {
            return !full;
        }

        @Override
        public Registration insert(long eventId, UUID userId) {
            return new Registration(12L, eventId, userId, LocalDateTime.of(2026, 11, 19, 11, 30));
        }
    }

    private final FakeEventRepository events = new FakeEventRepository();
    private final FakeNotifications notifications = new FakeNotifications();
    private final RegistrationConfirmation confirmation = new RegistrationConfirmation(events, notifications);

    private final FakeRegistrationRepository registrations = new FakeRegistrationRepository();
    private final RegistrationService service = new RegistrationService(registrations, null, confirmation,
            Clock.system(RegistrationService.GMU_TIME));

    /** Some tests pretend to be inside a transaction; always clean that up. */
    @AfterEach
    void endPretendTransaction() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    // =======================================================================
    // What gets sent
    // =======================================================================

    @Test
    void registeringSendsTheRegistrationTemplate() {
        service.register(3, STUDENT);

        assertEquals(1, notifications.sent.size());
        Sent message = notifications.sent.get(0);
        assertEquals(NotificationsClient.REGISTRATION, message.templateId());
        assertEquals(STUDENT.id(), message.userId());
        assertEquals(Map.of(
                "title", "Resume Workshop",
                "date", "Saturday, Nov 21, 2026",
                "time", "8:30 AM",
                "link", ""), message.params()); // no link -> "" (Notifications rejects a missing value)
    }

    @Test
    void aFullEventSendsNothing() {
        registrations.full = true;

        assertThrows(ApiException.class, () -> service.register(3, STUDENT));
        assertTrue(notifications.sent.isEmpty());
    }

    // =======================================================================
    // Waiting for the commit
    // =======================================================================

    @Test
    void insideATransactionItWaitsForTheCommit() {
        // Pretend we're inside @Transactional, like the real register() is.
        TransactionSynchronizationManager.initSynchronization();

        service.register(3, STUDENT);
        assertTrue(notifications.sent.isEmpty()); // not yet: nothing is saved for real until the commit

        // Pretend the transaction committed: Spring runs every afterCommit() it was given.
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        assertEquals(1, notifications.sent.size());
    }

    // =======================================================================
    // Problems never break registering
    // =======================================================================

    @Test
    void notificationsDownStillLetsTheStudentRegister() {
        notifications.up = false;

        Registration saved = service.register(3, STUDENT); // no exception
        assertEquals(12L, saved.id());
        assertEquals(1, notifications.sent.size());        // we did try
    }

    @Test
    void aDatabaseErrorWhileSendingDoesNotThrow() {
        events.broken = true;

        assertFalse(confirmation.send(3, STUDENT.id()));
        assertTrue(notifications.sent.isEmpty());
    }

    @Test
    void anUnknownEventSendsNothing() {
        assertFalse(confirmation.send(99, STUDENT.id()));
        assertTrue(notifications.sent.isEmpty());
    }

    // =======================================================================
    // Formatting
    // =======================================================================

    @Test
    void unreadableDatesAndTimesAreSentAsTheyAre() {
        assertEquals("TBD", RegistrationConfirmation.readableDate("TBD"));
        assertEquals("noon", RegistrationConfirmation.readableTime("noon"));
        assertEquals("", RegistrationConfirmation.readableDate(null));
        assertEquals("", RegistrationConfirmation.readableTime(null));
    }

    @Test
    void afternoonTimesUsePm() {
        assertEquals("2:00 PM", RegistrationConfirmation.readableTime("14:00"));
    }
}