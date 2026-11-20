package edu.gmu.cs321.events_resources.model;

/**
 * ResourceReview (MASON-11): the body of PUT /resources/{id}/status, sent by staff or admin.
 *
 *   {"status": "approved"}
 *   {"status": "rejected", "note": "Please link to the original article instead of a copy."}
 *   {"status": "pending",  "note": "On hold until we check the link works."}
 *
 * status = approved (post it), rejected (deny it) or pending (put it back on hold).
 * note   = optional message the student sees in "My submissions" (max 500 characters).
 */
public record ResourceReview(String status, String note) {
}
