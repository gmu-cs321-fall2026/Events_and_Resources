package edu.gmu.cs321.events_resources.repository;

import java.sql.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import edu.gmu.cs321.events_resources.model.Resource;
import edu.gmu.cs321.events_resources.model.ResourceRequest;

/**
 * ResourceRepository (MASON-7 / MASON-11 / MASON-12): the only class that reads and
 * writes the {@code resource} and {@code resource_audiences} tables.
 *
 *   ResourceController  ->  ResourceService (rules)  ->  ResourceRepository (SQL, this file)  ->  PostgreSQL
 *
 * Like the other repositories, each method is one small database step and never decides
 * yes/no; ResourceService does. Every user value goes into a "?" placeholder, never into
 * the SQL text, so SQL injection isn't possible.
 *
 * "Only while pending" is written INTO the SQL (WHERE ... AND status = 'pending').
 * The check and the change are then one statement, so a student's edit and an admin's
 * approval can't both win: whichever reaches the row first locks it, and the other one
 * then sees the new status.
 */
@Repository
public class ResourceRepository {

    private final JdbcTemplate jdbc;

    public ResourceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Filters for a list. Any field can be null, meaning "don't filter on it".
     *
     * @param q           keyword, found in title, description, industry or type (case ignored)
     * @param major       an audience name; also matches resources for "All Students"
     * @param type        exactly one of ResourceService.TYPES
     * @param status      pending, approved or rejected
     * @param userId      only resources this user shared ("My submissions")
     * @param oldestFirst true for the review queue (first come, first reviewed); false = newest first
     */
    public record Filter(String q, String major, String type, String status, UUID userId, boolean oldestFirst) {
    }

    // -----------------------------------------------------------------------
    // READING
    // -----------------------------------------------------------------------

    /** One page of resources that match the filter. */
    public List<Resource> findPage(Filter filter, int limit, int offset) {
        Where where = buildWhere(filter);
        List<Object> params = new ArrayList<>(where.params());
        params.add(limit);
        params.add(offset);

        String order = filter.oldestFirst() ? " ORDER BY r.created_at, r.id" : " ORDER BY r.created_at DESC, r.id DESC";
        return jdbc.query(
                "SELECT " + COLUMNS + " FROM resource r" + where.sql() + order + " LIMIT ? OFFSET ?",
                MAPPER,
                params.toArray());
    }

