package edu.gmu.cs321.events_resources.common;

// Our interface from integration/: asks "who does this token belong to?" -> tokenValidator.validate(token)
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import edu.gmu.cs321.events_resources.integration.TokenValidator;
import edu.gmu.cs321.events_resources.model.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
 

/**
 * AuthInterceptor: the "front door" check that runs BEFORE every controller method
 * on the URLs listed in WebConfig.
 *
 * It answers two questions, in this order:
 *   1. WHO are you?           (authentication)  no token / bad token   -> 401
 *   2. Are you ALLOWED?       (authorization)   e.g. student on staff-only URL -> 403
 *
 * If both pass, it attaches the CurrentUser to the request, and the controller
 * reads it with:   @RequestAttribute(AuthInterceptor.CURRENT_USER) CurrentUser user
 *
 * Errors are thrown as ApiException, so ErrorHandler turns them into the
 * standard {"error", "code", "detail"} JSON, just like errors from controllers.
 *
 * WHICH URLs NEED WHAT (see needsLogin() and needsStaff() below):
 *   GET  /events, GET /events/{id}           public, no token needed
 *   POST /events/{id}/register               any logged-in user
 *   GET  /users/{userId}/events/registered   any logged-in user (the service checks it's YOUR list)
 *   POST /events                             staff only
 *   PUT / DELETE /events/{id}                staff only (planned for Sprint 3)
 */
@Component // Spring creates one of these; WebConfig asks for it and plugs it in
public class AuthInterceptor implements HandlerInterceptor {

    /** The name we store the user under on the request. Controllers use this same name to read it. */
    public static final String CURRENT_USER = "currentUser";

    private static final String BEARER_PREFIX = "Bearer ";
    /* “bearer” means whoever carries this */

    // Spring hands us whichever TokenValidator has @Component (the fake one, for now).
    // We only know about the INTERFACE, so swapping to the real one changes nothing here.
    private final TokenValidator tokenValidator;

    public AuthInterceptor(TokenValidator tokenValidator) {
        this.tokenValidator = tokenValidator;
    }

    /**
     * Spring calls this before the controller method.
     * Return true  = "let the request continue to the controller".
     * Throw        = "stop here", and ErrorHandler sends the error response.
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String method = request.getMethod();   // "GET", "POST", ...
        String path = request.getRequestURI(); // "/events/5/register" (no "?page=1" part)

        // Public URL? Let it through without checking anything.
        if (!needsLogin(method, path)) {
            return true;
        }

        // ---- 1. WHO are you? ----
        CurrentUser user = authenticate(request);

        // ---- 2. Are you ALLOWED? ----
        if (needsStaff(method, path) && !user.isStaff()) {
            throw ApiException.forbidden("Only Career Services staff can do this");
        }

        // Passed both checks: hand the user to the controller.
        request.setAttribute(CURRENT_USER, user);
        return true;
    }

    /**
     * Reads "Authorization: Bearer <token>" and asks the TokenValidator who it belongs to.
     * Throws 401 if the header is missing, badly formatted, or the token is invalid.
     */
    private CurrentUser authenticate(HttpServletRequest request) {
        String header = request.getHeader("Authorization"); // null if the header isn't there

        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw ApiException.unauthorized("Please log in: missing \"Authorization: Bearer <token>\" header");
        }

        // "Bearer student-token" -> "student-token"
        String token = header.substring(BEARER_PREFIX.length()).trim();

        CurrentUser user = tokenValidator.validate(token);
        if (user == null) {
            throw ApiException.unauthorized("Your login token is invalid or expired");
        }
        return user;
    }

    /** Does this request need ANY logged-in user? Everything except reading events does. */
    private boolean needsLogin(String method, String path) {
        boolean readingEvents = method.equals("GET") && path.startsWith("/events");
        // OPTIONS = a browser asking "what am I allowed to send?" before the real request.
        // Browsers never attach tokens to it, so it must not require one.
        boolean browserPreCheck = method.equals("OPTIONS");
        return !(readingEvents || browserPreCheck);
    }

    /** Does this request need a STAFF user? Creating, editing, or deleting events. */
    private boolean needsStaff(String method, String path) {
        // .matches() checks the WHOLE path against a pattern ("regular expression"):
        //   /events/?        -> "/events" or "/events/"
        //   /events/\\d+/?   -> "/events/" + one or more digits, e.g. "/events/12"
        boolean createEvent = method.equals("POST") && path.matches("/events/?");
        boolean changeEvent = (method.equals("PUT") || method.equals("DELETE"))
                && path.matches("/events/\\d+/?");
        return createEvent || changeEvent;
    }
}
