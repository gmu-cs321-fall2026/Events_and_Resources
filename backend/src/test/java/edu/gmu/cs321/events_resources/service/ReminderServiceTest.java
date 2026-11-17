package edu.gmu.cs321.events_resources.service;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import edu.gmu.cs321.events_resources.integration.FakeTokenValidator;
import edu.gmu.cs321.events_resources.integration.NotificationsClient;
import edu.gmu.cs321.events_resources.model.DueReminder;
import edu.gmu.cs321.events_resources.repository.RegistrationRepository;

/**
 * Unit tests for ReminderService.
 *
 * Fakes stand in for the database and for Notifications, and a fixed Clock makes
 * "now" always Nov 20, 2026 at 9:00 in New York. No database, no network.
 *
 * Rules being checked:
 *   - it asks the repository for reminders between now and now + 1 day
 *   - each due reminder goes to Notifications as "event_reminder" with the right values
 *   - a reminder is marked sent ONLY when Notifications accepts it (so a failure retries)
 */

class ReminderServiceTest {
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 20, 9, 0);
    private static final Clock CLOCK = Clock.fixed(NOW.atZone(NEW_YORK).toInstant(), NEW_YORK);

    private static final DueReminder TOMORROW = new DueReminder(
            11L, FakeTokenValidator.STUDENT_ID, 3L, "Resume Workshop",
            LocalDate.of(2026, 11, 21), LocalTime.of(8, 30), "Horizon Hall", null);
    private static final DueReminder LATER_TODAY = new DueReminder(
            12L, FakeTokenValidator.STUDENT2_ID, 4L, "Capital One Tech Talk",
            LocalDate.of(2026, 11, 20), LocalTime.of(17, 30), "Johnson Center", "https://example.com/c1");

    /** Fake repository: returns the given due reminders and remembers what was marked sent. */
    static class FakeRegistrationRepository extends RegistrationRepository {
        final List<DueReminder> due;
        final List<Long> markedSent = new ArrayList<>();
        LocalDateTime askedFrom;
        LocalDateTime askedUntil;

        FakeRegistrationRepository(List<DueReminder> due) {
            super(null);
            this.due = due;
        }

        @Override
        public List<DueReminder> findDueReminders(LocalDateTime from, LocalDateTime until) {
            askedFrom = from;
            askedUntil = until;
            return due;
        }

        @Override
        public boolean markReminderSent(long registrationId) {
            markedSent.add(registrationId);
            return true;
        }
    }

    /** Fake Notifications: records every send, and says yes unless the user is in "refuseFor". */
    static class FakeNotifications extends NotificationsClient {
        record Sent(String templateId, UUID userId, Map<String, String> params) {
        }

        final List<Sent> sent = new ArrayList<>();
        UUID refuseFor;

        FakeNotifications() {
            super("http://notifications.invalid");
        }

        @Override
        public boolean send(String templateId, UUID userId, Map<String, String> params) {
            sent.add(new Sent(templateId, userId, params));
            return !userId.equals(refuseFor);
        }
    }

    @Test
    void asksForEventsInTheNextDayUsingNewYorkTime() {
        FakeRegistrationRepository repo = new FakeRegistrationRepository(List.of());
        new ReminderService(repo, new FakeNotifications(), 1, CLOCK).sendDueReminders();

        assertEquals(NOW, repo.askedFrom);
        assertEquals(NOW.plusDays(1), repo.askedUntil);
    }

    @Test
    void sendsEachDueReminderAndMarksItSent() {
        FakeRegistrationRepository repo = new FakeRegistrationRepository(List.of(TOMORROW, LATER_TODAY));
        FakeNotifications notifications = new FakeNotifications();

        int sent = new ReminderService(repo, notifications, 1, CLOCK).sendDueReminders();

        assertEquals(2, sent);
        assertEquals(List.of(11L, 12L), repo.markedSent);

        FakeNotifications.Sent first = notifications.sent.get(0);
        assertEquals("event_reminder", first.templateId());
        assertEquals(FakeTokenValidator.STUDENT_ID, first.userId());
        assertEquals(Map.of("title", "Resume Workshop", "days", "1",
                "location", "Horizon Hall", "link", ""), first.params()); // no link -> ""
    }

    @Test
    void aReminderNotificationsRefusesIsNotMarkedSoItIsRetried() {
        FakeRegistrationRepository repo = new FakeRegistrationRepository(List.of(TOMORROW, LATER_TODAY));
        FakeNotifications notifications = new FakeNotifications();
        notifications.refuseFor = FakeTokenValidator.STUDENT_ID; // TOMORROW's student

        int sent = new ReminderService(repo, notifications, 1, CLOCK).sendDueReminders();

        assertEquals(1, sent);
        assertEquals(List.of(12L), repo.markedSent); // only LATER_TODAY
        assertEquals(2, notifications.sent.size());  // but both were tried
    }

    @Test
    void anEventLaterTodayCountsAsOneDayAway() {
        Map<String, String> params = ReminderService.paramsFor(LATER_TODAY, NOW.toLocalDate());

        assertEquals("1", params.get("days"));
        assertEquals("https://example.com/c1", params.get("link"));
    }

    @Test
    void nothingDueMeansNothingSent() {
        FakeNotifications notifications = new FakeNotifications();

        int sent = new ReminderService(new FakeRegistrationRepository(List.of()), notifications, 1, CLOCK)
                .sendDueReminders();

        assertEquals(0, sent);
        assertTrue(notifications.sent.isEmpty());
    }
}
