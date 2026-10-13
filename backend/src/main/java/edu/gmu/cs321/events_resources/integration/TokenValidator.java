package edu.gmu.cs321.events_resources.integration;

import edu.gmu.cs321.events_resources.model.CurrentUser;

/**
 * Turns a login token into the user it belongs to.
 * Today: FakeTokenValidator. Later: a version that calls Shared Core's GET /auth/validate.
 */
public interface TokenValidator {

    /** Returns the user for this token, or null if the token is invalid. */
    CurrentUser validate(String token);
}
