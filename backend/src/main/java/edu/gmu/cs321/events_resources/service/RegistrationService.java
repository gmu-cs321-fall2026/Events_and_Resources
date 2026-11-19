// "This file lives in the service/ folder." Must match the folder the file is in.
package edu.gmu.cs321.events_resources.service;

// ---------------------------------------------------------------------------
// IMPORTS: tell Java where to find each class this file uses.
// ---------------------------------------------------------------------------

// Our exception from common/: how we say "no" (404, 409, 403). ErrorHandler turns it into JSON.
import edu.gmu.cs321.events_resources.common.ApiException;

// Our record from common/: the {items, page, pageSize, totalCount} list shape
import edu.gmu.cs321.events_resources.common.PagedResponse;

// Our record from model/: the logged-in user (UUID id + list of roles), attached by AuthInterceptor
import edu.gmu.cs321.events_resources.model.CurrentUser;

// Our record from model/: one student who asked for a reminder about one event
import edu.gmu.cs321.events_resources.model.DueReminder;

// Our record from model/: one event in a student's "my events" list
import edu.gmu.cs321.events_resources.model.RegisteredEvent;

// Our record from model/: one saved registration
import edu.gmu.cs321.events_resources.model.Registration;
import edu.gmu.cs321.events_resources.model.RegistrationStatus;
// Our class from repository/: all the SQL. The service calls it; it never writes SQL itself.
import edu.gmu.cs321.events_resources.repository.RegistrationRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
// Java's built-in ordered collection
import java.util.ArrayList;
import java.util.List;

// Java's built-in type for Shared Core's user ids
import java.util.UUID;

import java.util.Optional;

// Spring throws this when an INSERT breaks a UNIQUE rule in the database
// (here: the same student registering twice for the same event).
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
// For @Service: "Spring, create one of these at startup" (and it marks this as the service layer)
import org.springframework.stereotype.Service;

// For @Transactional: makes a method all-or-nothing in the database
import org.springframework.transaction.annotation.Transactional;

/**
 * RegistrationService: the RULES for registering.
 *
 *   Controller  ->  Service (rules, this file)  ->  Repository (SQL)  ->  PostgreSQL
 *
 * It decides yes or no; the repository does the actual database work.
 * To say "no", it THROWS an ApiException, and ErrorHandler turns that into the
 * right status code and the standard {"error", "code", "detail"} JSON.
 * It never deals with HTTP directly (no status codes, no requests).
 */
@Service // Spring creates one of these and passes it to RegistrationController
public class RegistrationService {

    /** GMU's time zone. Event dates and times are stored as local GMU times. */
    public static final ZoneId GMU_TIME = ZoneId.of("America/New_York");

    // The repository with all the SQL. "final" = set once, never replaced.
    private final RegistrationRepository registrations;

    // Where "now" comes from (only the reminders list uses it).
    private final Clock clock;

    /**
     * MASON-18: the waitlist rules. Used to fill a freed seat and to report a student's
     * place in line. null only in tests that don't involve the waitlist (the reminder tests).
     */
    private final WaitlistService waitlist;

    /**
     * Sends the "you're registered" message through Notifications after a registration is saved.
     * null only in tests that don't check confirmations.
     */
    private final RegistrationConfirmation confirmation;

    // Spring calls this at startup and passes in the repository, the WaitlistService and the
    // RegistrationConfirmation. @Autowired tells Spring to use THIS constructor, since there are several.
    @Autowired
    public RegistrationService(RegistrationRepository registrations, WaitlistService waitlist,
                               RegistrationConfirmation confirmation) {
        this(registrations, waitlist, confirmation, Clock.system(GMU_TIME));
    }

     /** For tests: a fixed Clock makes "now" the same on every run. */
    RegistrationService(RegistrationRepository registrations, WaitlistService waitlist,
                        RegistrationConfirmation confirmation, Clock clock) {
        this.registrations = registrations;
        this.waitlist = waitlist;
        this.confirmation = confirmation;
        this.clock = clock;
    }

    /** For tests that use the waitlist but don't check confirmations (RegistrationServiceCancelTest). */
    RegistrationService(RegistrationRepository registrations, WaitlistService waitlist) {
        this(registrations, waitlist, null, Clock.system(GMU_TIME));
    }

    /** For tests that don't use the waitlist (RegistrationServiceReminderTest): nobody is ever waiting. */
    RegistrationService(RegistrationRepository registrations) {
        this(registrations, null, null, Clock.system(GMU_TIME));
    }