    /** How many resources match in total, across every page. Becomes "totalCount". */
    public long count(Filter filter) {
        Where where = buildWhere(filter);
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM resource r" + where.sql(),
                Long.class,
                where.params().toArray());
        return count == null ? 0 : count;
    }

    /** One resource by id, or an empty Optional if there's none. */
    public Optional<Resource> findById(long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM resource r WHERE r.id = ?", MAPPER, id)
                .stream().findFirst();
    }

    /**
     * Looks up audience names ("CS Majors", "Juniors") and returns name -> id for the
     * ones that exist. A name missing from the answer isn't a real audience.
     */
    public Map<String, Long> findAudienceIds(Collection<String> names) {
        Map<String, Long> ids = new HashMap<>();
        if (names.isEmpty()) {
            return ids;
        }
        String placeholders = String.join(", ", names.stream().map(n -> "?").toList());
        jdbc.query("SELECT id, name FROM audiences WHERE name IN (" + placeholders + ")",
                rs -> {
                    ids.put(rs.getString("name"), rs.getLong("id"));
                },
                names.toArray());
        return ids;
    }

    // -----------------------------------------------------------------------
    // WRITING
    // -----------------------------------------------------------------------

    /** Saves a new resource and returns its new id. Audiences are saved separately. */
    public long insert(ResourceRequest r, UUID userId, String owner, String status) {
        Long id = jdbc.queryForObject(
                "INSERT INTO resource (user_id, owner, title, description, resource_type, industry, link, status) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
                Long.class,
                userId, owner, r.title(), r.description(), r.type(), r.industry(), r.link(), status);
        return id == null ? 0 : id;
    }

    /**
     * Replaces the fields of a resource, but ONLY if this user shared it and it's still
     * pending. Returns false if nothing was changed (the service then works out why).
     */
    public boolean updateIfPending(long id, UUID userId, ResourceRequest r) {
        return jdbc.update(
                "UPDATE resource SET title = ?, description = ?, resource_type = ?, industry = ?, link = ?, "
                        + "updated_at = now() "
                        + "WHERE id = ? AND user_id = ? AND status = 'pending'",
                r.title(), r.description(), r.type(), r.industry(), r.link(), id, userId) > 0;
    }

    /**
     * Deletes a resource, but ONLY if this user shared it and it's still pending.
     * resource_audiences rows go with it (ON DELETE CASCADE).
     */
    public boolean deleteIfPending(long id, UUID userId) {
        return jdbc.update(
                "DELETE FROM resource WHERE id = ? AND user_id = ? AND status = 'pending'",
                id, userId) > 0;
    }

    /** Staff/admin decision: sets the status, who decided, when, and the note. False = no such resource. */
    public boolean updateStatus(long id, String status, UUID reviewerId, String note) {
        return jdbc.update(
                "UPDATE resource SET status = ?, reviewed_by = ?, reviewed_at = now(), review_note = ? WHERE id = ?",
                status, reviewerId, note, id) > 0;
    }

    /** Replaces which audiences a resource is for: removes the old links, adds the new ones. */
    public void replaceAudiences(long resourceId, Collection<Long> audienceIds) {
        jdbc.update("DELETE FROM resource_audiences WHERE resource_id = ?", resourceId);
        List<Object[]> rows = audienceIds.stream().map(a -> new Object[] { resourceId, a }).toList();
        jdbc.batchUpdate("INSERT INTO resource_audiences (resource_id, audience_id) VALUES (?, ?)", rows);
    }

    // -----------------------------------------------------------------------
    // HELPERS
    // -----------------------------------------------------------------------

    private record Where(String sql, List<Object> params) {
    }

    /**
     * Turns the filter into " WHERE ... AND ..." plus its values, in order.
     * The keyword uses strpos instead of LIKE so a typed % or _ is searched as itself
     * (same as OpportunityRepository).
     */
    private Where buildWhere(Filter f) {
        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (f.status() != null) {
            conditions.add("r.status = ?");
            params.add(f.status());
        }
        if (f.userId() != null) {
            conditions.add("r.user_id = ?");
            params.add(f.userId());
        }
        if (f.type() != null) {
            conditions.add("r.resource_type = ?");
            params.add(f.type());
        }
        if (f.major() != null) {
            conditions.add("EXISTS (SELECT 1 FROM resource_audiences ra JOIN audiences a ON a.id = ra.audience_id "
                    + "WHERE ra.resource_id = r.id AND a.name IN (?, 'All Students'))");
            params.add(f.major());
        }
        if (f.q() != null) {
            conditions.add("strpos(lower(concat_ws(' ', r.title, r.description, r.industry, r.resource_type)), lower(?)) > 0");
            params.add(f.q());
        }

        String sql = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        return new Where(sql, params);
    }

    /**
     * The columns every SELECT returns. "majors" is a small sub-query that collects the
     * resource's audience names into one array, so one row = one whole resource.
     */
    private static final String COLUMNS = "r.id, r.title, r.description, r.resource_type, r.industry, r.link, "
            + "r.owner, r.status, r.user_id, r.review_note, "
            + "to_char(r.created_at, 'YYYY-MM-DD\"T\"HH24:MI:SS') AS submitted_at, "
            + "to_char(r.reviewed_at, 'YYYY-MM-DD\"T\"HH24:MI:SS') AS reviewed_at, "
            + "ARRAY(SELECT a.name FROM resource_audiences ra JOIN audiences a ON a.id = ra.audience_id "
            + "WHERE ra.resource_id = r.id ORDER BY a.id) AS majors";

    private static final RowMapper<Resource> MAPPER = (rs, rowNum) -> new Resource(
            rs.getLong("id"),
            rs.getString("title"),
            rs.getString("description"),
            rs.getString("resource_type"),
            rs.getString("industry"),
            rs.getString("link"),
            toList(rs.getArray("majors")),
            rs.getString("owner"),
            rs.getString("status"),
            rs.getObject("user_id", UUID.class),
            rs.getString("review_note"),
            rs.getString("submitted_at"),
            rs.getString("reviewed_at"));

    /** A SQL array of names -> a Java List. */
    private static List<String> toList(Array array) throws java.sql.SQLException {
        if (array == null) {
            return List.of();
        }
        return List.of((String[]) array.getArray());
    }
}
