\\## Asked



Asked ChatGPT to check the integration snippet suggest what to fix so other teams can run Subsystem 3 in the integration docker-compose.







\\## Produced



Found two problems in the README snippet and wrote a corrected version:

- The snippet sets AUTH_MODE: shared-core but not SHARED_CORE_URL or NOTIFICATIONS_URL, so the container falls back to http://localhost:8081 / :8082 from application.properties. Inside a container, localhost is our own container, so real logins and Notifications calls would fail. Claude added SHARED_CORE_URL: http://shared-core:8080 and NOTIFICATIONS_URL: http://notifications:8080.
- depends_on: - events-db only waits for Postgres to start, not to be ready, so Spring Boot could crash connecting too early. Claude added a pg_isready healthcheck to events-db and changed depends_on to condition: service_healthy, the same as our own docker-compose.yml.



\\## Changed or rejected



Shared Core and Notifications because have no integration compose file or published image yet, so I kept them as placeholders until the teams pick their service names

