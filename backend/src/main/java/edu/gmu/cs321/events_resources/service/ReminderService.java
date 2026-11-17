package edu.gmu.cs321.events_resources.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import edu.gmu.cs321.events_resources.integration.NotificationsClient;
import edu.gmu.cs321.events_resources.model.DueReminder;
import edu.gmu.cs321.events_resources.repository.RegistrationRepository;

/**
 * ReminderService: sends the reminders that are due.
 *
 *   ReminderScheduler (every 5 min)  ->  ReminderService (this file)
 *        ->  RegistrationRepository (which reminders are due?)
 *        ->  NotificationsClient (please send this one)
 *
 * WHY WE SCHEDULE IT OURSELVES: Notifications is event-driven (their ADR 001). They only
 * send something when another subsystem calls them, and they don't schedule anything.
 * So at reminder time, we call them.
 *
 * THE RULES
 *   - A reminder is due when it's on, not sent yet, and the event starts within the
 *     next reminders.days-before days (1 = "the day before").
 *   - It is marked as sent ONLY after Notifications accepts it. If they're down, it
 *     stays unsent and the next check tries again, until the event has started.
 *   - "Now" is in Career Services' time zone (reminders.time-zone), because event
 *     dates and times are stored as local times, while servers often run in UTC.
 */

@Service
public class ReminderService {
    private final RegistrationRepository registrations;
    private final NotificationsClient notifications;
    private final int daysBefore;
    private final Clock clock;

    @Autowired
    public ReminderService(RegistrationRepository registrations,
                           NotificationsClient notifications,
                           @Value("${reminders.days-before:1}") int daysBefore,
                           @Value("${reminders.time-zone:America/New_York}") String timeZone) {
        this(registrations, notifications, daysBefore, Clock.system(ZoneId.of(timeZone)));
    }

    /** For the unit test: a fixed Clock makes "now" the same on every run. */
    ReminderService(RegistrationRepository registrations, NotificationsClient notifications,
                    int daysBefore, Clock clock) {
        this.registrations = registrations;
        this.notifications = notifications;
        this.daysBefore = daysBefore;
        this.clock = clock;
    }

    /**
     * Sends every due reminder. Returns how many Notifications accepted.
     */
    public int sendDueReminders() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<DueReminder> due = registrations.findDueReminders(now, now.plusDays(daysBefore));

        int sent = 0;
        for (DueReminder reminder : due) {
            Map<String, String> params = paramsFor(reminder, now.toLocalDate());
            if (notifications.send(NotificationsClient.EVENT_REMINDER, reminder.userId(), params)) {
                registrations.markReminderSent(reminder.registrationId());
                sent++;
            }
        }
        return sent;
    }

    /**
     * The values Notifications' "event_reminder" template needs:
     *   "Your {title} is in {days} days at {location}. {link}"
     * days is at least 1, so an event later today reads "in 1 days", not "in 0 days".
     * (We've asked Notifications whether the template could take the date and time instead.)
     * link may be empty: not every event has a registration link.
     */
    static Map<String, String> paramsFor(DueReminder reminder, LocalDate today) {
        long days = Math.max(1, ChronoUnit.DAYS.between(today, reminder.eventDate()));
        return Map.of(
                "title", reminder.eventTitle(),
                "days", String.valueOf(days),
                "location", reminder.location(),
                "link", reminder.registerLink() == null ? "" : reminder.registerLink());
    }
}
