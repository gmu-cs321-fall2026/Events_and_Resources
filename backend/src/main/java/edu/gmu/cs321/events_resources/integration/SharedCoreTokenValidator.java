package edu.gmu.cs321.events_resources.integration;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import edu.gmu.cs321.events_resources.common.ApiException;
import edu.gmu.cs321.events_resources.model.CurrentUser;

/**
 * SharedCoreTokenValidator: the REAL login check. It asks Shared Core
 * "who does this token belong to?" by calling their GET /auth/validate.
 *
 * Shared Core's answers, and what we do with each:
 *   200 {"userId": "...", "roles": ["student"], "status": "active"}  -> that user
 *   401 (bad or expired token)                                      -> null -> our 401 UNAUTHORIZED
 *   403 ACCOUNT_SUSPENDED                                            -> our 403 ACCOUNT_SUSPENDED
 *   anything else, or no answer within 2 seconds                     -> our 503 SERVICE_DOWN
 *
 * WHICH VALIDATOR RUNS: the AUTH_MODE setting (application.properties) picks one.
 *   AUTH_MODE=fake         -> FakeTokenValidator (default; tests and CI use it)
 *   AUTH_MODE=shared-core  -> this class
 * @ConditionalOnProperty makes Spring create this class only in shared-core mode,
 * so only one validator exists at a time and AuthInterceptor doesn't change.
 */
@Component
@ConditionalOnProperty(name = "auth.mode", havingValue = "shared-core")
public class SharedCoreTokenValidator implements TokenValidator {

    /**
     * The JSON Shared Core sends back, as a record (matches their ValidateResponse.java).
     * Spring fills it in by name: "userId" -> userId, "roles" -> roles, "status" -> status.
     */
    public record ValidateResponse(UUID userId, List<String> roles, String status) {
    }

    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    private final RestClient sharedCore;

    /**
     * Constructors.
     * The first is the one Spring uses: @Value passes in the shared-core.url setting
     * (from SHARED_CORE_URL, default http://localhost:8081), and the client gives up
     * after 2 seconds so our API never hangs. this(...) hands off to the second one.
     * The second is for the unit test, which passes in a builder wired to a fake Shared Core.
     */
    @Autowired
    public SharedCoreTokenValidator(@Value("${shared-core.url}") String sharedCoreUrl) {
        this(RestClient.builder()
                .baseUrl(sharedCoreUrl)
                .requestFactory(withTimeouts()));
    }

    SharedCoreTokenValidator(RestClient.Builder builder) {
        this.sharedCore = builder.build();
    }

    private static SimpleClientHttpRequestFactory withTimeouts() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(TIMEOUT);
        factory.setReadTimeout(TIMEOUT);
        return factory;
    }

    /**
     * Sends GET /auth/validate with the same "Authorization: Bearer <token>" the user sent us.
     *
     * Success: builds a CurrentUser. Roles are lowercased so "STAFF" or "staff" both work.
     * A missing id counts as an invalid token; a status other than "active" counts as suspended.
     *
     * retrieve() throws when Shared Core answers with an error, so the catch blocks
     * turn each case into our own response:
     *   RestClientResponseException  = Shared Core answered with 401 / 403 / 5xx
     *   ResourceAccessException      = no answer at all (off, wrong URL, or over 2 seconds)
     */
    @Override
    public CurrentUser validate(String token) {
        try {
            ValidateResponse answer = sharedCore.get()
                    .uri("/auth/validate")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(ValidateResponse.class);

            if (answer == null || answer.userId() == null) {
                return null;
            }
            if (answer.status() != null && !"active".equalsIgnoreCase(answer.status())) {
                throw suspended();
            }

            List<String> roles = answer.roles() == null
                    ? List.of()
                    : answer.roles().stream().map(role -> role.toLowerCase(Locale.ROOT)).toList();

            return new CurrentUser(answer.userId(), roles);

        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            if (status == 401) {
                return null;
            }
            if (status == 403) {
                throw suspended();
            }
            throw serviceDown();

        } catch (ResourceAccessException e) {
            throw serviceDown();
        }
    }

    /**
     * The two errors this class sends, in our standard {"error", "code", "detail"} format.
     *   403 ACCOUNT_SUSPENDED: Shared Core's own code, so the frontend can say why.
     *   503 SERVICE_DOWN:      "a service we depend on is down; try again later".
     */
    private static ApiException suspended() {
        return new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_SUSPENDED",
                "Your account is suspended", "");
    }

    private static ApiException serviceDown() {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_DOWN",
                "Login service is unavailable. Please try again later", "Could not reach Shared Core");
    }
}