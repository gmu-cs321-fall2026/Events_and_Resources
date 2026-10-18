
package edu.gmu.cs321.events_resources.controller;

import edu.gmu.cs321.events_resources.model.Event;
import edu.gmu.cs321.events_resources.service.EventService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    public ResponseEntity<Long> createEvent(
            @RequestBody Event event) {

        long eventId = eventService.createEvent(event);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(eventId);
    }
}
