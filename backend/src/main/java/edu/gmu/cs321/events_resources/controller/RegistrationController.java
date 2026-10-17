// "This file lives in the controller/ folder." Must match the folder the file is in.
package edu.gmu.cs321.events_resources.controller;

// ---------------------------------------------------------------------------
// IMPORTS: tell Java where to find each class this file uses.
// ---------------------------------------------------------------------------

// Our class from common/: we only need its CURRENT_USER constant (the name the user is stored under)
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.gmu.cs321.events_resources.common.AuthInterceptor;
import edu.gmu.cs321.events_resources.common.PagedResponse;
import edu.gmu.cs321.events_resources.model.CurrentUser;
import edu.gmu.cs321.events_resources.model.RegisteredEvent;
import edu.gmu.cs321.events_resources.model.Registration;
import edu.gmu.cs321.events_resources.service.RegistrationService;

/**
 * RegistrationController: the API layer for registration.
 *
 *   Controller (this file)  ->  Service (rules)  ->  Repository (SQL)  ->  PostgreSQL
 *
 * Its only job is translating HTTP <-> Java: read values from the request, call the
 * service, and send back the result. No rules and no SQL here.
 *
 * By the time a method here runs, AuthInterceptor has already checked the token,
 * so "user" is always a real, logged-in user. Errors thrown by the service are
 * turned into JSON by ErrorHandler, so there's no error handling here either.
 *
 * ENDPOINTS
 *   POST /events/{id}/register                   register the logged-in user      -> 201
 *   GET  /users/{userId}/events/registered       list of events a user signed up for -> 200
 */
@RestController // Spring creates one of these and sends matching requests to its methods
public class RegistrationController {

    // The service with the rules. "final" = set once, never replaced.
    private final RegistrationService service;

    // Spring calls this at startup and passes in the RegistrationService.
    public RegistrationController(RegistrationService service) {
        this.service = service;
    }

    /**
     * POST /events/{id}/register: register the logged-in user for event {id}.
     *
     * Success: 201 Created + {"id": 12, "eventId": 3, "userId": 7, "createdAt": "..."}
     * Errors (from the service): 404 NOT_FOUND, 409 ALREADY_REGISTERED, 409 EVENT_FULL
     * Errors (from AuthInterceptor, before we even get here): 401 no/bad token
     */
    @PostMapping("/events/{id}/register") // {id} is a placeholder for the event id in the URL
    public ResponseEntity<Registration> register(
            // Pull {id} out of the URL: "/events/3/register" -> id = 3.
            // If it isn't a number ("/events/abc/register"), ErrorHandler returns 400.
            @PathVariable long id,
            // The user AuthInterceptor attached. It comes from the TOKEN, never from the
            // request body, so nobody can register someone else.
            @RequestAttribute(AuthInterceptor.CURRENT_USER) CurrentUser user) {

        Registration registration = service.register(id, user); // the service applies the rules

        // 201 Created (not the default 200), because we created a new registration.
        return ResponseEntity.status(HttpStatus.CREATED).body(registration);
    }

    /**
     * GET /users/{userId}/events/registered?page=1&pageSize=20
     * The events {userId} registered for. Used by our "My events" view and by
     * Subsystem 2's calendar.
     *
     * Success: 200 + {"items": [...], "page": 1, "pageSize": 20, "totalCount": 2}
     * Errors: 403 (a student asking for someone else's list), 400 (bad page/pageSize)
     */
    @GetMapping("/users/{userId}/events/registered")
    public PagedResponse<RegisteredEvent> registeredEvents( // returning the object directly = 200 OK
            @PathVariable long userId,                         // whose list: the 7 in /users/7/...
            @RequestParam(defaultValue = "1") int page,        // page=   (1 if missing)
            @RequestParam(defaultValue = "20") int pageSize,   // pageSize= (20 if missing)
            @RequestAttribute(AuthInterceptor.CURRENT_USER) CurrentUser user) { // who is asking

        return service.registeredEvents(userId, user, page, pageSize);
    }
}
