package edu.gmu.cs321.events_resources.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.web.client.RestClient;

import edu.gmu.cs321.events_resources.common.ApiException;
import edu.gmu.cs321.events_resources.model.CurrentUser;

/**
 * Unit tests for SharedCoreTokenValidator, using a FAKE Shared Core.
 *
 * MockRestServiceServer pretends to be Shared Core: each test says what request it
 * expects (URL + Authorization header) and what to answer (200 + JSON, 401, 403,
 * 500, or no connection). No network and no real Shared Core needed.
 *
 * setUp() runs before every test (@BeforeEach), so each one starts fresh.
 * bindTo(builder) must happen BEFORE the validator calls builder.build().
 */
class SharedCoreTokenValidatorTest {

    private MockRestServiceServer fakeSharedCore;
    private SharedCoreTokenValidator validator;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://shared-core");
        fakeSharedCore = MockRestServiceServer.bindTo(builder).build();
        validator = new SharedCoreTokenValidator(builder);
    }

    private ResponseActions expectValidate(String token) {
        return fakeSharedCore.expect(requestTo("http://shared-core/auth/validate"))
                .andExpect(header("Authorization", "Bearer " + token));
    }

    // ------------------------------------------------------------------ 200: a real user

    @Test
    void validTokenBecomesACurrentUser() {
        expectValidate("good-token").andRespond(withSuccess(
                "{\"userId\":\"00000000-0000-0000-0000-000000000004\","
                        + "\"roles\":[\"student\",\"mentor\"],\"status\":\"active\"}",
                MediaType.APPLICATION_JSON));

        CurrentUser user = validator.validate("good-token");

        assertEquals(FakeTokenValidator.STUDENT2_ID, user.id());
        assertEquals(List.of(CurrentUser.STUDENT, CurrentUser.MENTOR), user.roles());
        fakeSharedCore.verify();
    }

    @Test
    void uppercaseRolesAreLowercased() {
        expectValidate("staff-token").andRespond(withSuccess(
                "{\"userId\":\"00000000-0000-0000-0000-000000000002\",\"roles\":[\"STAFF\"],\"status\":\"active\"}",
                MediaType.APPLICATION_JSON));

        CurrentUser user = validator.validate("staff-token");

        assertEquals(List.of(CurrentUser.STAFF), user.roles());
    }

    // ------------------------------------------------------------------ errors

    @Test
    void sharedCore401MeansInvalidToken() {
        expectValidate("expired").andRespond(withUnauthorizedRequest());

        assertNull(validator.validate("expired"));
    }

    @Test
    void sharedCore403MeansSuspended() {
        expectValidate("suspended").andRespond(withStatus(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":\"Account suspended\",\"code\":\"ACCOUNT_SUSPENDED\",\"detail\":\"\"}"));

        ApiException error = assertThrows(ApiException.class, () -> validator.validate("suspended"));

        assertEquals(403, error.getStatus().value());
        assertEquals("ACCOUNT_SUSPENDED", error.getCode());
    }

    @Test
    void sharedCoreErrorIs503() {
        expectValidate("any").andRespond(withServerError());

        ApiException error = assertThrows(ApiException.class, () -> validator.validate("any"));

        assertEquals(503, error.getStatus().value());
        assertEquals("SERVICE_DOWN", error.getCode());
    }

    @Test
    void sharedCoreDownIs503() {
        expectValidate("any").andRespond(withException(new IOException("Connection refused")));

        ApiException error = assertThrows(ApiException.class, () -> validator.validate("any"));

        assertEquals("SERVICE_DOWN", error.getCode());
    }
}