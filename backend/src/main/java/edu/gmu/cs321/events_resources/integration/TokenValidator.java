package edu.gmu.cs321.events_resources.integration;

import edu.gmu.cs321.events_resources.model.CurrentUser;

/**
 * Turns a login token into the user it belongs to.
  * Two versions; the AUTH_MODE setting picks which one runs:
 *   FakeTokenValidator        (AUTH_MODE=fake, the default) -> 4 made-up test tokens
 *   SharedCoreTokenValidator  (AUTH_MODE=shared-core)       -> calls Shared Core's GET /auth/validate
 * Either one may also throw an ApiException (e.g. 403 ACCOUNT_SUSPENDED, 503 SERVICE_DOWN).
 **/
public interface TokenValidator {

    /** Returns the user for this token, or null if the token is invalid. */
    CurrentUser validate(String token);
}
