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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.gmu.cs321.events_resources.common.AuthInterceptor;
import edu.gmu.cs321.events_resources.common.PagedResponse;
import edu.gmu.cs321.events_resources.model.CurrentUser;
import edu.gmu.cs321.events_resources.model.Resource;
import edu.gmu.cs321.events_resources.model.ResourceRequest;
import edu.gmu.cs321.events_resources.model.ResourceReview;
import edu.gmu.cs321.events_resources.service.ResourceService;

/**
 * ResourceController: the API layer for career resources (MASON-7, MASON-11, MASON-12).
 *
 *   Controller (this file)  ->  ResourceService (rules)  ->  ResourceRepository (SQL)  ->  PostgreSQL
 *
 * Same job as the other controllers: turn HTTP into a service call and back. No rules, no SQL.
 * AuthInterceptor has already checked the token (and staff role where needed).
 *
 * ENDPOINTS
 *   GET    /resources                 approved resources, public                     -> 200 page
 *   GET    /resources?status=pending  the review queue, staff/admin                  -> 200 page
 *   POST   /resources                 share a resource, logged in                    -> 201 Resource
 *   PUT    /resources/{id}            edit your own, only while pending              -> 200 Resource
 *   DELETE /resources/{id}            delete your own, only while pending            -> 204
 *   PUT    /resources/{id}/status     approve / deny / put on hold, staff/admin      -> 200 Resource
 *   GET    /users/me/resources        what I shared, every status, logged in         -> 200 page
 */
@RestController
public class ResourceController {

    private final ResourceService service;

    public ResourceController(ResourceService service) {
        this.service = service;
    }

    /**
     * GET /resources?page=&pageSize=&q=&major=&type=&status=
     * required = false on the user: guests have no token, so nothing is attached.
     */
    @GetMapping("/resources")
    public PagedResponse<Resource> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String major,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestAttribute(value = AuthInterceptor.CURRENT_USER, required = false) CurrentUser user) {
        return service.list(page, pageSize, q, major, type, status, user);
    }

    /**
     * POST /resources
     * Errors: 400 VALIDATION_ERROR / INVALID_JSON, 401
     */
    @PostMapping("/resources")
    public ResponseEntity<Resource> submit(
            @RequestBody ResourceRequest body,
            @RequestAttribute(AuthInterceptor.CURRENT_USER) CurrentUser user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.submit(body, user));
    }

    /**
     * PUT /resources/{id}
     * Errors: 400, 401, 403 FORBIDDEN (not yours), 404 NOT_FOUND, 409 NOT_PENDING (already reviewed)
     */
    @PutMapping("/resources/{id}")
    public Resource update(
            @PathVariable long id,
            @RequestBody ResourceRequest body,
            @RequestAttribute(AuthInterceptor.CURRENT_USER) CurrentUser user) {
        return service.update(id, body, user);
    }

    /**
     * DELETE /resources/{id}
     * Errors: 401, 403 FORBIDDEN, 404 NOT_FOUND, 409 NOT_PENDING
     */
    @DeleteMapping("/resources/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable long id,
            @RequestAttribute(AuthInterceptor.CURRENT_USER) CurrentUser user) {
        service.delete(id, user);
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /resources/{id}/status   body {"status": "approved" | "rejected" | "pending", "note": "..."}
     * Errors: 400, 401, 403 FORBIDDEN (a student), 404 NOT_FOUND
     */
    @PutMapping("/resources/{id}/status")
    public Resource review(
            @PathVariable long id,
            @RequestBody ResourceReview body,
            @RequestAttribute(AuthInterceptor.CURRENT_USER) CurrentUser user) {
        return service.review(id, body, user);
    }

    /** GET /users/me/resources?page=&pageSize=  (the "My submissions" list) */
    @GetMapping("/users/me/resources")
    public PagedResponse<Resource> mine(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestAttribute(AuthInterceptor.CURRENT_USER) CurrentUser user) {
        return service.mine(page, pageSize, user);
    }
}
