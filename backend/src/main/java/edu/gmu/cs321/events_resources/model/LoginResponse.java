package edu.gmu.cs321.events_resources.model;

/**
 * LoginResponse: what POST /session/login sends back after a correct email + password.
 *
 *   {"token": "eyJhbGciOi...", "expiresIn": 3600}
 *
 * Same shape as Shared Core's POST /auth/login answer.
 *   token     = what the website sends as "Authorization: Bearer <token>" from now on
 *   expiresIn = how many seconds the token stays valid
 */

public record LoginResponse(String token, long expiresIn) {
}
