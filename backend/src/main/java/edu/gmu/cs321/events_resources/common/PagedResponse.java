package edu.gmu.cs321.events_resources.common;

// Java's built-in ordered collection. "items" is a List of whatever this page holds.
import java.util.List;

/**
 * PagedResponse: the standard shape of EVERY list response.
 *
 * SOW §6 requires pagination on all list endpoints: never return an unbounded list,
 * max 50 per page, and include the total count. Every list endpoint returns this:
 *   {"items": [...], "page": 1, "pageSize": 20, "totalCount": 57}
 *
 * <T> IS A "TYPE PARAMETER": it means "a list of SOMETHING", filled in when used:
 *   PagedResponse<RegisteredEvent>  = a page of RegisteredEvents
 *   PagedResponse<Event>            = a page of Events (for GET /events later)
 * One class, reused for every list.
 *
 * NOTE FOR MERGING: whoever builds GET /events needs this same shape. Share this one class.
 */
public record PagedResponse<T>(
        List<T> items,   // the rows on THIS page
        int page,        // which page this is, starting at 1
        int pageSize,    // how many rows per page were asked for
        long totalCount  // how many rows exist in total, across ALL pages
) {

    // "static final" = constants shared by everyone, never changed.
    public static final int DEFAULT_PAGE_SIZE = 20; // used when the URL doesn't say ?pageSize=
    public static final int MAX_PAGE_SIZE = 50;     // SOW §6: never more than 50 per page

    /**
     * Checks page and pageSize from the URL. Throws 400 VALIDATION_ERROR if they're out of range.
     * Call this at the start of any list endpoint, before running the query.
     *
     * "static" = called on the class itself: PagedResponse.validate(page, pageSize)
     */
    public static void validate(int page, int pageSize) {
        if (page < 1) { // pages start at 1, so 0 or negative makes no sense
            throw ApiException.badRequest("Invalid page", "page: must be 1 or more");
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) { // between 1 and 50
            throw ApiException.badRequest("Invalid pageSize", "pageSize: must be between 1 and " + MAX_PAGE_SIZE);
        }
    }

    /**
     * How many rows to skip in SQL (the OFFSET) to reach this page.
     *   page 1, size 20 -> skip 0   (rows 1-20)
     *   page 2, size 20 -> skip 20  (rows 21-40)
     *   page 3, size 20 -> skip 40  (rows 41-60)
     */
    public static int offset(int page, int pageSize) {
        return (page - 1) * pageSize;
    }
}
