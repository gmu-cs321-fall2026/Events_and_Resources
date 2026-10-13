package edu.gmu.cs321.events_resources.model;
/* Record is basically a shorthand for a final class with automatic initialization for all the variables and automatic getters for all of them */
public record CurrentUser(long id, String role) {

    // Role names
    //can be edited when Shared Core confirms their exact names.
    public static final String STUDENT = "STUDENT";
    public static final String MENTOR = "MENTOR";
    public static final String STAFF = "STAFF";
    public static final String ADMIN = "ADMIN";

    /** True for Career Services Staff, who can create events. */
    public boolean isStaff() {
        return STAFF.equals(role);
    }
}