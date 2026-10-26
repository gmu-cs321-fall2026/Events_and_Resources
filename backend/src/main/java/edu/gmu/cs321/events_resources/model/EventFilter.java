package edu.gmu.cs321.events_resources.model;
import java.time.LocalDate;

public record EventFilter(
        LocalDate from,    // earliest event date to include (inclusive), or null
        LocalDate to,      // latest event date to include (inclusive), or null
        String category,   // exactly one of EventService.CATEGORIES, or null
        String audience,   // an audience name from the audiences table, e.g. "CS Majors", or null
        String format      // "In-person" or "Virtual", or null
)	{
    /** A filter with nothing set: "give me every event". Handy for tests and future callers. */
    public static EventFilter none() {
        return new EventFilter(null, null, null, null, null);
    }
}