    /** For tests that don't use the waitlist (RegistrationServiceUpcomingRemindersTest), with a fixed Clock. */
    RegistrationService(RegistrationRepository registrations, Clock clock) {
        this(registrations, null, null, clock);
    }

    /**
     * Registers a user for an event. The rules, in order:
     *   1. The event must exist                   -> otherwise 404 NOT_FOUND
     *   2. They must not already be registered    -> otherwise 409 ALREADY_REGISTERED
     *   3. There must be a seat left              -> otherwise 409 EVENT_FULL
     *   4. Save the registration                  -> the controller returns 201 Created
     *
     * @Transactional = ALL-OR-NOTHING. Everything this method does in the database
     * either fully happens or is fully undone. If saving fails after the seat was
     * taken (step 3), the +1 is undone too, so registered_count always matches
     * the number of registrations.
     */
    @Transactional
    public Registration register(long eventId, CurrentUser user) {
        // Rule 1: does the event exist?
        if (!registrations.eventExists(eventId)) {
            throw ApiException.notFound("Event " + eventId + " not found");
        }

        // Rule 2: already registered?
        // Checked BEFORE taking a seat, so a duplicate click never uses up a seat.
        // user.id() is now a UUID (Shared Core's user id).
        if (registrations.isRegistered(eventId, user.id())) {
            throw ApiException.conflict("ALREADY_REGISTERED", "You're already registered for this event");
        }

        // Rule 3: take a seat (the capacity check). false = the event is full.
        if (!registrations.tryTakeSeat(eventId)) {
            throw ApiException.conflict("EVENT_FULL", "This event is full");
        }

        // Step 4: save the registration and return it (with its new id and timestamp).
        // Rare: throws exception when the same student double-clicked, and both requests passed Rule 2 at the
        // same moment. The table's UNIQUE (event_id, user_id) rule stopped the second one.
        // Throwing here also undoes its +1 seat (because of @Transactional).
        Registration saved;
        try {
            saved = registrations.insert(eventId, user.id());
        } catch (DuplicateKeyException e) {
            throw ApiException.conflict("ALREADY_REGISTERED", "You're already registered for this event");
        }

        if (confirmation != null)
            confirmation.sendAfterCommit(eventId, user.id());
        
        return saved;
    }

    /**
     * Cancels the logged-in user's registration for an event.
     * The rules, in order:
     *   1. The event must exist                    -> otherwise 404 NOT_FOUND
     *   2. The user must be registered for it      -> otherwise 404 NOT_REGISTERED
     *   3. Delete the registration and free the seat -> the controller returns 204 No Content
     *   4. MASON-18: give the freed seat to the first person on the waitlist (if anyone is waiting)
     *
     * A user can only ever cancel THEIR OWN registration: the user comes from the
     * login token, and there's no user id in the URL or body to change.
     *
     */
    @Transactional
    public void cancel(long eventId, CurrentUser user) {
        if (!registrations.eventExists(eventId)) {
            throw ApiException.notFound("Event " + eventId + " not found");
        }

        if (!registrations.cancel(eventId, user.id())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_REGISTERED",
                    "You aren't registered for this event", "");
        }

