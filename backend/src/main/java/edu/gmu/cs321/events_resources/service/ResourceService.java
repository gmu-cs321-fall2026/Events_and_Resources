package edu.gmu.cs321.events_resources.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.gmu.cs321.events_resources.common.ApiException;
import edu.gmu.cs321.events_resources.common.PagedResponse;
import edu.gmu.cs321.events_resources.model.CurrentUser;
import edu.gmu.cs321.events_resources.model.Resource;
import edu.gmu.cs321.events_resources.model.ResourceRequest;
import edu.gmu.cs321.events_resources.model.ResourceReview;
import edu.gmu.cs321.events_resources.repository.ResourceRepository;

/**
 * ResourceService: the rules for career resources.
 *
 *   ResourceController  ->  ResourceService (rules, this file)  ->  ResourceRepository (SQL)
 *
 * MASON-7  Submit a resource for review (student)
 *   - A student's resource starts as "pending" and is NOT in the public directory yet.
 *   - The student sees it, with its status, in GET /users/me/resources.
 *   - While it's pending they can edit or delete it. Once it's approved or rejected it's locked
 *     (409 NOT_PENDING).
 *   - Staff/admin are Career Services: what they share is approved right away.
 *
 * MASON-11 Approve resources from students (staff/admin)
 *   - GET /resources?status=pending lists the review queue, oldest first.
 *   - PUT /resources/{id}/status approves, denies ("rejected") or puts it back on hold
 *     ("pending"), with an optional note the student can read.
 *
 * MASON-12 Browse resources directory: GET /resources shows approved resources only.
 *
 * Every bad field is listed at once in "detail", e.g.
 * "title: required; link: must start with http:// or https://".
 * Who may call what (logged in, staff) is checked earlier, in AuthInterceptor.
 */
@Service
public class ResourceService {

    /** The allowed values for "type" (exact match). Same list as RESOURCE_TYPES on the website. */
    public static final List<String> TYPES = List.of(
            "Resume & Cover Letter",
            "Interview Questions",
            "Salary Negotiation",
            "Elevator Pitch",
            "Application Tips",
            "Practice Tools");

    public static final List<String> STATUSES = List.of(Resource.PENDING, Resource.APPROVED, Resource.REJECTED);

    static final int MAX_KEYWORD_LENGTH = 100;
    static final int MAX_MAJORS = 12;
    static final int MAX_NOTE_LENGTH = 500;
    static final String DEFAULT_INDUSTRY = "All industries";

    private final ResourceRepository repository;

    public ResourceService(ResourceRepository repository) {
        this.repository = repository;
    }

    // -----------------------------------------------------------------------
    // READING
    // -----------------------------------------------------------------------

    /**
     * GET /resources: one page of resources.
     *
     * @param status null or "approved" = the public directory (anyone).
     *               "pending" / "rejected" = the review queue (staff/admin only).
     * @param viewer the logged-in user, or null for a guest
     */
    public PagedResponse<Resource> list(int page, int pageSize, String q, String major, String type,
            String status, CurrentUser viewer) {
        PagedResponse.validate(page, pageSize);

        List<String> problems = new ArrayList<>();
        String keyword = blankToNull(q);
        if (keyword != null && keyword.length() > MAX_KEYWORD_LENGTH) {
            problems.add("q: must be " + MAX_KEYWORD_LENGTH + " characters or fewer");
        }
        String typeFilter = blankToNull(type);
        if (typeFilter != null && !TYPES.contains(typeFilter)) {
            problems.add("type: must be one of " + String.join(", ", TYPES));
        }
        String statusFilter = blankToNull(status) == null ? Resource.APPROVED : status.trim().toLowerCase();
        if (!STATUSES.contains(statusFilter)) {
            problems.add("status: must be one of " + String.join(", ", STATUSES));
        }
        throwIfAny("Invalid filter", problems);

        boolean publicList = statusFilter.equals(Resource.APPROVED);
        if (!publicList && (viewer == null || !viewer.isStaff())) {
            throw ApiException.forbidden("Only Career Services staff or admin can see resources that aren't approved");
        }

        ResourceRepository.Filter filter = new ResourceRepository.Filter(
                keyword, blankToNull(major), typeFilter, statusFilter, null, !publicList);

        List<Resource> items = repository.findPage(filter, pageSize, PagedResponse.offset(page, pageSize));
        boolean showPrivate = viewer != null && viewer.isStaff();
        if (!showPrivate) {
            items = items.stream().map(Resource::withoutPrivateFields).toList();
        }
        return new PagedResponse<>(items, page, pageSize, repository.count(filter));
    }

