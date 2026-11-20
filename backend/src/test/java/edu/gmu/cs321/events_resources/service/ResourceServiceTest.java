package edu.gmu.cs321.events_resources.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import edu.gmu.cs321.events_resources.common.ApiException;
import edu.gmu.cs321.events_resources.common.PagedResponse;
import edu.gmu.cs321.events_resources.integration.FakeTokenValidator;
import edu.gmu.cs321.events_resources.model.CurrentUser;
import edu.gmu.cs321.events_resources.model.Resource;
import edu.gmu.cs321.events_resources.model.ResourceRequest;
import edu.gmu.cs321.events_resources.model.ResourceReview;
import edu.gmu.cs321.events_resources.repository.ResourceRepository;

/**
 * Unit tests for ResourceService: MASON-7 (submit a resource for review) and
 * MASON-11 (approve resources from students).
 *
 * Like OpportunityServiceTest: no Spring and no database. FakeRepository keeps
 * resources in a HashMap and follows the same "only while pending" rule as the real SQL.
 */
class ResourceServiceTest {

    private static final CurrentUser STUDENT = new CurrentUser(FakeTokenValidator.STUDENT_ID, List.of(CurrentUser.STUDENT));
    private static final CurrentUser OTHER_STUDENT = new CurrentUser(FakeTokenValidator.STUDENT2_ID, List.of(CurrentUser.STUDENT));
    private static final CurrentUser STAFF = new CurrentUser(FakeTokenValidator.STAFF_ID, List.of(CurrentUser.STAFF));
    private static final CurrentUser ADMIN = new CurrentUser(FakeTokenValidator.ADMIN_ID, List.of(CurrentUser.ADMIN));

    private static final ResourceRequest GOOD = new ResourceRequest(
            "Questions from the fintech fair", "Five questions recruiters asked me.",
            "Interview Questions", "Finance", "https://example.com/notes", List.of("CS Majors", "Juniors"));

    /** A repository that lives in memory. */
    static class FakeRepository extends ResourceRepository {
        final Map<Long, Resource> rows = new HashMap<>();
        Filter lastFilter;
        long nextId = 1;

        FakeRepository() {
            super(null);
        }

        @Override
        public List<Resource> findPage(Filter filter, int limit, int offset) {
            lastFilter = filter;
            return rows.values().stream()
                    .filter(r -> filter.status() == null || filter.status().equals(r.status()))
                    .filter(r -> filter.userId() == null || filter.userId().equals(r.userId()))
                    .toList();
        }

        @Override
        public long count(Filter filter) {
            return findPage(filter, 50, 0).size();
        }

        @Override
        public Optional<Resource> findById(long id) {
            return Optional.ofNullable(rows.get(id));
        }

        @Override
        public Map<String, Long> findAudienceIds(Collection<String> names) {
            Map<String, Long> ids = new HashMap<>();
            List<String> known = List.of("All Students", "CS Majors", "IT Majors", "Juniors");
            names.stream().filter(known::contains).forEach(n -> ids.put(n, (long) known.indexOf(n) + 1));
            return ids;
        }

        @Override
        public long insert(ResourceRequest r, UUID userId, String owner, String status) {
            long id = nextId++;
            rows.put(id, new Resource(id, r.title(), r.description(), r.type(), r.industry(), r.link(),
                    r.majors(), owner, status, userId, null, "2026-11-19T10:00:00", null));
            return id;
        }

        @Override
        public boolean updateIfPending(long id, UUID userId, ResourceRequest r) {
            Resource old = rows.get(id);
            if (old == null || !old.userId().equals(userId) || !old.status().equals(Resource.PENDING)) {
                return false;
            }
            rows.put(id, new Resource(id, r.title(), r.description(), r.type(), r.industry(), r.link(),
                    r.majors(), old.sharedBy(), old.status(), userId, old.reviewNote(), old.submittedAt(), old.reviewedAt()));
            return true;
        }

        @Override
        public boolean deleteIfPending(long id, UUID userId) {
            Resource old = rows.get(id);
            if (old == null || !old.userId().equals(userId) || !old.status().equals(Resource.PENDING)) {
                return false;
            }
            rows.remove(id);
            return true;
        }