        if (waitlist != null) {
            waitlist.fillOpenSeats(eventId);
        }
    }

    /**
     * Is the logged-in user registered for this event?
     * The pop-up calls this when it opens, so it shows the right button straight away.
     *   1. The event must exist -> otherwise 404 NOT_FOUND
     *   2. Answer yes/no (and when they registered, if yes)
     *  find() returns an Optional: a box with the registration in it, or an empty box.
     * .map(...) runs only if the box has something in it: take its createdAt.
     * .orElse(null) = if the box was empty, use null.
     */
    public RegistrationStatus status(long eventId, CurrentUser user) {
        if (!registrations.eventExists(eventId)) {
            throw ApiException.notFound("Event " + eventId + " not found");
        }

        Optional<Registration> found = registrations.find(eventId, user.id());

        LocalDateTime registeredAt = found.map(Registration::createdAt).orElse(null);
        boolean remind = found.map(Registration::remind).orElse(false);

        // MASON-18: also say whether they're waiting in line, and where.
        Optional<Integer> place = (waitlist == null) ? Optional.empty() : waitlist.position(eventId, user.id());

        return new RegistrationStatus(eventId, found.isPresent(), registeredAt, remind,
                place.isPresent(), place.orElse(null));
    }

    /**
     * Turns the logged-in user's reminder for an event on or off.
     * The rules, in order:
     *   1. The event must exist                -> otherwise 404 NOT_FOUND
     *   2. The user must be registered for it  -> otherwise 404 NOT_REGISTERED
     *   3. Save the choice and return the updated status (the pop-up redraws from it)
     *
     * Like cancel(), the user comes from the login token, so nobody can change
     * someone else's reminder. The Notifications subsystem reads who wants a
     * reminder (GET /registrations/reminders) and sends it.
     */
    public RegistrationStatus setReminder(long eventId, CurrentUser user, boolean remind) {
        if (!registrations.eventExists(eventId)) {
            throw ApiException.notFound("Event " + eventId + " not found");
        }

        if (!registrations.setReminder(eventId, user.id(), remind)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_REGISTERED",
                    "You aren't registered for this event", "");
        }

        return status(eventId, user);
    }

    /**
     * One page of the events a user registered for.
     * Rule: students can only see THEIR OWN list. Staff and admins can see anyone's.
     *
     * @param userId    whose list is being asked for (from the URL, a UUID)
     * @param requester who is asking (from the login token)
     */
    public PagedResponse<RegisteredEvent> registeredEvents(UUID userId, CurrentUser requester, int page, int pageSize) {
        // Permission check -> 403 if a student asks for someone else's list.
        // UUIDs are objects, so compare them with .equals() (== would compare memory addresses).
        boolean ownList = requester.id().equals(userId);
        // isStaff() is true for staff OR admin (see CurrentUser).
        if (!ownList && !requester.isStaff()) {
            throw ApiException.forbidden("You can only view your own registered events");
        }

        // 400 if page/pageSize are out of range (page < 1, or pageSize not 1-50).
        PagedResponse.validate(page, pageSize);

        // Get this page of events, and the total count across all pages.
        List<RegisteredEvent> items = registrations.findEventsForUser(
                userId, pageSize, PagedResponse.offset(page, pageSize)); // limit = pageSize, offset = rows to skip
        long total = registrations.countEventsForUser(userId);

        // <> = "Java, figure out the type yourself" (here: RegisteredEvent)
        return new PagedResponse<>(items, page, pageSize, total);
    }

    /**
     * One page of reminders for the Notifications subsystem: students who turned on
     * "Remind me", for events starting between "from" and "to" (GMU local time).
     * Notifications decides when to send each one and remembers which ones it sent.
     *
     * The rules, in order:
     *   1. from / to must look like 2026-11-20T09:00   -> otherwise 400 VALIDATION_ERROR
     *      Left out: from = now, to = from + 1 day ("events in the next 24 hours")
     *   2. to must not be before from                  -> otherwise 400 VALIDATION_ERROR
     *   3. page / pageSize in range                    -> otherwise 400 VALIDATION_ERROR
     * Only staff/admin may ask (AuthInterceptor checks that before we get here).
     */
    public PagedResponse<DueReminder> upcomingReminders(String from, String to, int page, int pageSize) {
        List<String> problems = new ArrayList<>();

        // truncatedTo(MINUTES) drops seconds, so the default window is a tidy 09:00 -> 09:00.
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime start = parseDateTime(from, "from", now, problems);
        LocalDateTime end = parseDateTime(to, "to", start == null ? now.plusDays(1) : start.plusDays(1), problems);

        if (start != null && end != null && end.isBefore(start)) {
            problems.add("to: must not be before from");
        }
        if (!problems.isEmpty()) {
            throw ApiException.badRequest("Invalid reminder window", String.join("; ", problems));
        }

        PagedResponse.validate(page, pageSize);

        List<DueReminder> items = registrations.findUpcomingReminders(
                start, end, pageSize, PagedResponse.offset(page, pageSize));
        long total = registrations.countUpcomingReminders(start, end);
        return new PagedResponse<>(items, page, pageSize, total);
    }

    /**
     * Reads "2026-11-20T09:00" into a LocalDateTime. Empty -> the given default.
     * A bad value adds a problem and returns null, so all problems are reported at once.
     */
    private static LocalDateTime parseDateTime(String value, String name, LocalDateTime fallback,
                                               List<String> problems) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return LocalDateTime.parse(value.trim());
        } catch (DateTimeParseException e) {
            problems.add(name + ": must look like 2026-11-20T09:00");
            return null;
        }
    }
}