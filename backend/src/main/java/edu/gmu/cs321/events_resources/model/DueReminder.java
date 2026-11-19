package edu.gmu.cs321.events_resources.model;
import java.util.UUID;

/**
 * DueReminder: one reminder that should be sent now.
 * "User {userId} turned on the reminder for event {eventId}, which starts soon."
 *
 * WHO USES IT
 *   RegistrationRepository.findUpcomingReminders(...) builds these from the database.
 *   GET /registrations/reminders sends a page of them to the Notifications subsystem,
 *   which decides when to send each reminder and remembers which ones it already sent.
 *
 * WHAT NOTIFICATIONS RECEIVES (one item of the list):
 *   {"registrationId": 41, "userId": "00000000-0000-0000-0000-000000000003",
 *    "eventId": 3, "eventTitle": "Resume Workshop", "date": "2026-11-21", "time": "08:30",
 *    "location": "Horizon Hall", "registerLink": null}
 *
 * registrationId is unique per student + event, so Notifications can use it to
 * avoid sending the same reminder twice. date and time use the same format as our
 * Event object, in GMU's local time.
 */

public record DueReminder(
    long registrationId,  // which registration row; used to mark the reminder as sent
    UUID userId,          // who gets the reminder (Shared Core's user id)
    long eventId,
    String eventTitle,
    String date,
    String time,
    String location,
    String registerLink   // may be null
) {
}
