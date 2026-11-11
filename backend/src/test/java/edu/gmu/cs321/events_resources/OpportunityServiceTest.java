package edu.gmu.cs321.events_resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.gmu.cs321.events_resources.common.ApiException;
import edu.gmu.cs321.events_resources.common.PagedResponse;
import edu.gmu.cs321.events_resources.model.Opportunity;
import edu.gmu.cs321.events_resources.repository.OpportunityRepository;
import edu.gmu.cs321.events_resources.service.OpportunityService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Unit tests for OpportunityService (MASON-13 Post jobs).
 *
 * Like EventServiceTest: no Spring and no database. A fake repository remembers
 * what the service asked it to do, and a fixed clock makes "today" Nov 11,
 * 2026.
 */
class OpportunityServiceTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 11, 11);

	/** A fake repository: never touches a database, just records the last call. */
	static class FakeRepository extends OpportunityRepository {
		Opportunity saved;
		LocalDate savedDeadline;
		Filter lastFilter;
		boolean exists = true; // false = pretend there's no posting with that id

		FakeRepository() {
			super(null);
		}

		@Override
		public Opportunity insert(Opportunity o, LocalDate deadline) {
			saved = o;
			savedDeadline = deadline;
			return withId(o, deadline);
		}

		@Override
		public Optional<Opportunity> update(long id, Opportunity o, LocalDate deadline) {
			saved = o;
			savedDeadline = deadline;
			return exists ? Optional.of(withId(o, deadline)) : Optional.empty();
		}

		@Override
		public boolean delete(long id) {
			return exists;
		}

		@Override
		public Optional<Opportunity> findById(long id) {
			return exists ? Optional.of(withId(valid(), TODAY)) : Optional.empty();
		}

		@Override
		public List<Opportunity> findPage(Filter filter, int limit, int offset) {
			lastFilter = filter;
			return List.of(withId(valid(), TODAY));
		}

		@Override
		public long count(Filter filter) {
			return 1;
		}

		private static Opportunity withId(Opportunity o, LocalDate deadline) {
			return new Opportunity(7L, o.title(), o.employer(), o.type(), o.description(),
					o.link(), deadline.toString(), TODAY.toString());
		}
	}

	private final FakeRepository repository = new FakeRepository();
	private final OpportunityService service = new OpportunityService(
			repository, Clock.fixed(TODAY.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault()));

	/** A posting that passes every check. Tests change one field at a time. */
	private static Opportunity valid() {
		return new Opportunity(null, "Software Intern", "Capital One", "Internship",
				"Build features with a mentor.", "https://example.com/apply", "2026-12-15", null);
	}

	// =======================================================================
	// POST /opportunities
	// =======================================================================

	@Test
	void validPostingIsSavedTrimmedWithoutIdOrDatePosted() {
		Opportunity sent = new Opportunity(99L, "  Software Intern ", "Capital One", "Internship",
				"Build features.", "https://example.com/apply", "2026-12-15", "1999-01-01");

		Opportunity result = service.create(sent);

		assertEquals("Software Intern", repository.saved.title());
		assertNull(repository.saved.id()); // the database picks the id
		assertNull(repository.saved.datePosted()); // and the date posted
		assertEquals(LocalDate.of(2026, 12, 15), repository.savedDeadline);
		assertEquals(Long.valueOf(7), result.id());
	}

	@Test
	void everyMissingFieldIsListedAtOnce() {
		Opportunity empty = new Opportunity(null, "", null, null, " ", null, null, null);

		ApiException e = assertThrows(ApiException.class, () -> service.create(empty));

		assertEquals("VALIDATION_ERROR", e.getCode());
		for (String field : List.of("title", "employer", "type", "description", "link", "deadline")) {
			assertTrue(e.getDetail().contains(field + ": required"), field);
		}
		assertNull(repository.saved); // nothing was saved
	}

	@Test
	void typeMustBeAnAllowedValue() {
		Opportunity wrong = new Opportunity(null, "Intern", "Acme", "Part-time",
				"Desc", "https://example.com", "2026-12-15", null);

		ApiException e = assertThrows(ApiException.class, () -> service.create(wrong));

		assertTrue(e.getDetail().contains("type: must be one of Internship, Full-time, On-campus"));
	}

	@Test
	void linkMustBeAWebAddress() {
		Opportunity wrong = new Opportunity(null, "Intern", "Acme", "Internship",
				"Desc", "javascript:alert(1)", "2026-12-15", null);

		ApiException e = assertThrows(ApiException.class, () -> service.create(wrong));

		assertTrue(e.getDetail().contains("link: must start with http:// or https://"));
	}

	@Test
	void deadlineMustBeARealDateAndNotInThePast() {
		Opportunity notADate = new Opportunity(null, "Intern", "Acme", "Internship",
				"Desc", "https://example.com", "2026-02-30", null);
		Opportunity yesterday = new Opportunity(null, "Intern", "Acme", "Internship",
				"Desc", "https://example.com", "2026-11-10", null);

		assertTrue(assertThrows(ApiException.class, () -> service.create(notADate))
				.getDetail().contains("deadline: must be a real date"));
		assertTrue(assertThrows(ApiException.class, () -> service.create(yesterday))
				.getDetail().contains("deadline: can't be in the past"));
	}

	@Test
	void deadlineTodayIsAllowed() {
		Opportunity today = new Opportunity(null, "Intern", "Acme", "Internship",
				"Desc", "https://example.com", "2026-11-11", null);

		service.create(today);

		assertEquals(TODAY, repository.savedDeadline);
	}

	@Test
	void tooLongTitleIsRejected() {
		Opportunity wrong = new Opportunity(null, "a".repeat(201), "Acme", "Internship",
				"Desc", "https://example.com", "2026-12-15", null);

		ApiException e = assertThrows(ApiException.class, () -> service.create(wrong));

		assertTrue(e.getDetail().contains("title: must be 200 characters or fewer"));
	}

	// =======================================================================
	// PUT and DELETE /opportunities/{id}
	// =======================================================================

	@Test
	void editingAClosedPostingIsAllowed() {
		Opportunity closed = new Opportunity(null, "Intern", "Acme", "Internship",
				"Fixed a typo", "https://example.com", "2026-10-01", null);

		Opportunity result = service.update(7, closed);

		assertEquals("Fixed a typo", result.description());
	}

	@Test
	void editingOrDeletingAMissingPostingIs404() {
		repository.exists = false;

		ApiException edit = assertThrows(ApiException.class, () -> service.update(999, valid()));
		ApiException delete = assertThrows(ApiException.class, () -> service.delete(999));

		assertEquals(HttpStatus.NOT_FOUND, edit.getStatus());
		assertEquals("NOT_FOUND", delete.getCode());
	}

	@Test
	void editStillChecksTheFields() {
		Opportunity wrong = new Opportunity(null, "", "Acme", "Internship",
				"Desc", "https://example.com", "2026-12-15", null);

		assertThrows(ApiException.class, () -> service.update(7, wrong));
	}

	// =======================================================================
	// GET /opportunities
	// =======================================================================

	@Test
	void listHidesClosedPostingsByDefault() {
		PagedResponse<Opportunity> page = service.list(1, 20, null, null, false);

		assertEquals(TODAY, repository.lastFilter.openOnDate());
		assertEquals(1, page.totalCount());
	}

	@Test
	void includeClosedShowsEverything() {
		service.list(1, 20, null, null, true);

		assertNull(repository.lastFilter.openOnDate());
	}

	@Test
	void keywordAndTypeArePassedTrimmed() {
		service.list(1, 20, "  capital ", " Internship ", false);

		assertEquals("capital", repository.lastFilter.q());
		assertEquals("Internship", repository.lastFilter.type());
	}

	@Test
	void badTypeFilterIs400() {
		ApiException e = assertThrows(ApiException.class, () -> service.list(1, 20, null, "Job", false));

		assertEquals("VALIDATION_ERROR", e.getCode());
	}

	@Test
	void missingPostingIs404() {
		repository.exists = false;

		assertEquals("NOT_FOUND", assertThrows(ApiException.class, () -> service.get(5)).getCode());
	}
}