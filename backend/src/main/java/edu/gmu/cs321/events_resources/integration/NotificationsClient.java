package edu.gmu.cs321.events_resources.integration;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * NotificationsClient: asks the Notifications subsystem (Subsystem 6) to notify a user.
 *
 * Send userId
 *
 * send() returns false instead of throwing when Notifications is down, slow (2 s timeout) or says no. The caller decides what to do;
 * ReminderService simply tries again on its next check.
 *
 */

@Component
public class NotificationsClient {
    public static final String EVENT_REMINDER = "event_reminder";
    public static final String REGISTRATION = "registration";

    private static final Logger log = LoggerFactory.getLogger(NotificationsClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    /** The JSON body we send. title = null lets Notifications use the template's own title. */
    public record TemplateRequest(String templateId, UUID userId, Map<String, String> params, String title) {
    }

    private final RestClient notifications;

    @Autowired
    public NotificationsClient(@Value("${notifications.url}") String notificationsUrl) {
        this(RestClient.builder()
                .baseUrl(notificationsUrl)
                .requestFactory(withTimeouts()));
    }

    /** For the unit test, which plugs in a fake Notifications server. */
    NotificationsClient(RestClient.Builder builder) {
        this.notifications = builder.build();
    }

    private static SimpleClientHttpRequestFactory withTimeouts() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(TIMEOUT);
        factory.setReadTimeout(TIMEOUT);
        return factory;
    }

    /**
     * Sends one templated notification to one user.
     * Returns true if Notifications accepted it (any 2xx answer), false otherwise.
     * RestClientException covers both "answered with an error" and "no answer at all".
     */
    public boolean send(String templateId, UUID userId, Map<String, String> params) {
        try {
            notifications.post()
                    .uri("/api/notifications/template")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new TemplateRequest(templateId, userId, params, null))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            log.warn("Notifications didn't accept '{}' for user {}: {}", templateId, userId, e.getMessage());
            return false;
        }
    }

}
