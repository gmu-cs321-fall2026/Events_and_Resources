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
 *   registered:     {"eventId": 3, "registered": true,  "registeredAt": "2026-10-30T17:05:12"}
 *   not registered: {"eventId": 3, "registered": false, "registeredAt": null}
 *
 * record: It only holds data and never changes after it's created, like Registration.
 */
public record RegistrationStatus(
        long eventId,               // which event we're answering about
        boolean registered,         // true = this user has a spot
        LocalDateTime registeredAt  // when they registered; null when registered is false
) {
}
