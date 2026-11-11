package edu.gmu.cs321.events_resources.repository;

import edu.gmu.cs321.events_resources.model.Opportunity;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * OpportunityRepository (MASON-13): the only class that reads and writes the
 * {@code opportunity} table. Same plain-JDBC style as EventRepository.
 *
 * Every value from a user goes into a "?" placeholder, never into the SQL text,
 * so SQL injection isn't possible.
 */
@Repository
public class OpportunityRepository {

	private final JdbcTemplate jdbc;

	public OpportunityRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	/**
	 * Filters for GET /opportunities. Any field can be null, meaning "don't filter
	 * on it".
	 *
	 * @param q          keyword, found in title, employer, description or type
	 *                   (case ignored)
	 * @param type       exactly one of OpportunityService.TYPES
	 * @param openOnDate only postings whose deadline is on or after this day, or
	 *                   null for all
	 */
	public record Filter(String q, String type, LocalDate openOnDate) {
	}

	// -----------------------------------------------------------------------
	// READING
	// -----------------------------------------------------------------------

	/** One page of postings that match, soonest deadline first (id breaks ties). */
	public List<Opportunity> findPage(Filter filter, int limit, int offset) {
		Where where = buildWhere(filter);
		List<Object> params = new ArrayList<>(where.params());
		params.add(limit);
		params.add(offset);

		return jdbc.query(
				"SELECT " + COLUMNS + " FROM opportunity" + where.sql()
						+ " ORDER BY deadline, id LIMIT ? OFFSET ?",
				MAPPER,
				params.toArray());
	}

	/**
	 * How many postings match in total, across every page. Becomes "totalCount".
	 */
	public long count(Filter filter) {
		Where where = buildWhere(filter);
		Long count = jdbc.queryForObject(
				"SELECT COUNT(*) FROM opportunity" + where.sql(),
				Long.class,
				where.params().toArray());
		return count == null ? 0 : count;
	}

	/** One posting by id, or an empty Optional if there's no such posting. */
	public Optional<Opportunity> findById(long id) {
		return jdbc.query("SELECT " + COLUMNS + " FROM opportunity WHERE id = ?", MAPPER, id)
				.stream().findFirst();
	}

	// -----------------------------------------------------------------------
	// WRITING (staff only, checked by AuthInterceptor before we get here)
	// -----------------------------------------------------------------------

	/** Saves a new posting. date_posted = today. Returns it with its new id. */
	public Opportunity insert(Opportunity o, LocalDate deadline) {
		return jdbc.queryForObject(
				"INSERT INTO opportunity (title, employer, job_type, description, link, deadline, date_posted) "
						+ "VALUES (?, ?, ?, ?, ?, ?, CURRENT_DATE) RETURNING " + COLUMNS,
				MAPPER,
				o.title(), o.employer(), o.type(), o.description(), o.link(), deadline);
	}

	/**
	 * Replaces every editable field of a posting. date_posted stays the same.
	 * Returns the updated posting, or an empty Optional if there's no posting with
	 * that id.
	 */
	public Optional<Opportunity> update(long id, Opportunity o, LocalDate deadline) {
		return jdbc.query(
				"UPDATE opportunity SET title = ?, employer = ?, job_type = ?, description = ?, "
						+ "link = ?, deadline = ? WHERE id = ? RETURNING " + COLUMNS,
				MAPPER,
				o.title(), o.employer(), o.type(), o.description(), o.link(), deadline, id)
				.stream().findFirst();
	}

	/**
	 * Deletes a posting. Returns true if a row was deleted, false if there was
	 * none.
	 */
	public boolean delete(long id) {
		return jdbc.update("DELETE FROM opportunity WHERE id = ?", id) > 0;
	}

	// -----------------------------------------------------------------------
	// HELPERS
	// -----------------------------------------------------------------------

	private record Where(String sql, List<Object> params) {
	}

	/**
	 * Turns the filter into " WHERE ... AND ..." plus its values, in order.
	 * The keyword uses strpos instead of LIKE so a typed % or _ is searched as
	 * itself.
	 */
	private Where buildWhere(Filter f) {
		List<String> conditions = new ArrayList<>();
		List<Object> params = new ArrayList<>();

		if (f.openOnDate() != null) {
			conditions.add("deadline >= ?");
			params.add(f.openOnDate());
		}
		if (f.type() != null) {
			conditions.add("job_type = ?");
			params.add(f.type());
		}
		if (f.q() != null) {
			conditions.add("strpos(lower(concat_ws(' ', title, employer, description, job_type)), lower(?)) > 0");
			params.add(f.q());
		}

		String sql = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
		return new Where(sql, params);
	}

	private static final String COLUMNS = "id, title, employer, job_type, description, link, "
			+ "to_char(deadline, 'YYYY-MM-DD') AS deadline, "
			+ "to_char(date_posted, 'YYYY-MM-DD') AS date_posted";

	private static final RowMapper<Opportunity> MAPPER = (rs, rowNum) -> new Opportunity(
			rs.getLong("id"),
			rs.getString("title"),
			rs.getString("employer"),
			rs.getString("job_type"),
			rs.getString("description"),
			rs.getString("link"),
			rs.getString("deadline"),
			rs.getString("date_posted"));
}