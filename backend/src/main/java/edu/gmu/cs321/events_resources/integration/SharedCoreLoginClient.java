package edu.gmu.cs321.events_resources.integration;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import edu.gmu.cs321.events_resources.common.ApiException;
import edu.gmu.cs321.events_resources.model.LoginRequest;
import edu.gmu.cs321.events_resources.model.LoginResponse;

/**
 * SharedCoreLoginClient: the REAL login. Passes the email + password on to
 * Shared Core's POST /auth/login and returns the token they give back.
 *
 * Shared Core's answers, and what we send to the website:
 *   200 {"token", "expiresIn"}            -> the same {"token", "expiresIn"}
 *   400 or 401 (wrong email or password)  -> 401 UNAUTHORIZED "Wrong email or password"
 *   403 ACCOUNT_SUSPENDED                 -> 403 ACCOUNT_SUSPENDED
 *   anything else, or no answer in 2 s    -> 503 SERVICE_DOWN
 *
 * Built like SharedCoreTokenValidator: same SHARED_CORE_URL, same 2-second timeout,
 * and a second constructor so the unit test can plug in a fake Shared Core.
 * Only created when AUTH_MODE=shared-core.
 */

@Component
@ConditionalOnProperty(name = "auth.mode", havingValue = "shared-core")
public class SharedCoreLoginClient implements LoginClient {
    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    private final RestClient sharedCore;

    @Autowired
    public SharedCoreLoginClient(@Value("${shared-core.url}") String sharedCoreUrl) {
        this(RestClient.builder()
                .baseUrl(sharedCoreUrl)
                .requestFactory(withTimeouts()));
    }

    SharedCoreLoginClient(RestClient.Builder builder) {
        this.sharedCore = builder.build();
    }

    private static SimpleClientHttpRequestFactory withTimeouts() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(TIMEOUT);
        factory.setReadTimeout(TIMEOUT);
        return factory;
    }

    /**
     * Sends {"email", "password"} to Shared Core and returns their token.
     * RestClientResponseException = Shared Core answered with an error status;
     * ResourceAccessException = no answer at all (off, wrong URL, or too slow).
     */
    @Override
    public LoginResponse login(String email, String password) {
        LoginResponse answer;
        try {
            answer = sharedCore.post()
                    .uri("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new LoginRequest(email, password))
                    .retrieve()
                    .body(LoginResponse.class);

        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            if (status == 400 || status == 401) {
                throw ApiException.unauthorized(WRONG_LOGIN);
            }
            if (status == 403) {
                throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_SUSPENDED",
                        "Your account is suspended", "");
            }
            throw serviceDown();

        } catch (ResourceAccessException e) {
            throw serviceDown();
        }

        if (answer == null || answer.token() == null || answer.token().isBlank()) {
            throw serviceDown();
        }
        return answer;
    }

    private static ApiException serviceDown() {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_DOWN",
                "Login service is unavailable. Please try again later", "Could not reach Shared Core");
    }

}
