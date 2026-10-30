package edu.gmu.cs321.events_resources.model;
/* Record is basically a shorthand for a final class with automatic initialization for all the variables and automatic getters for all of them */
import java.util.List;   // a user can have several roles, e.g. ["student", "mentor"]
import java.util.UUID;   // Shared Core's user ids are UUIDs

// Matches Shared Core's /auth/validate: {"userId": "<UUID>", "roles": ["student"], ...}
public record CurrentUser(UUID id, List<String> roles) {

    // Shared Core's role names, lowercase, exactly as /auth/validate sends them
    public static final String STUDENT = "student";
    public static final String MENTOR = "mentor";
    public static final String STAFF = "staff";
    public static final String ADMIN = "admin";

    // Runs on every new CurrentUser: unchangeable copy, so roles can't be added after the token check
    public CurrentUser {
        roles = List.copyOf(roles);
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    // Staff OR admin may create/edit/delete events
    public boolean isStaff() {
        return hasRole(STAFF) || hasRole(ADMIN);
    }
}