    /** GET /users/me/resources: everything this user shared, any status, newest first. */
    public PagedResponse<Resource> mine(int page, int pageSize, CurrentUser user) {
        PagedResponse.validate(page, pageSize);
        ResourceRepository.Filter filter = new ResourceRepository.Filter(null, null, null, null, user.id(), false);
        List<Resource> items = repository.findPage(filter, pageSize, PagedResponse.offset(page, pageSize));
        return new PagedResponse<>(items, page, pageSize, repository.count(filter));
    }

    // -----------------------------------------------------------------------
    // MASON-7: SUBMIT, EDIT, DELETE (the person who shared it)
    // -----------------------------------------------------------------------

    /**
     * POST /resources. Students -> "pending" and "Student submission".
     * Staff/admin -> "approved" and "Career Services" (they maintain the directory).
     * Saving the resource and its audiences is one transaction: both or neither.
     */
    @Transactional
    public Resource submit(ResourceRequest input, CurrentUser user) {
        List<String> problems = new ArrayList<>();
        ResourceRequest clean = clean(input, problems);
        Map<String, Long> audienceIds = audienceIds(clean, problems);
        throwIfAny("Some fields are invalid", problems);

        boolean staff = user.isStaff();
        String status = staff ? Resource.APPROVED : Resource.PENDING;
        String sharedBy = staff ? Resource.SHARED_BY_STAFF : Resource.SHARED_BY_STUDENT;

        long id = repository.insert(clean, user.id(), sharedBy, status);
        repository.replaceAudiences(id, audienceIds.values());
        return load(id);
    }

    /**
     * PUT /resources/{id}: replaces the fields, only for the person who shared it and only
     * while it's pending. Same checks as submit.
     */
    @Transactional
    public Resource update(long id, ResourceRequest input, CurrentUser user) {
        List<String> problems = new ArrayList<>();
        ResourceRequest clean = clean(input, problems);
        Map<String, Long> audienceIds = audienceIds(clean, problems);
        throwIfAny("Some fields are invalid", problems);

        if (!repository.updateIfPending(id, user.id(), clean)) {
            throw whyLocked(id, user, "edited");
        }
        repository.replaceAudiences(id, audienceIds.values());
        return load(id);
    }

    /** DELETE /resources/{id}: only the person who shared it, only while it's pending. */
    public void delete(long id, CurrentUser user) {
        if (!repository.deleteIfPending(id, user.id())) {
            throw whyLocked(id, user, "deleted");
        }
    }

    // -----------------------------------------------------------------------
    // MASON-11: REVIEW (staff/admin)
    // -----------------------------------------------------------------------

    /**
     * PUT /resources/{id}/status: approve, deny or put on hold.
     * Any status can be changed again later, e.g. an approved resource whose link broke
     * can be put back on hold (the student can then fix it) or denied.
     */
    public Resource review(long id, ResourceReview input, CurrentUser reviewer) {
        List<String> problems = new ArrayList<>();
        String status = input == null ? null : blankToNull(input.status());
        if (status == null) {
            problems.add("status: required");
        } else {
            status = status.toLowerCase();
            if (!STATUSES.contains(status)) {
                problems.add("status: must be one of " + String.join(", ", STATUSES));
            }
        }
        String note = input == null ? null : blankToNull(input.note());
        if (note != null && note.length() > MAX_NOTE_LENGTH) {
            problems.add("note: must be " + MAX_NOTE_LENGTH + " characters or fewer");
        }
        throwIfAny("Some fields are invalid", problems);

        if (!repository.updateStatus(id, status, reviewer.id(), note)) {
            throw notFound(id);
        }
        return load(id);
    }

