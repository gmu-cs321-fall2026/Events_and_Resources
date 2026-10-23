
package edu.gmu.cs321.events_resources.service;
import edu.gmu.cs321.events_resources.common.ApiException;
import edu.gmu.cs321.events_resources.model.Event;
import edu.gmu.cs321.events_resources.repository.EventRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
@Service
public class EventService {
	public static final List<String> CATEGORIES = List.of(
		"Internal",
		"Career Fair",
		"Workshop",
		"Employer Info Session",
		"Networking",
		"Panel"
	);

	public static final List<String> FORMATS = List.of(
		"In-person",
		"Virtual"
	);

	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
	private final EventRepository eventRepository;
	public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

	public Event createEvent(Event event) {
        // Every problem found gets added here as "field: what's wrong".
        List<String> problems = new ArrayList<>();
 
        // 1. TEXT FIELDS. The numbers are the column sizes in schema.sql (VARCHAR(200) etc.).
        //    Checking them here gives a clear 400 instead of the database crashing with a 500.
        String title    = requiredText(event.eventTitle(), "eventTitle", 200, problems);
        String host     = requiredText(event.host(),       "host",       200, problems);
        String location = requiredText(event.location(),   "location",   200, problems);
        String audience = requiredText(event.audience(),   "audience",   500, problems);
 
        // 2. DROPDOWN FIELDS: must be one of the allowed values.
        String category = oneOf(event.category(), "category", CATEGORIES, problems);
        String format   = oneOf(event.format(),   "format",   FORMATS,    problems);
 
        // 3. DATE AND TIME: must be real, e.g. not "2026-02-30" or "25:00".
        LocalDate date = parseDate(event.date(), problems);
        LocalTime time = parseTime(event.time(), problems);
 
        // 4. OPTIONAL LINK: blank counts as "no link" (null).
        String registerLink = blankToNull(event.registerLink());
        if (registerLink != null && registerLink.length() > 500) {
            problems.add("registerLink: must be 500 characters or fewer");
        }
 
        // 5. CAPACITY: 0 means unlimited, so only negative numbers are wrong.
        if (event.capacity() < 0) {
            problems.add("capacity: cannot be negative (use 0 for unlimited)");
        }
 
        // Anything wrong? Stop here. Nothing has been saved yet.
        // String.join("; ", problems) glues the list into one line: "a; b; c".
        if (!problems.isEmpty()) {
            throw ApiException.badRequest("Some fields are invalid", String.join("; ", problems));
        }
 
        // Everything is valid. Build a CLEAN copy to save: spaces trimmed, no id yet
        // (the database picks it), and registeredCount 0 (nobody has registered yet).
        // We ignore any id/registeredCount the caller sent, so nobody can fake them.
        Event clean = new Event(
                null, title, host, event.date(), event.time(), location,
                category, audience, format, registerLink, event.capacity(), 0);
 
        return eventRepository.insert(clean, date, time);
    }
 
    private String requiredText(String value, String field, int maxLength, List<String> problems) {
        if (value == null || value.isBlank()) {       // isBlank() = empty or only spaces
            problems.add(field + ": required");
            return null;
        }
        String trimmed = value.trim();                // trim() removes spaces at the start/end
        if (trimmed.length() > maxLength) {
            problems.add(field + ": must be " + maxLength + " characters or fewer");
        }
        return trimmed;
    }
 
    /** A dropdown field: required, and must be exactly one of the allowed values. */
    private String oneOf(String value, String field, List<String> allowed, List<String> problems) {
        if (value == null || value.isBlank()) {
            problems.add(field + ": required");
            return null;
        }
        if (!allowed.contains(value)) {               // contains() = is it in the list? (exact match)
            problems.add(field + ": must be one of " + String.join(", ", allowed));
        }
        return value;
    }
 
    /** "2026-10-20" -> LocalDate. Returns null (and records a problem) if it's missing or not a real date. */
    private LocalDate parseDate(String value, List<String> problems) {
        if (value == null || value.isBlank()) {
            problems.add("date: required");
            return null;
        }
        // try/catch: try to parse; if Java says it's not a date, record a problem instead of crashing.
        try {
            return LocalDate.parse(value);            // expects YYYY-MM-DD
        } catch (DateTimeParseException e) {
            problems.add("date: must be a real date in YYYY-MM-DD format");
            return null;
        }
    }
 
    /** "15:00" -> LocalTime. Returns null (and records a problem) if it's missing or not a real time. */
    private LocalTime parseTime(String value, List<String> problems) {
        if (value == null || value.isBlank()) {
            problems.add("time: required");
            return null;
        }
        try {
            return LocalTime.parse(value, TIME_FORMAT); // expects HH:mm
        } catch (DateTimeParseException e) {
            problems.add("time: must be a real time in HH:mm format (24-hour)");
            return null;
        }
    }
 
    /** Turns null, "" or "   " into null; otherwise returns the text trimmed. */
    private String blankToNull(String value) {
        // Ternary operator: condition ? valueIfTrue : valueIfFalse
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
