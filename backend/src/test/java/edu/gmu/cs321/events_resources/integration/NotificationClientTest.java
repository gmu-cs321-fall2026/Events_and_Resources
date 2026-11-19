package edu.gmu.cs321.events_resources.integration;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Unit tests for NotificationsClient, using a FAKE Notifications server.
 * Same set-up as SharedCoreTokenValidatorTest: MockRestServiceServer checks the request
 * we send and answers whatever each test says. No network needed.
 * containsString checks pieces of the JSON, because the order inside "params" can vary.
 *
 * Rules being checked:
 *   - we POST {"templateId", "userId", "params"} to /api/notifications/template
 *   - any 2xx answer -> true
 *   - an error answer or no answer -> false (never an exception)
 */
class NotificationClientTest {
     private static final String URL = "http://notifications/api/notifications/template";
    private static final Map<String, String> PARAMS = Map.of(
            "title", "Resume Workshop", "days", "1", "location", "Horizon Hall", "link", "");

    private MockRestServiceServer fakeNotifications;
    private NotificationsClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://notifications");
        fakeNotifications = MockRestServiceServer.bindTo(builder).build();
        client = new NotificationsClient(builder);
    }

    @Test
    void sendsTheTemplateUserAndValues() {
        fakeNotifications.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(allOf(
                        containsString("\"templateId\":\"event_reminder\""),
                        containsString("\"userId\":\"00000000-0000-0000-0000-000000000003\""),
                        containsString("\"title\":\"Resume Workshop\""),
                        containsString("\"days\":\"1\""),
                        containsString("\"link\":\"\""))))
                .andRespond(withSuccess("{\"id\":\"n-1\"}", MediaType.APPLICATION_JSON));

        assertTrue(client.send(NotificationsClient.EVENT_REMINDER, FakeTokenValidator.STUDENT_ID, PARAMS));
        fakeNotifications.verify();
    }

    @Test
    void anErrorAnswerReturnsFalse() {
        fakeNotifications.expect(requestTo(URL))
                .andRespond(withBadRequest().body("{\"error\":\"Missing required template parameter: link\"}"));

        assertFalse(client.send(NotificationsClient.EVENT_REMINDER, FakeTokenValidator.STUDENT_ID, PARAMS));
    }

    @Test
    void noAnswerAtAllReturnsFalse() {
        fakeNotifications.expect(requestTo(URL))
                .andRespond(withException(new IOException("Connection refused")));

        assertFalse(client.send(NotificationsClient.EVENT_REMINDER, FakeTokenValidator.STUDENT_ID, PARAMS));
    }
}
