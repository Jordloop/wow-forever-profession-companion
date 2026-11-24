package com.wowcompanion;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
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

    private HttpResponse<String> getManagementEndpoint(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + managementPort + path))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
