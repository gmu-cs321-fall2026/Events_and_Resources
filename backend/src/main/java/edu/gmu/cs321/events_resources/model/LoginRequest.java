package edu.gmu.cs321.events_resources.model;

/**
 * LoginRequest: the JSON body the website sends to POST /session/login.
 *
 *   {"email": "staff@gmu.edu", "password": "ChangeMe123!"}
 *
 * It is the same shape as Shared Core's own LoginRequest, so SharedCoreLoginClient
 * can pass it straight on to their POST /auth/login.
 * We never save or log these values.
 */
public record LoginRequest(String email, String password) {
}
