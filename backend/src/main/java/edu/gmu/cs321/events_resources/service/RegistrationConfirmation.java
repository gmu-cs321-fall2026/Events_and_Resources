package edu.gmu.cs321.events_resources.service;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import edu.gmu.cs321.events_resources.integration.NotificationsClient;
import edu.gmu.cs321.events_resources.model.Event;
import edu.gmu.cs321.events_resources.repository.EventRepository;

/**
 * RegistrationConfirmation: tells a student "you're registered" through the
 * Notifications subsystem (Subsystem 6), using their "registration" template:
 *
 *   "You have successfully registered for {title} on {date} at {time}. {link}"
 *
 * WHO CALLS IT
 *   RegistrationService.register      a student registers for an event
 *   WaitlistService.fillOpenSeats     a waitlisted student gets a freed seat
 *                                     (Notifications has no "promoted" template yet,
 *                                     and the student IS now registered, so the same
 *                                     message is accurate)
 *
 * TWO RULES
 *   1. Send only AFTER the database saves the registration (after the transaction
 *      commits). If the save is undone, the student must not get a confirmation for a
 *      seat they don't have.
 *   2. A notification problem never breaks registering. If Notifications is down, the
 *      registration still succeeds and we only log a warning (NotificationsClient.send
 *      already returns false instead of throwing).
 */
@Service
public class RegistrationConfirmation {

    private static final Logger log = LoggerFactory.getLogger(RegistrationConfirmation.class);

    /** "Saturday, Nov 21, 2026" and "8:30 AM": what the student reads in the message. */
    private static final DateTimeFormatter DATE_TEXT =
            DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", Locale.US);
    private static final DateTimeFormatter TIME_TEXT =
            DateTimeFormatter.ofPattern("h:mm a", Locale.US);

    private final EventRepository events;
    private final NotificationsClient notifications;

    public RegistrationConfirmation(EventRepository events, NotificationsClient notifications) {
        this.events = events;
        this.notifications = notifications;
    }

    /**
     * Rule 1: wait for the commit, then send.
     *
     * Inside a @Transactional method, nothing is saved for real until the method
     * finishes. So instead of sending now, we leave Spring a note:
     * "when this transaction commits, run afterCommit()". If it rolls back instead,
     * the note is thrown away and nothing is sent.
     *
     * Outside a transaction (e.g. a unit test), there's nothing to wait for, so it sends right away.
     */
    public void sendAfterCommit(long eventId, UUID userId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(eventId, userId);
                }
            });
        } else {
            send(eventId, userId);
        }
    }

    /**
     * Looks up the event and asks Notifications to send the message.
     * Returns true if Notifications accepted it.
     *
     * Rule 2: catches everything. By the time this runs the registration is already
     * saved, so an error here must not turn the student's 201 into a 500.
     */
    boolean send(long eventId, UUID userId) {
        try {
            Optional<Event> found = events.findById(eventId);
            if (found.isEmpty()) {
                log.warn("No confirmation sent: event {} not found", eventId);
                return false;
            }
            boolean accepted = notifications.send(NotificationsClient.REGISTRATION, userId, params(found.get()));
            if (!accepted) {
                log.warn("Registration confirmation for event {} to user {} was not sent", eventId, userId);
            }
            return accepted;
        } catch (RuntimeException e) {
            log.warn("Registration confirmation for event {} to user {} failed: {}", eventId, userId, e.getMessage());
            return false;
        }
    }

    /**
     * The four values the "registration" template needs. Notifications rejects the
     * request if any is missing, so a missing value is sent as "" instead of null
     * (Map.of also refuses nulls).
     */
    static Map<String, String> params(Event event) {
        return Map.of(
                "title", orEmpty(event.eventTitle()),
                "date", readableDate(event.date()),
                "time", readableTime(event.time()),
                "link", orEmpty(event.registerLink()));
    }

    /** "2026-11-21" -> "Saturday, Nov 21, 2026". Anything unreadable is sent as it is. */
    static String readableDate(String date) {
        if (date == null) {
            return "";
        }
        try {
            return LocalDate.parse(date).format(DATE_TEXT);
        } catch (DateTimeParseException e) {
            return date;
        }
    }

    /** "08:30" -> "8:30 AM". Anything unreadable is sent as it is. */
    static String readableTime(String time) {
        if (time == null) {
            return "";
        }
        try {
            return LocalTime.parse(time).format(TIME_TEXT);
        } catch (DateTimeParseException e) {
            return time;
        }
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }
}