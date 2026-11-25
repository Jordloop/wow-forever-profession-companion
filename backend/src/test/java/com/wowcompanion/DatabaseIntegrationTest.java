package com.wowcompanion;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.UUID;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.wowcompanion.security.SecurityProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@Import({DatabaseIntegrationTest.ErrorRoutes.class, DatabaseIntegrationTest.SecurityRoutes.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.password=unused-test-placeholder",
                "management.server.port=0"
        })
class DatabaseIntegrationTest {
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.7");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SecurityProperties securityProperties;

    @Autowired
    private Environment environment;

    @Autowired
    private ApplicationContext applicationContext;

    @LocalServerPort
    private int serverPort;

    @LocalManagementPort
    private int managementPort;

    @Test
    void connectsToPostgres() {
        assertEquals(1, jdbcTemplate.queryForObject("SELECT 1", Integer.class));
        assertEquals("PostgreSQL", jdbcTemplate.execute(
                (org.springframework.jdbc.core.ConnectionCallback<String>) connection ->
                        connection.getMetaData().getDatabaseProductName()));
    }

    @Test
    void appliesInitialMigration() {
        assertEquals(1L, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '1' AND success",
                Long.class));
    }

    @Test
    void doesNotConfigureUserManagement() {
        assertTrue(applicationContext.getBeansOfType(UserDetailsService.class).isEmpty());
    }

    @Test
    void keepsBrowserSecurityTestEndpointsDisabledOutsideTheirTestProfile()
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(
                        "http://127.0.0.1:" + serverPort + "/api/test-security/csrf"))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
    }

    @Test
    void returnsServerGeneratedCorrelationIdForApiRequests() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + serverPort + "/"))
                .header("X-Correlation-ID", "caller-supplied-value")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
        String correlationId = response.headers().firstValue("X-Correlation-ID").orElse(null);
        assertNotNull(correlationId);
        assertNotEquals("caller-supplied-value", correlationId);
        UUID.fromString(correlationId);
        assertTrue(response.headers().firstValue("Content-Type").orElse("")
                .startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
        assertTrue(response.body().contains("\"instance\":\"/\""));
        assertTrue(response.body().contains("\"correlationId\":\"" + correlationId + "\""));
    }

    @Test
    void returnsValidationErrorsWithoutRejectedValues() throws IOException, InterruptedException {
        CsrfCredentials csrf = getCsrfCredentials(allowedOrigin());
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + serverPort + "/test-errors"))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("Origin", allowedOrigin())
                .header("Cookie", csrf.cookie())
                .header("X-XSRF-TOKEN", csrf.token())
                .POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"private-rejected-value\"}"))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("")
                .startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
        String correlationId = response.headers().firstValue("X-Correlation-ID").orElse(null);
        assertNotNull(correlationId);
        assertTrue(response.body().contains("\"correlationId\":\"" + correlationId + "\""));
        assertTrue(response.body().contains("\"instance\":\"/test-errors\""));
        assertTrue(response.body().contains("\"field\":\"name\""));
        assertTrue(response.body().contains("\"message\":\"Value length is outside the allowed range.\""));
        assertFalse(response.body().contains("\"rejectedValue\""));
        assertFalse(response.body().contains("private-rejected-value"));
    }

    @Test
    void sanitizesUnexpectedErrorsAndReturnsCorrelationId() throws IOException, InterruptedException {
        Logger logger = (Logger) LoggerFactory.getLogger("com.wowcompanion.api.ApiExceptionHandler");
        Level previousLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        HttpResponse<String> response;
        try {
            logger.setLevel(Level.ERROR);
            HttpRequest request = HttpRequest.newBuilder(
                            URI.create("http://127.0.0.1:" + serverPort + "/test-errors/unexpected"))
                    .GET()
                    .build();
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previousLevel);
            appender.stop();
        }

        String correlationId = response.headers().firstValue("X-Correlation-ID").orElse(null);
        assertEquals(500, response.statusCode());
        assertNotNull(correlationId);
        assertTrue(response.headers().firstValue("Content-Type").orElse("")
                .startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
        assertTrue(response.body().contains("\"status\":500"));
        assertTrue(response.body().contains("\"instance\":\"/test-errors/unexpected\""));
        assertTrue(response.body().contains("\"correlationId\":\"" + correlationId + "\""));
        assertTrue(response.body().contains("An unexpected server error occurred."));
        assertFalse(response.body().contains("synthetic-password"));
        assertFalse(response.body().contains("synthetic-access-token"));
        assertFalse(response.body().contains("synthetic-url-token"));
        assertFalse(response.body().contains("provider.example"));
        assertFalse(response.body().contains("access_token"));
        assertFalse(response.body().contains("synthetic-provider-body"));

        assertEquals(1, appender.list.size());
        ILoggingEvent logEvent = appender.list.getFirst();
        String logMessage = logEvent.getFormattedMessage();
        assertTrue(logMessage.contains("IllegalStateException"));
        assertTrue(logMessage.contains("IllegalArgumentException"));
        assertTrue(logMessage.contains("ErrorRoutes#fail"));
        assertFalse(logMessage.contains("synthetic-password"));
        assertFalse(logMessage.contains("synthetic-access-token"));
        assertFalse(logMessage.contains("synthetic-url-token"));
        assertFalse(logMessage.contains("provider.example"));
        assertFalse(logMessage.contains("access_token"));
        assertFalse(logMessage.contains("synthetic-provider-body"));
        assertNull(logEvent.getThrowableProxy());
    }

    @Test
    void returnsFieldErrorsForInvalidMethodParameters() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(
                        "http://127.0.0.1:" + serverPort + "/test-errors/parameter?count=0"))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("")
                .startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
        String correlationId = response.headers().firstValue("X-Correlation-ID").orElse(null);
        assertNotNull(correlationId);
        assertTrue(response.body().contains("\"correlationId\":\"" + correlationId + "\""));
        assertTrue(response.body().contains("\"field\":\"count\""));
        assertTrue(response.body().contains("\"message\":\"Value is below the allowed minimum.\""));
        assertFalse(response.body().contains("count=0"));
    }

    @Test
    void exposesHealthInfoAndMetricsWithoutHealthDetails() throws IOException, InterruptedException {
        HttpResponse<String> health = getManagementEndpoint("/actuator/health");
        HttpResponse<String> info = getManagementEndpoint("/actuator/info");
        HttpResponse<String> metrics = getManagementEndpoint("/actuator/metrics");

        assertEquals(200, health.statusCode());
        assertTrue(health.body().contains("\"status\":\"UP\""));
        assertFalse(health.body().contains("\"components\""));
        assertEquals(200, info.statusCode());
        assertTrue(info.body().contains("\"version\":\"0.1.0-SNAPSHOT\""));
        assertEquals(200, metrics.statusCode());
        assertTrue(metrics.body().contains("\"names\""));
    }

    @Test
    void acceptsValidCsrfTokenFromTheConfiguredBrowserOrigin() throws IOException, InterruptedException {
        String origin = allowedOrigin();
        CsrfCredentials csrf = getCsrfCredentials(origin);
        HttpResponse<String> response = postTestWrite(origin, csrf);

        assertEquals(200, response.statusCode());
    }

    @Test
    void rejectsStateChangingRequestWithoutCsrfToken() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + serverPort + "/test-security/write"))
                .header("Origin", allowedOrigin())
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertForbiddenProblem(response);
    }

    @Test
    void rejectsStateChangingRequestWithInvalidCsrfToken() throws IOException, InterruptedException {
        String origin = allowedOrigin();
        CsrfCredentials csrf = getCsrfCredentials(origin);
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + serverPort + "/test-security/write"))
                .header("Origin", origin)
                .header("Cookie", csrf.cookie())
                .header("X-XSRF-TOKEN", "invalid-token")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertForbiddenProblem(response);
    }

    @Test
    void rejectsStateChangingRequestFromDisallowedOrigin() throws IOException, InterruptedException {
        CsrfCredentials csrf = getCsrfCredentials(allowedOrigin());
        HttpResponse<String> response = postTestWrite("https://attacker.example", csrf);

        assertForbiddenProblem(response);
    }

    @Test
    void rejectsStateChangingRequestWithoutOrigin() throws IOException, InterruptedException {
        CsrfCredentials csrf = getCsrfCredentials(allowedOrigin());
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + serverPort + "/test-security/write"))
                .header("Cookie", csrf.cookie())
                .header("X-XSRF-TOKEN", csrf.token())
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertForbiddenProblem(response);
    }

    private CsrfCredentials getCsrfCredentials(String origin) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + serverPort + "/test-security/csrf"))
                .header("Origin", origin)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        String csrfSetCookie = response.headers().allValues("Set-Cookie").stream()
                .filter(value -> value.startsWith("XSRF-TOKEN="))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected the Angular CSRF cookie."));
        assertTrue(csrfSetCookie.contains("Path=/"));
        assertTrue(csrfSetCookie.contains("SameSite=Strict"));
        assertFalse(csrfSetCookie.contains("HttpOnly"));
        assertFalse(response.headers().allValues("Set-Cookie").stream()
                .anyMatch(value -> value.startsWith("JSESSIONID=")));
        String cookie = csrfSetCookie
                .split(";", 2)[0];
        return new CsrfCredentials(cookie, cookie.substring("XSRF-TOKEN=".length()));
    }

    private HttpResponse<String> postTestWrite(String origin, CsrfCredentials csrf)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + serverPort + "/test-security/write"))
                .header("Origin", origin)
                .header("Cookie", csrf.cookie())
                .header("X-XSRF-TOKEN", csrf.token())
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void assertForbiddenProblem(HttpResponse<String> response) {
        assertEquals(403, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("")
                .startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
        String correlationId = response.headers().firstValue("X-Correlation-ID").orElse(null);
        assertNotNull(correlationId);
        assertTrue(response.body().contains("\"status\":403"));
        assertTrue(response.body().contains("\"correlationId\":\"" + correlationId + "\""));
    }

    private String allowedOrigin() {
        boolean productionProfile = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (!productionProfile && !securityProperties.developmentProxyOrigins().isEmpty()) {
            return securityProperties.developmentProxyOrigins().getFirst();
        }
        return "http://127.0.0.1:" + serverPort;
    }

    private HttpResponse<String> getManagementEndpoint(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + managementPort + path))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @RestController
    @RequestMapping("/test-errors")
    static class ErrorRoutes {
        @PostMapping
        void validate(@Valid @RequestBody TestRequest body) { }

        @GetMapping("/parameter")
        void validateParameter(@RequestParam("count") @Min(1) int count) { }

        @GetMapping("/unexpected")
        void fail() {
            throw new IllegalStateException(
                    "password=synthetic-password authorization=Bearer synthetic-access-token "
                            + "url=https://provider.example/items?access_token=synthetic-url-token "
                            + "body={\"payload\":\"synthetic-provider-body\"}",
                    new IllegalArgumentException("synthetic-provider-body in nested cause"));
        }
    }

    @RestController
    @RequestMapping("/test-security")
    static class SecurityRoutes {
        @GetMapping("/csrf")
        void csrf() { }

        @PostMapping("/write")
        void write() { }
    }

    private record TestRequest(@Size(max = 2) String name) { }

    private record CsrfCredentials(String cookie, String token) { }
}
