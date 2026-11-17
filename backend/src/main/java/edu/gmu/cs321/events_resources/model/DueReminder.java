package edu.gmu.cs321.events_resources.model;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * DueReminder: one reminder that should be sent now.
 * "User {userId} turned on the reminder for event {eventId}, which starts soon."
 *
 * WHO USES IT
 *   RegistrationRepository.findDueReminders(...) builds these from the database.
 *   ReminderService turns each one into a message for the Notifications subsystem.
 *
 * It never goes to the website, so it's not part of our API.
 */

public record DueReminder(
    long registrationId,  // which registration row; used to mark the reminder as sent
    UUID userId,          // who gets the reminder (Shared Core's user id)
    long eventId,
    String eventTitle,
    LocalDate eventDate,
    LocalTime eventTime,
    String location,
    String registerLink   // may be null
) {
}