    // -----------------------------------------------------------------------
    // CHECKS AND HELPERS
    // -----------------------------------------------------------------------

    /**
     * Checks the text fields and returns a trimmed copy. Problems are added to the list;
     * nothing is thrown here, so every problem can be reported at once.
     */
    private ResourceRequest clean(ResourceRequest r, List<String> problems) {
        if (r == null) {
            problems.add("body: required");
            return new ResourceRequest(null, null, null, null, null, List.of());
        }
        String title = requiredText(r.title(), "title", 200, problems);
        String description = requiredText(r.description(), "description", 2000, problems);

        String type = blankToNull(r.type());
        if (type == null) {
            problems.add("type: required");
        } else if (!TYPES.contains(type)) {
            problems.add("type: must be one of " + String.join(", ", TYPES));
        }

        String industry = blankToNull(r.industry());
        if (industry == null) {
            industry = DEFAULT_INDUSTRY;
        } else if (industry.length() > 100) {
            problems.add("industry: must be 100 characters or fewer");
        }

        String link = blankToNull(r.link());
        if (link != null) {
            if (link.length() > 500) {
                problems.add("link: must be 500 characters or fewer");
            }
            if (!(link.startsWith("http://") || link.startsWith("https://"))) {
                problems.add("link: must start with http:// or https://");
            }
        }

        Set<String> majors = new LinkedHashSet<>();
        if (r.majors() != null) {
            r.majors().stream().map(this::blankToNull).filter(m -> m != null).forEach(majors::add);
        }
        if (majors.isEmpty()) {
            problems.add("majors: pick at least one (use \"All Students\" for everyone)");
        } else if (majors.size() > MAX_MAJORS) {
            problems.add("majors: at most " + MAX_MAJORS);
        }

        return new ResourceRequest(title, description, type, industry, link, List.copyOf(majors));
    }

    /** Turns audience names into ids, and reports any name that isn't a real audience. */
    private Map<String, Long> audienceIds(ResourceRequest clean, List<String> problems) {
        if (clean.majors().isEmpty()) {
            return Map.of();
        }
        Map<String, Long> ids = repository.findAudienceIds(clean.majors());
        for (String name : clean.majors()) {
            if (!ids.containsKey(name)) {
                problems.add("majors: \"" + name + "\" isn't a known audience");
            }
        }
        return ids;
    }

    /**
     * A pending-only change touched no row. Finds out why, in this order:
     * no such resource (404), someone else's (403), already reviewed (409 NOT_PENDING).
     */
    private ApiException whyLocked(long id, CurrentUser user, String pastTense) {
        Resource existing = repository.findById(id).orElse(null);
        if (existing == null) {
            return notFound(id);
        }
        if (!user.id().equals(existing.userId())) {
            return ApiException.forbidden("You can only change resources you shared");
        }
        return ApiException.conflict("NOT_PENDING",
                "This resource is already " + existing.status() + ", so it can't be " + pastTense + " anymore");
    }

    private Resource load(long id) {
        return repository.findById(id).orElseThrow(() -> notFound(id));
    }

    private String requiredText(String value, String field, int maxLength, List<String> problems) {
        String trimmed = blankToNull(value);
        if (trimmed == null) {
            problems.add(field + ": required");
        } else if (trimmed.length() > maxLength) {
            problems.add(field + ": must be " + maxLength + " characters or fewer");
        }
        return trimmed;
    }

    private void throwIfAny(String message, List<String> problems) {
        if (!problems.isEmpty()) {
            throw ApiException.badRequest(message, String.join("; ", problems));
        }
    }

    private ApiException notFound(long id) {
        return ApiException.notFound("Resource " + id + " not found");
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
