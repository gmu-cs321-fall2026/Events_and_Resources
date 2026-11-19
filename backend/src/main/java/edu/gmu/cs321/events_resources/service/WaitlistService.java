package edu.gmu.cs321.events_resources.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.gmu.cs321.events_resources.common.ApiException;
import edu.gmu.cs321.events_resources.model.CapacityRequest;
import edu.gmu.cs321.events_resources.model.CurrentUser;
import edu.gmu.cs321.events_resources.model.EventSeats;
import edu.gmu.cs321.events_resources.model.RegistrationStatus;
import edu.gmu.cs321.events_resources.model.WaitlistView;
import edu.gmu.cs321.events_resources.repository.RegistrationRepository;
import edu.gmu.cs321.events_resources.repository.WaitlistRepository;

/**
 * WaitlistService: the RULES for the waitlist.
 *
 *   MASON-18 (student)  join the waitlist when an event is full, leave it, see your place in line
 *   MASON-19 (staff)    see an event's waitlist, change the capacity
 *
 * THE BIG RULE: whenever a seat opens, the first person in line gets it automatically.
 * A seat opens in two ways, and both call fillOpenSeats(...):
 *   1. staff raise the capacity       (changeCapacity, below)   e.g. 30 -> 35 accepts up to 5 people
 *   2. a registered student cancels   (RegistrationService.cancel)
 *
 * The team decided (Nov 16) that "override a waitlisted spot" means staff RAISE THE
 * CAPACITY to let waitlisted people in. Nobody is ever pushed past the capacity.
 *
 * Like RegistrationService, it says "no" by throwing ApiException, and ErrorHandler
 * turns that into the standard {"error", "code", "detail"} JSON.
 */
@Service
public class WaitlistService {

    private final WaitlistRepository waitlist;
    private final RegistrationRepository registrations;

    public WaitlistService(WaitlistRepository waitlist, RegistrationRepository registrations) {
        this.waitlist = waitlist;
        this.registrations = registrations;
    }
    /**
     * Puts the logged-in student at the end of the waitlist. The rules, in order: FOR STUDENTS
     *   1. The event must exist                 -> otherwise 404 NOT_FOUND
     *   2. They must not already be registered  -> otherwise 409 ALREADY_REGISTERED
     *   3. They must not already be in line     -> otherwise 409 ALREADY_WAITLISTED
     *   4. The event must really be full        -> otherwise 409 SPOTS_AVAILABLE (just register instead)
     *   5. Add them to the end of the line      -> the controller returns 201 + their position
     *
     * lockSeats holds the event row until we finish, so a seat can't open between
     * rule 4 and step 5 and leave them waiting for a seat that's already free.
     */
    @Transactional
    public RegistrationStatus join(long eventId, CurrentUser user) {
        EventSeats seats = waitlist.lockSeats(eventId)
                .orElseThrow(() -> ApiException.notFound("Event " + eventId + " not found"));

        if (registrations.isRegistered(eventId, user.id())) {
            throw ApiException.conflict("ALREADY_REGISTERED", "You're already registered for this event");
        }
        if (waitlist.position(eventId, user.id()).isPresent()) {
            throw ApiException.conflict("ALREADY_WAITLISTED", "You're already on the waitlist for this event");
        }
        if (!seats.isFull()) {
            throw ApiException.conflict("SPOTS_AVAILABLE", "This event still has spots open, so you can register now");
        }

        try {
            waitlist.add(eventId, user.id());
        } catch (DuplicateKeyException e) {
            // A double-click: both requests passed the check above at the same moment.
            throw ApiException.conflict("ALREADY_WAITLISTED", "You're already on the waitlist for this event");
        }

        int position = waitlist.position(eventId, user.id()).orElseThrow();
        return RegistrationStatus.onWaitlist(eventId, position);
    }

