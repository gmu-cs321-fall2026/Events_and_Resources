package edu.gmu.cs321.events_resources.service;

import edu.gmu.cs321.events_resources.common.ApiException;
import edu.gmu.cs321.events_resources.common.PagedResponse;
import edu.gmu.cs321.events_resources.model.Opportunity;
import edu.gmu.cs321.events_resources.repository.OpportunityRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * OpportunityService (MASON-13 Post jobs): the rules for job and internship
 * postings.
 *
 * Checks every value before anything reaches the database, and lists every bad
 * field at once in "detail", e.g. "title: required; link: must start with
 * http:// or https://".
 * Who may call what (staff for create/edit/delete) is checked earlier, in
 * AuthInterceptor.
 */
@Service
public class OpportunityService {

	/** The allowed values for "type" (exact match). */
	public static final List<String> TYPES = List.of("Internship", "Full-time", "On-campus");

	/** Longest keyword allowed in GET /opportunities?q=. */
	static final int MAX_KEYWORD_LENGTH = 100;

	private final OpportunityRepository repository;
	private final Clock clock;

	/** The constructor Spring uses: "today" comes from the computer's clock. */
	@Autowired
	public OpportunityService(OpportunityRepository repository) {
		this(repository, Clock.systemDefaultZone());
	}

	/** For tests: pass a fixed clock so "today" is always the same day. */
	public OpportunityService(OpportunityRepository repository, Clock clock) {
		this.repository = repository;
		this.clock = clock;
	}

	// -----------------------------------------------------------------------
	// READING (public)
	// -----------------------------------------------------------------------

	/**
	 * GET /opportunities: one page of postings, soonest deadline first.
	 *
	 * @param q             optional keyword (title, employer, description, type)
	 * @param type          optional, one of TYPES
	 * @param includeClosed false (default) = hide postings whose deadline has
	 *                      passed
	 */
	public PagedResponse<Opportunity> list(int page, int pageSize, String q, String type, boolean includeClosed) {
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
		if (!problems.isEmpty()) {
			throw ApiException.badRequest("Invalid filter", String.join("; ", problems));
		}

		OpportunityRepository.Filter filter = new OpportunityRepository.Filter(
				keyword, typeFilter, includeClosed ? null : LocalDate.now(clock));

		List<Opportunity> items = repository.findPage(filter, pageSize, PagedResponse.offset(page, pageSize));
		return new PagedResponse<>(items, page, pageSize, repository.count(filter));
	}

	/** GET /opportunities/{id}: one posting, or 404 NOT_FOUND. */
	public Opportunity get(long id) {
		return repository.findById(id).orElseThrow(() -> notFound(id));
	}

	// -----------------------------------------------------------------------
	// WRITING (staff or admin)
	// -----------------------------------------------------------------------

	/**
	 * POST /opportunities: checks the posting and saves it. The deadline can't be
	 * in the past.
	 */
	public Opportunity create(Opportunity input) {
		List<String> problems = new ArrayList<>();
		Opportunity clean = clean(input, problems);
		LocalDate deadline = parseDeadline(input == null ? null : input.deadline(), problems);
		if (deadline != null && deadline.isBefore(LocalDate.now(clock))) {
			problems.add("deadline: can't be in the past");
		}
		throwIfAny(problems);
		return repository.insert(clean, deadline);
	}

	/**
	 * PUT /opportunities/{id}: replaces the posting's fields with the ones sent.
	 * Same checks as create, except a past deadline is allowed, so staff can still
	 * fix a typo in a posting that has closed.
	 */
	public Opportunity update(long id, Opportunity input) {
		List<String> problems = new ArrayList<>();
		Opportunity clean = clean(input, problems);
		LocalDate deadline = parseDeadline(input == null ? null : input.deadline(), problems);
		throwIfAny(problems);
		return repository.update(id, clean, deadline).orElseThrow(() -> notFound(id));
	}

	/**
	 * DELETE /opportunities/{id}: removes the posting, or 404 NOT_FOUND if it
	 * doesn't exist.
	 */
	public void delete(long id) {
		if (!repository.delete(id)) {
			throw notFound(id);
		}
	}

	// -----------------------------------------------------------------------
	// CHECKS
	// -----------------------------------------------------------------------

	/**
	 * Checks the text fields and returns a trimmed copy with no id or datePosted
	 * (the server sets those). Problems are added to the list, nothing is thrown
	 * here.
	 */
	private Opportunity clean(Opportunity o, List<String> problems) {
		if (o == null) {
			problems.add("body: required");
			return null;
		}
		String title = requiredText(o.title(), "title", 200, problems);
		String employer = requiredText(o.employer(), "employer", 200, problems);
		String description = requiredText(o.description(), "description", 2000, problems);
		String link = requiredText(o.link(), "link", 500, problems);

		String type = blankToNull(o.type());
		if (type == null) {
			problems.add("type: required");
		} else if (!TYPES.contains(type)) {
			problems.add("type: must be one of " + String.join(", ", TYPES));
		}

		// Only web links: this also stops "javascript:" links from being saved and
		// clicked.
		if (link != null && !(link.startsWith("http://") || link.startsWith("https://"))) {
			problems.add("link: must start with http:// or https://");
		}

		return new Opportunity(null, title, employer, type, description, link, null, null);
	}

	private LocalDate parseDeadline(String value, List<String> problems) {
		if (value == null || value.isBlank()) {
			problems.add("deadline: required");
			return null;
		}
		try {
			return LocalDate.parse(value.trim());
		} catch (DateTimeParseException e) {
			problems.add("deadline: must be a real date in YYYY-MM-DD format");
			return null;
		}
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

	private void throwIfAny(List<String> problems) {
		if (!problems.isEmpty()) {
			throw ApiException.badRequest("Some fields are invalid", String.join("; ", problems));
		}
	}

	private ApiException notFound(long id) {
		return ApiException.notFound("Posting " + id + " not found");
	}

	private String blankToNull(String value) {
		return (value == null || value.isBlank()) ? null : value.trim();
	}
}