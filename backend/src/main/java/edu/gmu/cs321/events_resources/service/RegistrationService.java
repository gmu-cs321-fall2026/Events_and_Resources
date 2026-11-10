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

// Our record from model/: one event in a student's "my events" list
import edu.gmu.cs321.events_resources.model.RegisteredEvent;

// Our record from model/: one saved registration
import edu.gmu.cs321.events_resources.model.Registration;
import edu.gmu.cs321.events_resources.model.RegistrationStatus;
// Our class from repository/: all the SQL. The service calls it; it never writes SQL itself.
import edu.gmu.cs321.events_resources.repository.RegistrationRepository;

import java.time.LocalDateTime;
// Java's built-in ordered collection
import java.util.List;

// Java's built-in type for Shared Core's user ids
import java.util.UUID;

import java.util.Optional;

// Spring throws this when an INSERT breaks a UNIQUE rule in the database
// (here: the same student registering twice for the same event).
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

    // The repository with all the SQL. "final" = set once, never replaced.
    private final RegistrationRepository registrations;

    // Spring calls this at startup and passes in the RegistrationRepository.
    public RegistrationService(RegistrationRepository registrations) {
        this.registrations = registrations;
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
        try {
            return registrations.insert(eventId, user.id());
        } catch (DuplicateKeyException e) {
            // Rare: the same student double-clicked, and both requests passed Rule 2 at the
            // same moment. The table's UNIQUE (event_id, user_id) rule stopped the second one.
            // Throwing here also undoes its +1 seat (because of @Transactional).
            throw ApiException.conflict("ALREADY_REGISTERED", "You're already registered for this event");
        }
    }

    /**
     * Cancels the logged-in user's registration for an event.
     * The rules, in order:
     *   1. The event must exist                    -> otherwise 404 NOT_FOUND
     *   2. The user must be registered for it      -> otherwise 404 NOT_REGISTERED
     *   3. Delete the registration and free the seat -> the controller returns 204 No Content
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

        return new RegistrationStatus(eventId, found.isPresent(), registeredAt);
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
}