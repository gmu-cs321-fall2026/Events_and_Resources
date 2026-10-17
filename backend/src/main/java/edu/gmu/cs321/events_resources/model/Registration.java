package edu.gmu.cs321.events_resources.model;

// Java's built-in type for a date + time with no time zone, e.g. 2026-10-12T19:45:03.
// We use it for createdAt (when the registration happened).
import java.time.LocalDateTime;

/**
 * Registration: one row of the "registrations" table.
 * It means: "user {userId} registered for event {eventId} at {createdAt}".
 *
 * WHO USES IT
 *   RegistrationRepository.insert(...) creates one after saving to the database.
 *   RegistrationController sends it back as the response to POST /events/{id}/register.
 *
 * WHAT THE FRONTEND RECEIVES (Spring turns this record into JSON automatically):
 *   {"id": 12, "eventId": 3, "userId": 7, "createdAt": "2026-10-12T19:45:03"}
 *
 * WHY A RECORD
 *   It only holds data and never changes after it's created. One line gives us a
 *   constructor and the getters id(), eventId(), userId(), createdAt().
 */
public record Registration(
        long id,                 // the registration's own id (set by the database)
        long eventId,            // which event         -> events.id
        long userId,             // which user          -> the user's id from Shared Core
        LocalDateTime createdAt  // when they registered (set by the database: DEFAULT now())
) {
}