        @Override
        public boolean updateStatus(long id, String status, UUID reviewerId, String note) {
            Resource old = rows.get(id);
            if (old == null) {
                return false;
            }
            rows.put(id, new Resource(id, old.title(), old.description(), old.type(), old.industry(), old.link(),
                    old.majors(), old.sharedBy(), status, old.userId(), note, old.submittedAt(), "2026-11-20T09:00:00"));
            return true;
        }

        @Override
        public void replaceAudiences(long resourceId, Collection<Long> audienceIds) {
            // Audience names are already stored on the fake row by insert/update.
        }
    }

    private final FakeRepository repository = new FakeRepository();
    private final ResourceService service = new ResourceService(repository);

    private ApiException error(Runnable call) {
        return assertThrows(ApiException.class, call::run);
    }

    // ---- MASON-7: submit ----

    @Test
    void aStudentSubmissionStartsPendingAndIsMarkedAsAStudentSubmission() {
        Resource saved = service.submit(GOOD, STUDENT);

        assertEquals(Resource.PENDING, saved.status());
        assertEquals(Resource.SHARED_BY_STUDENT, saved.sharedBy());
        assertEquals(STUDENT.id(), saved.userId());
        assertEquals(List.of("CS Majors", "Juniors"), saved.majors());
    }

    @Test
    void staffAndAdminPostsAreApprovedRightAway() {
        assertEquals(Resource.APPROVED, service.submit(GOOD, STAFF).status());
        Resource fromAdmin = service.submit(GOOD, ADMIN);
        assertEquals(Resource.APPROVED, fromAdmin.status());
        assertEquals(Resource.SHARED_BY_STAFF, fromAdmin.sharedBy());
    }

    @Test
    void everyBadFieldIsReportedAtOnce() {
        ResourceRequest bad = new ResourceRequest(" ", "", "Gossip", null, "javascript:alert(1)", List.of("Martians"));

        ApiException e = error(() -> service.submit(bad, STUDENT));

        assertEquals("VALIDATION_ERROR", e.getCode());
        assertTrue(e.getDetail().contains("title: required"));
        assertTrue(e.getDetail().contains("description: required"));
        assertTrue(e.getDetail().contains("type: must be one of"));
        assertTrue(e.getDetail().contains("link: must start with http:// or https://"));
        assertTrue(e.getDetail().contains("\"Martians\" isn't a known audience"));
        assertTrue(repository.rows.isEmpty());
    }

    @Test
    void linkAndIndustryAreOptional() {
        ResourceRequest tipOnly = new ResourceRequest("Bring printed resumes", "Recruiters still take paper.",
                "Application Tips", "", null, List.of("All Students"));

        Resource saved = service.submit(tipOnly, STUDENT);

        assertNull(saved.link());
        assertEquals("All industries", saved.industry());
    }

    @Test
    void atLeastOneMajorIsRequired() {
        ResourceRequest noMajors = new ResourceRequest("t", "d", "Practice Tools", null, null, List.of());

        assertTrue(error(() -> service.submit(noMajors, STUDENT)).getDetail().contains("majors: pick at least one"));
    }

    // ---- MASON-7: see my submissions, edit and delete only while pending ----

    @Test
    void myListShowsOnlyMyOwnSubmissionsWithTheirStatus() {
        service.submit(GOOD, STUDENT);
        service.submit(GOOD, OTHER_STUDENT);

        PagedResponse<Resource> mine = service.mine(1, 20, STUDENT);

        assertEquals(1, mine.totalCount());
        assertEquals(Resource.PENDING, mine.items().get(0).status());
        assertEquals(STUDENT.id(), repository.lastFilter.userId());
    }

    @Test
    void aPendingSubmissionCanBeEditedAndDeleted() {
        long id = service.submit(GOOD, STUDENT).id();
        ResourceRequest fixed = new ResourceRequest("Fixed title", GOOD.description(), GOOD.type(),
                GOOD.industry(), GOOD.link(), List.of("All Students"));

        assertEquals("Fixed title", service.update(id, fixed, STUDENT).title());

        service.delete(id, STUDENT);
        assertTrue(repository.rows.isEmpty());
    }

