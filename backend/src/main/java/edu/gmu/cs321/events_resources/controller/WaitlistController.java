package edu.gmu.cs321.events_resources.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import edu.gmu.cs321.events_resources.common.AuthInterceptor;
import edu.gmu.cs321.events_resources.model.CapacityRequest;
import edu.gmu.cs321.events_resources.model.CurrentUser;
import edu.gmu.cs321.events_resources.model.RegistrationStatus;
import edu.gmu.cs321.events_resources.model.WaitlistView;
import edu.gmu.cs321.events_resources.service.WaitlistService;

/**
 * WaitlistController: the API layer for the waitlist (MASON-18 / MASON-19).
 *
 *   Controller (this file)  ->  WaitlistService (rules)  ->  WaitlistRepository (SQL)  ->  PostgreSQL
 *
 * Same job as RegistrationController: turn HTTP into a service call and back. No rules, no SQL.
 * AuthInterceptor has already checked the token (and staff role where needed) before we get here.
 *
 * ENDPOINTS
 *   POST   /events/{id}/waitlist   join the waitlist (logged-in user)        -> 201 + RegistrationStatus
 *   DELETE /events/{id}/waitlist   leave the waitlist (logged-in user)       -> 204
 *   GET    /events/{id}/waitlist   see the waitlist (staff/admin)            -> 200 + WaitlistView
 *   PUT    /events/{id}/capacity   change capacity, fill seats (staff/admin) -> 200 + WaitlistView
 *
 * A student sees their own place in line through the existing GET /events/{id}/register.
 */
@RestController
public class WaitlistController {

    private final WaitlistService service;

    public WaitlistController(WaitlistService service) {
        this.service = service;
    }

    /**
     * POST /events/{id}/waitlist
     * Success: 201 + {"eventId": 3, "registered": false, "registeredAt": null, "waitlisted": true, "waitlistPosition": 1}
     * Errors: 404 NOT_FOUND, 409 ALREADY_REGISTERED, 409 ALREADY_WAITLISTED, 409 SPOTS_AVAILABLE, 401
     */
    @PostMapping("/events/{id}/waitlist")
    public ResponseEntity<RegistrationStatus> join(
            @PathVariable long id,
            @RequestAttribute(AuthInterceptor.CURRENT_USER) CurrentUser user) {

        return ResponseEntity.status(HttpStatus.CREATED).body(service.join(id, user));
    }

    /**
     * DELETE /events/{id}/waitlist
     * Success: 204 No Content
     * Errors: 404 NOT_FOUND, 404 NOT_WAITLISTED, 401
     */
    @DeleteMapping("/events/{id}/waitlist")
    public ResponseEntity<Void> leave(
            @PathVariable long id,
            @RequestAttribute(AuthInterceptor.CURRENT_USER) CurrentUser user) {

        service.leave(id, user);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /events/{id}/waitlist  (staff/admin)
     * Success: 200 + {"eventId": 3, "capacity": 30, "registeredCount": 30,
     *                 "waitlist": [{"position": 1, "userId": "...", "joinedAt": "..."}], "justAccepted": []}
     * Errors: 404 NOT_FOUND, 403 FORBIDDEN, 401
     */
    @GetMapping("/events/{id}/waitlist")
    public WaitlistView view(
            @PathVariable long id,
            @RequestAttribute(AuthInterceptor.CURRENT_USER) CurrentUser user) {

        return service.view(id, user);
    }

    /**
     * PUT /events/{id}/capacity  (staff/admin)   body: {"capacity": 35}
     * Success: 200 + WaitlistView; "justAccepted" lists who moved from the waitlist into a seat.
     * Errors: 400 VALIDATION_ERROR (missing/negative, or below registeredCount), 404 NOT_FOUND, 403, 401
     */
    @PutMapping("/events/{id}/capacity")
    public WaitlistView changeCapacity(
            @PathVariable long id,
            @RequestBody(required = false) CapacityRequest body,
            @RequestAttribute(AuthInterceptor.CURRENT_USER) CurrentUser user) {

        return service.changeCapacity(id, body, user);
    }
}
