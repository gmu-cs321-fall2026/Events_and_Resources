package edu.gmu.cs321.events_resources.common;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import edu.gmu.cs321.events_resources.integration.FakeTokenValidator;
import edu.gmu.cs321.events_resources.model.CurrentUser;

/**
 * Unit tests for AuthInterceptor: the 401 / 403 rules from Interface Contract v2/v3.
 *
 * "Unit test" = we test this ONE class by itself: no Spring app, no database,
 * no network. We build the interceptor with `new`, hand it fake requests, and
 * check what it does. That makes these tests fast and easy to read.
 *
 * Rules being checked:
 *   GET  /events                  -> public, no token needed
 *   POST /events/{id}/register    -> any logged-in user (no/bad token -> 401)
 *   POST /events                  -> staff only (student -> 403)
 */
class AuthInterceptorTest {

    // The object under test. It uses the same FakeTokenValidator the app uses,
    // so "student-token" = student #7 and "staff-token" = staff #1.
    private final AuthInterceptor interceptor = new AuthInterceptor(new FakeTokenValidator());

    /** Helper: builds a fake request. token = null means "no Authorization header". */
    private MockHttpServletRequest request(String method, String path, String token) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        if (token != null) {
            request.addHeader("Authorization", "Bearer " + token);
        }
        return request;
    }

    /** Helper: runs the interceptor exactly the way Spring would, before the controller. */
    private boolean run(MockHttpServletRequest request) {
        return interceptor.preHandle(request, new MockHttpServletResponse(), null);
    }

    @Test
    void listingEventsIsPublic() {
        MockHttpServletRequest request = request("GET", "/events", null);

        assertTrue(run(request));                                  // let through
        assertNull(request.getAttribute(AuthInterceptor.CURRENT_USER)); // and no user attached
    }

    @Test
    void registeringWithoutATokenIs401() {
        // assertThrows = "this code MUST throw an ApiException"; it gives us the exception to inspect.
        ApiException error = assertThrows(ApiException.class,
                () -> run(request("POST", "/events/3/register", null)));

        assertEquals(401, error.getStatus().value());
        assertEquals("UNAUTHORIZED", error.getCode());
    }

    @Test
    void registeringWithAnUnknownTokenIs401() {
        ApiException error = assertThrows(ApiException.class,
                () -> run(request("POST", "/events/3/register", "made-up-token")));

        assertEquals("UNAUTHORIZED", error.getCode());
    }

    @Test
    void studentCanRegisterAndIsAttachedToTheRequest() {
        MockHttpServletRequest request = request("POST", "/events/3/register", "student-token");

        assertTrue(run(request));

        // The controller reads the user from this attribute, so check it's the right one.
        CurrentUser user = (CurrentUser) request.getAttribute(AuthInterceptor.CURRENT_USER);
        assertEquals(FakeTokenValidator.STUDENT_ID, user.id());   // Shared Core's jdoe UUID
        assertEquals(List.of(CurrentUser.STUDENT), user.roles()); // roles is now a list
    }

    @Test
    void studentCreatingAnEventIs403() {
        ApiException error = assertThrows(ApiException.class,
                () -> run(request("POST", "/events", "student-token")));

        assertEquals(403, error.getStatus().value());
        assertEquals("FORBIDDEN", error.getCode());
    }

    @Test
    void staffCanCreateAnEvent() {
        assertTrue(run(request("POST", "/events", "staff-token")));
    }

    @Test
    void adminCanCreateAnEvent() {
        // Shared Core's note for us: admins may create events too.
        assertTrue(run(request("POST", "/events", "admin-token")));
    }

    @Test
    void studentWhoIsAlsoAMentorStillCannotCreateAnEvent() {
        // student2 has TWO roles: ["student", "mentor"]. Neither one is staff.
        ApiException error = assertThrows(ApiException.class,
                () -> run(request("POST", "/events", "student2-token")));

        assertEquals("FORBIDDEN", error.getCode());
    }
}
