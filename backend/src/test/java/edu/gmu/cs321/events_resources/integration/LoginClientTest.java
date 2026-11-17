package edu.gmu.cs321.events_resources.integration;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.web.client.RestClient;

import edu.gmu.cs321.events_resources.common.ApiException;
import edu.gmu.cs321.events_resources.model.LoginResponse;

/**
 * Unit tests for both LoginClients (website login).
 *
 *   FakeLoginClient:        the 4 test users with password ChangeMe123!
 *   SharedCoreLoginClient:  a FAKE Shared Core (MockRestServiceServer), like
 *                           SharedCoreTokenValidatorTest. No network needed.
 */
class LoginClientTest {
    // ---- FakeLoginClient ----
    private final FakeLoginClient fake = new FakeLoginClient();

    @Test
    void fakeLoginGivesTheMatchingTestToken() {
        assertEquals("staff-token", fake.login("staff@gmu.edu", "ChangeMe123!").token());
        assertEquals("student-token", fake.login(" JDoe@GMU.edu ", "ChangeMe123!").token()); // case and spaces
    }

    @Test
    void fakeLoginWithAWrongPasswordOrUnknownEmailIs401() {
        ApiException wrongPassword = assertThrows(ApiException.class,
                () -> fake.login("staff@gmu.edu", "wrong"));
        ApiException unknownEmail = assertThrows(ApiException.class,
                () -> fake.login("nobody@gmu.edu", "ChangeMe123!"));

        assertEquals(401, wrongPassword.getStatus().value());
        assertEquals(401, unknownEmail.getStatus().value());
        assertEquals(wrongPassword.getMessage(), unknownEmail.getMessage()); // never says which part was wrong
    }

    // ---- SharedCoreLoginClient ----
    private MockRestServiceServer fakeSharedCore;
    private SharedCoreLoginClient sharedCore;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://shared-core");
        fakeSharedCore = MockRestServiceServer.bindTo(builder).build();
        sharedCore = new SharedCoreLoginClient(builder);
    }

    private ResponseActions expectLogin() {
        return fakeSharedCore.expect(requestTo("http://shared-core/auth/login"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(allOf(
                        containsString("\"email\":\"staff@gmu.edu\""),
                        containsString("\"password\":\"ChangeMe123!\""))));
    }

    @Test
    void sharedCoreTokenIsPassedBack() {
        expectLogin().andRespond(withSuccess("{\"token\":\"real-jwt\",\"expiresIn\":3600}",
                MediaType.APPLICATION_JSON));

        LoginResponse answer = sharedCore.login("staff@gmu.edu", "ChangeMe123!");

        assertEquals("real-jwt", answer.token());
        assertEquals(3600, answer.expiresIn());
        fakeSharedCore.verify();
    }

    @Test
    void sharedCore401BecomesOur401() {
        expectLogin().andRespond(withUnauthorizedRequest());

        ApiException error = assertThrows(ApiException.class,
                () -> sharedCore.login("staff@gmu.edu", "ChangeMe123!"));

        assertEquals(401, error.getStatus().value());
        assertEquals(LoginClient.WRONG_LOGIN, error.getMessage());
    }

    @Test
    void sharedCore403BecomesAccountSuspended() {
        expectLogin().andRespond(withStatus(HttpStatus.FORBIDDEN));

        ApiException error = assertThrows(ApiException.class,
                () -> sharedCore.login("staff@gmu.edu", "ChangeMe123!"));

        assertEquals("ACCOUNT_SUSPENDED", error.getCode());
    }

    @Test
    void sharedCoreErrorOrNoAnswerIs503() {
        expectLogin().andRespond(withServerError());
        ApiException serverError = assertThrows(ApiException.class,
                () -> sharedCore.login("staff@gmu.edu", "ChangeMe123!"));
        assertEquals("SERVICE_DOWN", serverError.getCode());

        fakeSharedCore.reset();
        expectLogin().andRespond(withException(new IOException("Connection refused")));
        ApiException noAnswer = assertThrows(ApiException.class,
                () -> sharedCore.login("staff@gmu.edu", "ChangeMe123!"));
        assertEquals("SERVICE_DOWN", noAnswer.getCode());
    }
}
