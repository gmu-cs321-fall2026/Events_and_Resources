// "This file lives in the model/ folder." Must match the folder the file is in.
package edu.gmu.cs321.events_resources.model;


/**
 * RegisteredEvent: one event in a student's "my registered events" list.
 *
 * WHO USES IT
 *   RegistrationRepository.findEventsForUser(...) builds these from a SQL JOIN.
 *   GET /users/{userId}/events/registered returns a page of them.
 *   Subsystem 2 (Application Tracker) reads this list to show events on the student's calendar.
 *
 * WHAT THE FRONTEND / OTHER TEAMS RECEIVE (one item of "items"):
 *   {"id": 3, "eventTitle": "Capital One Tech Talk", "host": "Capital One",
 *    "date": "2026-10-22", "time": "17:30", "location": "Johnson Center",
 *    "tag": "Employer Info Session", "registerLink": null,
 *    "capacity": 1, "registeredCount": 1, "registeredAt": "2026-10-12T19:45:03"}
 *
 * WHY NOT REUSE AN Event CLASS?
 *   A teammate is building Event.java for GET/POST /events. A separate name here means
 *   our branches can't collide when we merge. The fields match the Event object in
 *   Interface Contract v1, plus registeredAt.
 *
 * WHY ARE DATE AND TIME STRINGS?
 *   So the JSON matches the contract exactly ("2026-10-20", "15:00").
 *   The SQL query formats them with to_char(...) before they get here.
 */
public record RegisteredEvent(
        long id,              // the event's id                        (events.id)
        String eventTitle,    // e.g. "Resume Workshop"                 (events.event_title)
        String host,          // who runs it, e.g. "Career Services"    (events.host)
        String date,          // "YYYY-MM-DD"                           (events.event_date)
        String time,          // "HH:mm", 24-hour                       (events.event_time)
        String location,      // e.g. "Horizon Hall 2400"               (events.location)
        String tag,           // e.g. "Workshop"                        (events.tag)
        String registerLink,  // optional external link; may be null    (events.register_link)
        int capacity,         // max seats; 0 = unlimited               (events.capacity)
        int registeredCount,  // seats taken so far                     (events.registered_count)
        String registeredAt   // when THIS student registered           (registrations.created_at)
) {
}