    @Test
    void anApprovedOrRejectedSubmissionIsLocked() {
        long approved = service.submit(GOOD, STUDENT).id();
        long rejected = service.submit(GOOD, STUDENT).id();
        service.review(approved, new ResourceReview("approved", null), STAFF);
        service.review(rejected, new ResourceReview("rejected", "Broken link"), STAFF);

        assertEquals("NOT_PENDING", error(() -> service.update(approved, GOOD, STUDENT)).getCode());
        assertEquals("NOT_PENDING", error(() -> service.delete(approved, STUDENT)).getCode());
        assertEquals("NOT_PENDING", error(() -> service.update(rejected, GOOD, STUDENT)).getCode());
        assertEquals(409, error(() -> service.delete(rejected, STUDENT)).getStatus().value());
    }

    @Test
    void someoneElsesSubmissionIs403AndAMissingOneIs404() {
        long id = service.submit(GOOD, STUDENT).id();

        assertEquals("FORBIDDEN", error(() -> service.update(id, GOOD, OTHER_STUDENT)).getCode());
        assertEquals("FORBIDDEN", error(() -> service.delete(id, OTHER_STUDENT)).getCode());
        assertEquals("NOT_FOUND", error(() -> service.delete(999, STUDENT)).getCode());
    }

    @Test
    void puttingAResourceBackOnHoldUnlocksItForTheStudent() {
        long id = service.submit(GOOD, STUDENT).id();
        service.review(id, new ResourceReview("approved", null), STAFF);
        service.review(id, new ResourceReview("pending", "Please fix the link"), ADMIN);

        Resource edited = service.update(id, GOOD, STUDENT);
        assertEquals(Resource.PENDING, edited.status());
        assertEquals("Please fix the link", edited.reviewNote());
    }

    // ---- MASON-11: review ----

    @Test
    void staffCanApproveOrDenyWithANote() {
        long id = service.submit(GOOD, STUDENT).id();

        Resource denied = service.review(id, new ResourceReview("Rejected", "Duplicate of another resource"), STAFF);

        assertEquals(Resource.REJECTED, denied.status());
        assertEquals("Duplicate of another resource", denied.reviewNote());
        assertNotNull(denied.reviewedAt());
    }

    @Test
    void reviewNeedsAKnownStatusAndAnExistingResource() {
        long id = service.submit(GOOD, STUDENT).id();

        assertTrue(error(() -> service.review(id, new ResourceReview(null, null), STAFF)).getDetail().contains("status: required"));
        assertTrue(error(() -> service.review(id, new ResourceReview("maybe", null), STAFF)).getDetail().contains("status: must be one of"));
        assertTrue(error(() -> service.review(id, new ResourceReview("approved", "x".repeat(501)), STAFF)).getDetail().contains("note:"));
        assertEquals("NOT_FOUND", error(() -> service.review(999, new ResourceReview("approved", null), STAFF)).getCode());
    }

    // ---- listing ----

    @Test
    void thePublicDirectoryOnlyShowsApprovedAndHidesWhoSubmitted() {
        long pending = service.submit(GOOD, STUDENT).id();
        long approved = service.submit(GOOD, STUDENT).id();
        service.review(approved, new ResourceReview("approved", "Nice"), STAFF);

        PagedResponse<Resource> page = service.list(1, 20, null, null, null, null, null);

        assertEquals(1, page.items().size());
        assertEquals(approved, (long) page.items().get(0).id());
        assertNull(page.items().get(0).userId());
        assertNull(page.items().get(0).reviewNote());
        assertTrue(repository.rows.containsKey(pending));
    }

    @Test
    void theReviewQueueIsOldestFirstAndForStaffOnly() {
        service.submit(GOOD, STUDENT);

        PagedResponse<Resource> queue = service.list(1, 20, null, null, null, "pending", STAFF);
        assertEquals(1, queue.items().size());
        assertNotNull(queue.items().get(0).userId());
        assertTrue(repository.lastFilter.oldestFirst());

        assertEquals("FORBIDDEN", error(() -> service.list(1, 20, null, null, null, "pending", STUDENT)).getCode());
        assertEquals("FORBIDDEN", error(() -> service.list(1, 20, null, null, null, "rejected", null)).getCode());
    }

    @Test
    void badFiltersAre400() {
        ApiException e = error(() -> service.list(1, 20, "x".repeat(101), null, "Gossip", "lost", STAFF));

        assertTrue(e.getDetail().contains("q:"));
        assertTrue(e.getDetail().contains("type:"));
        assertTrue(e.getDetail().contains("status:"));
        assertEquals("VALIDATION_ERROR", error(() -> service.list(0, 20, null, null, null, null, null)).getCode());
    }
}
