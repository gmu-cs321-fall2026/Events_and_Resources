package edu.gmu.cs321.events_resources.model;

import java.time.LocalDateTime;

/**
 * RegistrationStatus: Check if the valid user registered for this event
 *
 * WHO USES IT
 *   RegistrationService.status(...) builds one.
 *   RegistrationController sends it back for GET /events/{id}/register.
 *   The event pop-up uses it to decide which button to show:
 *   "Register" (registered = false) or "Cancel registration" (registered = true).
 *
 * WHAT THE FRONTEND RECEIVES (Spring turns this record into JSON automatically):
 *   registered:     {"eventId": 3, "registered": true,  "registeredAt": "2026-10-30T17:05:12", "remind": true,
 *                    "waitlisted": false, "waitlistPosition": null}
 *   not registered: {"eventId": 3, "registered": false, "registeredAt": null, "remind": false,
 *                    "waitlisted": false, "waitlistPosition": null}
 *   on waitlist:    {"eventId": 3, "registered": false, "registeredAt": null, "remind": false,
 *                    "waitlisted": true,  "waitlistPosition": 2}
 *
 * MASON-18: waitlisted / waitlistPosition were added so the pop-up can show
 * "You're #2 on the waitlist". When a seat opens the student is moved into
 * registrations automatically, and this answer flips to registered = true.
 *
 * "remind" tells the pop-op whether to tick "Remind me" box.
 * PUT /events/{id}/register/reminder answers with one of these
 * 
 * record: It only holds data and never changes after it's created, like Registration.
 */
public record RegistrationStatus(
        long eventId,               // which event we're answering about
        boolean registered,         // true = this user has a spot
        LocalDateTime registeredAt, // when they registered; null when registered is false
        boolean remind,             // remind on? Always false when registered is false
        boolean waitlisted,         // true = waiting in line for a seat
        Integer waitlistPosition    // 1 = next in line; null when waitlisted is false
) {

    /** Shortcut for "this user is on the waitlist at this position". */
    public static RegistrationStatus onWaitlist(long eventId, int position) {
        return new RegistrationStatus(eventId, false, null, false, true, position);
    }
}
