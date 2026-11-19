package edu.gmu.cs321.events_resources.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import edu.gmu.cs321.events_resources.common.ApiException;
import edu.gmu.cs321.events_resources.common.AuthInterceptor;
import edu.gmu.cs321.events_resources.integration.LoginClient;
import edu.gmu.cs321.events_resources.model.CurrentUser;
import edu.gmu.cs321.events_resources.model.LoginRequest;
import edu.gmu.cs321.events_resources.model.LoginResponse;
import edu.gmu.cs321.events_resources.model.SessionUser;

/**
 * SessionController: logging in on our website.
 *
 * ENDPOINTS
 *   POST /session/login  {"email", "password"}  -> 200 {"token", "expiresIn"}           public
 *   GET  /session/me                            -> 200 {"userId", "roles", "staff"}    needs a token
 *
 * WHY THE WEBSITE LOGS IN THROUGH OUR BACKEND (and not straight to Shared Core)
 *   In Docker, Spring Boot serves the built website itself; there is no Vite proxy.
 *   The browser can only reach our backend, and our backend already knows where
 *   Shared Core is (SHARED_CORE_URL). Going through here also means the login form
 *   works the same in fake mode (FakeLoginClient) and in shared-core mode.
 *   The password is passed on once and never saved or logged.
 *
 * WHY "/session" AND NOT "/auth"
 *   Shared Core owns /auth/* in the platform's API gateway. Using our own path
 *   means the gateway can never send our login to the wrong team.
 *
 * GET /session/me goes through AuthInterceptor (see WebConfig), so in shared-core
 * mode it asks Shared Core's /auth/validate, and in fake mode FakeTokenValidator.
 * It works for a token from the login form and for a VITE_DEV_TOKEN alike.
 */
@RestController
public class SessionController {

    private final LoginClient loginClient;

    public SessionController(LoginClient loginClient) {
        this.loginClient = loginClient;
    }

    /**
     * POST /session/login: email + password -> token.
     * Errors: 400 VALIDATION_ERROR (missing email or password), 401 UNAUTHORIZED,
     * 403 ACCOUNT_SUSPENDED, 503 SERVICE_DOWN.
     */
    @PostMapping("/session/login")
    public LoginResponse login(@RequestBody(required = false) LoginRequest request) {
        if (request == null || isBlank(request.email()) || isBlank(request.password())) {
            throw ApiException.badRequest("Email and password are required",
                    "email and password: required");
        }
        return loginClient.login(request.email().trim(), request.password());
    }

    /**
     * GET /session/me: who does this token belong to?
     * Errors (from AuthInterceptor): 401 no or bad token, 403 ACCOUNT_SUSPENDED, 503 SERVICE_DOWN.
     */
    @GetMapping("/session/me")
    public SessionUser me(@RequestAttribute(AuthInterceptor.CURRENT_USER) CurrentUser user) {
        return SessionUser.from(user);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
