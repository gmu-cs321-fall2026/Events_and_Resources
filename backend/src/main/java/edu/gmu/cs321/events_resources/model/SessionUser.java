package edu.gmu.cs321.events_resources.model;

import java.util.List;
import java.util.UUID;

/**
 * SessionUser: what GET /session/me sends back, so the website knows who is logged in.
 *
 *   {"userId": "00000000-0000-0000-0000-000000000002", "roles": ["staff"], "staff": true}
 *
 * The website uses "staff" to decide which buttons to SHOW (Create Event, post a job,
 * approve resources). Hiding a button is only a convenience: AuthInterceptor still
 * checks the role on every request.
 */

public record SessionUser(UUID userId, List<String> roles, boolean staff) {
    public static SessionUser from(CurrentUser user) {
        return new SessionUser(user.id(), user.roles(), user.isStaff());
    }
}