    /**
     * Takes the logged-in student off the waitlist.
     *   1. The event must exist                -> otherwise 404 NOT_FOUND
     *   2. They must be on its waitlist        -> otherwise 404 NOT_WAITLISTED
     *      (this also happens if they were just given a seat, so the website re-checks their status)
     */
    @Transactional
    public void leave(long eventId, CurrentUser user) {
        if (!registrations.eventExists(eventId)) {
            throw ApiException.notFound("Event " + eventId + " not found");
        }
        if (!waitlist.remove(eventId, user.id())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_WAITLISTED",
                    "You aren't on the waitlist for this event", "");
        }
    }

    /** This user's place in line (1 = next), or empty if they aren't waiting. Used by RegistrationService.status. */
    public Optional<Integer> position(long eventId, UUID userId) {
        return waitlist.position(eventId, userId);
    }

    /**
     * Staff/ Admin view of one event's waitlist.
     *   1. Staff or admin only                 -> otherwise 403 FORBIDDEN
     *   2. The event must exist                -> otherwise 404 NOT_FOUND
     */
    public WaitlistView view(long eventId, CurrentUser user) {
        requireStaff(user);
        EventSeats seats = waitlist.findSeats(eventId)
                .orElseThrow(() -> ApiException.notFound("Event " + eventId + " not found"));
        return new WaitlistView(eventId, seats.capacity(), seats.registeredCount(),
                waitlist.list(eventId), List.of());
    }

    /**
     * Staff change an event's capacity, then the waitlist fills any seats that opened.
     *
     * EXAMPLE: capacity 30, 30 registered, 1 on the waitlist. Staff set capacity to 35.
     *   -> 5 seats open, the 1 waitlisted student is moved in automatically,
     *      registeredCount becomes 31, and 4 seats are still free for anyone.
     *
     * The rules, in order:
     *   1. Staff or admin only                                  -> otherwise 403 FORBIDDEN
     *   2. "capacity" must be given and be 0 or more            -> otherwise 400 VALIDATION_ERROR
     *      (0 = unlimited, which lets EVERYONE on the waitlist in)
     *   3. The event must exist                                 -> otherwise 404 NOT_FOUND
     *   4. It can't be lower than the people already registered -> otherwise 400 VALIDATION_ERROR
     *      (we never kick out a registered student)
     *   5. Save it, fill the open seats from the waitlist, return the updated waitlist
     * @Transactional: the new capacity and every move off the waitlist happen together, or not at all.
     */
    @Transactional
    public WaitlistView changeCapacity(long eventId, CapacityRequest request, CurrentUser user) {
        requireStaff(user);

        Integer capacity = (request == null) ? null : request.capacity();
        if (capacity == null || capacity < 0) {
            throw ApiException.badRequest("Capacity must be 0 (unlimited) or a positive number", "capacity");
        }

        EventSeats seats = waitlist.lockSeats(eventId)
                .orElseThrow(() -> ApiException.notFound("Event " + eventId + " not found"));

        if (capacity > 0 && capacity < seats.registeredCount()) {
            throw ApiException.badRequest(
                    "Capacity can't be lower than the " + seats.registeredCount()
                            + " students already registered", "capacity");
        }

        waitlist.setCapacity(eventId, capacity);
        List<UUID> accepted = fillOpenSeats(eventId);

        EventSeats after = waitlist.findSeats(eventId).orElseThrow();
        return new WaitlistView(eventId, after.capacity(), after.registeredCount(),
                waitlist.list(eventId), accepted);
    }
    // Shared: hand open seats to the people waiting
    /**
     * Gives every open seat to the next person in line, first come first served,
     * and returns who got one (in order). Returns an empty list if there are no open
     * seats or nobody is waiting.
     *
     * Called by changeCapacity (above) and RegistrationService.cancel.
     * It must run inside their transaction: lockSeats keeps the event row locked,
     * so a brand-new registration can't grab the open seat ahead of the line.
     *
     * TODO (MASON-24): once the Notifications client exists, send each accepted
     * student a "You got a spot" message here.
     */
    @Transactional
    public List<UUID> fillOpenSeats(long eventId) {
        List<UUID> accepted = new ArrayList<>();

        Optional<EventSeats> seats = waitlist.lockSeats(eventId);
        if (seats.isEmpty()) {
            return accepted;
        }

        int open = seats.get().openSeats();
        while (open > 0) {
            List<UUID> next = waitlist.firstInLine(eventId, 1);
            if (next.isEmpty()) {
                break; // nobody left in line
            }
            UUID student = next.get(0);
            if (waitlist.moveToRegistrations(eventId, student)) {
                accepted.add(student);
                open--;
            }
        }
        return accepted;
    }

    /** 403 unless the user is staff or admin. (AuthInterceptor checks this too; this is a second lock on the door.) */
    private void requireStaff(CurrentUser user) {
        if (!user.isStaff()) {
            throw ApiException.forbidden("Only Career Services staff or admin can manage waitlists");
        }
    }
}
