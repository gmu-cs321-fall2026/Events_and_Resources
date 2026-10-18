
package edu.gmu.cs321.events_resources.service;

import edu.gmu.cs321.events_resources.model.Event;
import edu.gmu.cs321.events_resources.repository.EventRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
public class EventService {

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public long createEvent(Event event) {

        if (event.eventTitle() == null ||
            event.eventTitle().isBlank()) {
            throw new IllegalArgumentException(
                "Event title is required"
            );
        }

        if (event.capacity() < 0) {
            throw new IllegalArgumentException(
                "Capacity cannot be negative"
            );
        }

        LocalDate.parse(event.date());
        LocalTime.parse(event.time());

        return eventRepository.createEvent(event);
    }
}

