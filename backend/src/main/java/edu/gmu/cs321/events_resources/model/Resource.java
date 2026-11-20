package edu.gmu.cs321.events_resources.model;

import java.util.List;
import java.util.UUID;

/**
 * Resource (MASON-7 / MASON-11 / MASON-12): one career resource, as the API sends it.
 *
 *   {"id": 4, "title": "Tech resume checklist", "description": "...",
 *    "type": "Resume & Cover Letter", "industry": "Technology", "link": "https://...",
 *    "majors": ["CS Majors"], "sharedBy": "Student submission", "status": "pending",
 *    "userId": "00000000-...-0003", "reviewNote": null,
 *    "submittedAt": "2026-11-19T10:15:00", "reviewedAt": null}
 *
 * The same shape is used everywhere, but ResourceService blanks out userId and
 * reviewNote in the public directory (GET /resources): only the student who shared it
 * and staff/admin see who submitted it and why it was denied.
 *
 * This record is only for answers. What a student SENDS is a ResourceRequest, so a
 * student can never set status, sharedBy or userId themselves.
 */
public record Resource(
        Long id,
        String title,
        String description,
        String type,
        String industry,
        String link,
        List<String> majors,
        String sharedBy,
        String status,
        UUID userId,
        String reviewNote,
        String submittedAt,
        String reviewedAt) {

    /** The three values of the status column. */
    public static final String PENDING = "pending";
    public static final String APPROVED = "approved";
    public static final String REJECTED = "rejected";

    /** What "sharedBy" says: staff posts and student posts look different on the card. */
    public static final String SHARED_BY_STAFF = "Career Services";
    public static final String SHARED_BY_STUDENT = "Student submission";

    /** A copy without the private fields, for the public directory. */
    public Resource withoutPrivateFields() {
        return new Resource(id, title, description, type, industry, link, majors, sharedBy,
                status, null, null, submittedAt, reviewedAt);
    }
}
