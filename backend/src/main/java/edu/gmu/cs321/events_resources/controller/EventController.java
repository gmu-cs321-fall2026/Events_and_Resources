package edu.gmu.cs321.events_resources.controller;

import edu.gmu.cs321.events_resources.common.PagedResponse;  // the standard list shape: {items, page, pageSize, totalCount}
import edu.gmu.cs321.events_resources.model.Event;
import edu.gmu.cs321.events_resources.service.EventService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;     // marks a method that answers GET requests
import org.springframework.web.bind.annotation.PathVariable;   // reads a value from the URL path, e.g. the 3 in /events/3
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;   // reads a value after the ?, e.g. category in /events?category=Workshop
import org.springframework.web.bind.annotation.RestController;

/**
 * ENDPOINTS
 *   GET  /events        one page of events, with optional filters   -> 200   (public, no token)
 *   GET  /events/{id}   one event                                   -> 200 / 404 (public, no token)
 *   POST /events        create an event                             -> 201   (staff only)
 *
 * AuthInterceptor already lets every GET /events... through without a token,
 * so there's no auth code here.
 */
@RestController
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    /**
     * GET /events?page=&pageSize=&from=&to=&category=&audience=&format=
     *
     * @RequestParam(required = false) = "this one can be left out of the URL; then it's null".
     * defaultValue = what to use when it's left out (page 1, 20 per page).
     * All checking happens in EventService; this method only passes the values along.
     *
     * Examples:
     *   /events                                        every event, page 1
     *   /events?category=Workshop&format=Virtual       virtual workshops
     *   /events?from=2026-10-27&to=2026-11-02          events that week
     *   /events?audience=CS%20Majors                   events for CS Majors (%20 = a space in a URL)
     */
    @GetMapping
    public PagedResponse<Event> listEvents(                            // returning the object directly = 200 OK
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String from,               // YYYY-MM-DD, inclusive
            @RequestParam(required = false) String to,                 // YYYY-MM-DD, inclusive
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String audience,
            @RequestParam(required = false) String format) {

        return eventService.listEvents(page, pageSize, from, to, category, audience, format);
    }

    /**
     * GET /events/{id}: one event.
     * "long id" means /events/abc never reaches here: ErrorHandler answers 400 VALIDATION_ERROR.
     * If the number isn't a real event, EventService throws and ErrorHandler answers 404 NOT_FOUND.
     */
    @GetMapping("/{id}")
    public Event getEvent(@PathVariable long id) {
        return eventService.getEvent(id);
    }

    @PostMapping
    public ResponseEntity<Event> createEvent(@RequestBody Event event) {
        Event saved = eventService.createEvent(event);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
	}
}