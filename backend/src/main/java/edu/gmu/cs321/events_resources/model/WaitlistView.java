package edu.gmu.cs321.events_resources.model;

import java.util.List;
import java.util.UUID;

/**
 * WaitlistView: what staff see for one event's waitlist (MASON-19).
 *
 * WHO USES IT
 *   GET /events/{id}/waitlist       returns one (justAccepted is always empty)
 *   PUT /events/{id}/capacity       returns one after the capacity changes;
 *                                   justAccepted lists who moved off the waitlist into a seat
 *
 * WHAT THE FRONTEND RECEIVES:
 *   {"eventId": 3, "capacity": 35, "registeredCount": 31,
 *    "waitlist": [],
 *    "justAccepted": ["00000000-0000-0000-0000-000000000004"]}
 */
public record WaitlistView(
        long eventId,
        int capacity,
        int registeredCount,
        List<WaitlistEntry> waitlist,
        List<UUID> justAccepted
) {
}
