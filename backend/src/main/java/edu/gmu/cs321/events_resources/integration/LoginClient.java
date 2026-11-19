package edu.gmu.cs321.events_resources.integration;

import edu.gmu.cs321.events_resources.model.LoginResponse;

/**
 * LoginClient: checks an email + password and returns a login token.
 *
 * Two versions, picked by the same AUTH_MODE setting as TokenValidator, so the
 * token a user gets always works with the validator that is running:
 *   FakeLoginClient        (AUTH_MODE=fake, the default) -> the 4 test users
 *   SharedCoreLoginClient  (AUTH_MODE=shared-core)       -> Shared Core's POST /auth/login
 *
 * Errors are thrown as ApiException, so ErrorHandler sends our standard JSON:
 *   wrong email or password -> 401 UNAUTHORIZED
 *   suspended account       -> 403 ACCOUNT_SUSPENDED
 *   Shared Core is down     -> 503 SERVICE_DOWN
 */

public interface LoginClient {
    String WRONG_LOGIN = "Wrong email or password";

    LoginResponse login(String email, String password);
}
