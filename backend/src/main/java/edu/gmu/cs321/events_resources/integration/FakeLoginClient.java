package edu.gmu.cs321.events_resources.integration;
import java.util.Locale;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import edu.gmu.cs321.events_resources.common.ApiException;
import edu.gmu.cs321.events_resources.model.LoginResponse;

/**
 * FakeLoginClient: lets the login form work without Shared Core running.
 * Used when AUTH_MODE=fake (the default), on laptops, in CI and in Docker demos.
 *
 * It knows the same 4 users as Shared Core's seed data, with the same password,
 * so you log in exactly the same way in both modes:
 *
 *   jdoe@gmu.edu   -> student-token     staff@gmu.edu -> staff-token
 *   grad@gmu.edu   -> student2-token    admin@gmu.edu -> admin-token
 *   password for all: ChangeMe123!
 *
 * The tokens it hands out are FakeTokenValidator's test tokens, so every later
 * request is checked by FakeTokenValidator as before.
 * 
 * Shared Core's seed password, so demos use one password everywhere.
 */

@Component
@ConditionalOnProperty(name = "auth.mode", havingValue = "fake", matchIfMissing = true)

public class FakeLoginClient implements LoginClient{
    public static final String TEST_PASSWORD = "ChangeMe123!";

    private static final long EXPIRES_IN_SECONDS = 3600;

    private static final Map<String, String> TOKENS_BY_EMAIL = Map.of(
            "jdoe@gmu.edu", "student-token",
            "grad@gmu.edu", "student2-token",
            "staff@gmu.edu", "staff-token",
            "admin@gmu.edu", "admin-token");

    /**
     * Emails are compared in lowercase ("Staff@GMU.edu" works); the password must match exactly.
     * Anything else is a 401 with the same message, so nobody can tell which part was wrong.
     */
    @Override
    public LoginResponse login(String email, String password) {
        String token;
        if (email == null)
            token =  null;
        else
            token = TOKENS_BY_EMAIL.get(email.trim().toLowerCase(Locale.ROOT));
        
        if (token == null || !TEST_PASSWORD.equals(password)) {
            throw ApiException.unauthorized(WRONG_LOGIN);
        }
        return new LoginResponse(token, EXPIRES_IN_SECONDS);
    }

}
