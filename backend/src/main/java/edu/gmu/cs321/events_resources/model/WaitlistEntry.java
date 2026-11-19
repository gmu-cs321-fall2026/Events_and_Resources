package edu.gmu.cs321.events_resources.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * WaitlistEntry: one person in an event's waitlist, as staff see it 
 *
 * WHAT THE FRONTEND RECEIVES (one item of "waitlist"):
 *   {"position": 1, "userId": "00000000-0000-0000-0000-000000000004", "joinedAt": "2026-11-17T09:30:00"}
 * position 1 = first in line, the next person to get a seat.
 * We only have the user's id: names and emails live in Shared Core, not in our database.
 */
public record WaitlistEntry(
        int position,
        UUID userId,
        LocalDateTime joinedAt
) {
}
