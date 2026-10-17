package edu.gmu.cs321.events_resources.integration;

import edu.gmu.cs321.events_resources.model.CurrentUser;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * FakeTokenValidator: a TEMPORARY stand-in for Shared Core's login check.
 *
 * Shared Core's real GET /auth/validate isn't ready yet, so this class pretends
 * to be it. It knows three made-up tokens. Use them when testing with curl:
 *
 *     curl -H "Authorization: Bearer student-token"  ...   -> student #7
 *     curl -H "Authorization: Bearer student2-token" ...   -> student #8
 *     curl -H "Authorization: Bearer staff-token"    ...   -> staff   #1
 *     any other token                                      -> null (invalid -> 401)
 *
 * ========================================================================
 * HOW TO SWAP TO THE REAL ONE (later, once Shared Core is ready)
 *   1. Create SharedCoreTokenValidator.java in this same folder:
 *          @Component
 *          public class SharedCoreTokenValidator implements TokenValidator { ... }
 *   2. DELETE the @Component line below (just the annotation, or the whole file).
 *   3. Done. Spring now hands out the real one; nothing else needs to change.
 *
 *   Why step 2? If both classes have @Component, Spring finds two TokenValidators,
 *   can't choose, and the app refuses to start.
 * ========================================================================
 */
@Component // <-- tells Spring: "create one of these and give it to anyone who asks for a TokenValidator"
public class FakeTokenValidator implements TokenValidator { // "implements" = "I keep the TokenValidator promise"

    // token -> user
    // Map.of(...) builds a small, unchangeable lookup table.
    // static final = one shared copy for the whole app that never changes.
    private static final Map<String, CurrentUser> TEST_USERS = Map.of(
            "student-token",  new CurrentUser(7, CurrentUser.STUDENT),
            "student2-token", new CurrentUser(8, CurrentUser.STUDENT), // a 2nd student, to test "you can only see YOUR registrations"
            "staff-token",    new CurrentUser(1, CurrentUser.STAFF)    // staff, to test "only staff can create events"
    );

    @Override // "this method comes from the interface"; Java errors if the name/parameters don't match
    public CurrentUser validate(String token) {
        // Look the token up in the table.
        // Map.get() returns null if the token isn't there, which means "invalid token".
        return TEST_USERS.get(token);
    }
}