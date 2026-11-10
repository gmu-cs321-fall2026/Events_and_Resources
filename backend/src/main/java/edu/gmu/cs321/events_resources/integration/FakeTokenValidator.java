package edu.gmu.cs321.events_resources.integration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import edu.gmu.cs321.events_resources.model.CurrentUser;


/**
 * FakeTokenValidator: a stand-in for Shared Core's GET /auth/validate.
 * Used by tests and CI, and on your laptop when Shared Core isn't running.
 * Its users have the SAME UUIDs and roles as Shared Core's seed users
 * (their db/seed/001_seed_users.sql), so data saved in fake mode lines up
 * with real logins later:
 *
 *     curl -H "Authorization: Bearer student-token"  ...  -> jdoe@gmu.edu   ...0003  [student]
 *     curl -H "Authorization: Bearer student2-token" ...  -> grad@gmu.edu   ...0004  [student, mentor]
 *     curl -H "Authorization: Bearer staff-token"    ...  -> staff@gmu.edu  ...0002  [staff]
 *     curl -H "Authorization: Bearer admin-token"    ...  -> admin@gmu.edu  ...0001  [admin]
 *     any other token                                     -> null (invalid -> 401)
 *
 * ========================================================================
 *    * WHICH VALIDATOR RUNS: the AUTH_MODE setting (application.properties) picks one:
 *       AUTH_MODE=fake         -> this class (default; tests and CI use it)
 *       AUTH_MODE=shared-core  -> SharedCoreTokenValidator, which calls Shared Core
 *    @ConditionalOnProperty creates this class only in fake mode
 *   (matchIfMissing = true: also when AUTH_MODE isn't set at all).
 * ========================================================================
 */
@Component // tells Spring: "create one of these and give it to anyone who asks for a TokenValidator"

@ConditionalOnProperty(name = "auth.mode", havingValue = "fake", matchIfMissing = true)

public class FakeTokenValidator implements TokenValidator { // "implements" = "I keep the TokenValidator promise"

    // Shared Core's seed user ids, so fake data matches real logins later.
    // public = tests can use them, e.g. FakeTokenValidator.STUDENT_ID, instead of retyping the UUID.
    // UUID.fromString(...) turns the text into a real UUID (and fails if the text isn't a valid one).
    public static final UUID ADMIN_ID    = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID STAFF_ID    = UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static final UUID STUDENT_ID  = UUID.fromString("00000000-0000-0000-0000-000000000003");
    public static final UUID STUDENT2_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");

    // token -> user
    // Map.of(...) builds a small, unchangeable lookup table.
    // List.of(...) builds a small, unchangeable list (each user's roles).
    // static final = one shared copy for the whole app that never changes.
    private static final Map<String, CurrentUser> TEST_USERS = Map.of(
            // a regular student
            "student-token",  new CurrentUser(STUDENT_ID,  List.of(CurrentUser.STUDENT)),
            // a 2nd student who is ALSO a mentor: tests "you can only see YOUR registrations"
            // and "having several roles still doesn't make you staff"
            "student2-token", new CurrentUser(STUDENT2_ID, List.of(CurrentUser.STUDENT, CurrentUser.MENTOR)),
            // Career Services staff: tests "only staff can create events"
            "staff-token",    new CurrentUser(STAFF_ID,    List.of(CurrentUser.STAFF)),
            // platform admin: tests "admins may also create events"
            "admin-token",    new CurrentUser(ADMIN_ID,    List.of(CurrentUser.ADMIN))
    );

    @Override // "this method comes from the interface"; Java errors if the name/parameters don't match
    public CurrentUser validate(String token) {
        // Look the token up in the table.
        // Map.get() returns null if the token isn't there, which means "invalid token" (-> 401).
        return TEST_USERS.get(token);
    }
}