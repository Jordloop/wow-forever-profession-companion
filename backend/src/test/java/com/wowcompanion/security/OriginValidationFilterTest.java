package com.wowcompanion.security;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OriginValidationFilterTest {
    private static final List<String> DEVELOPMENT_ORIGINS = List.of(
            "http://localhost:4200",
            "http://127.0.0.1:4200");

    private final OriginValidationFilter filter = new OriginValidationFilter(
            new SecurityProperties(DEVELOPMENT_ORIGINS),
            new SecurityProblemWriter(new ObjectMapper()));

    @Test
    void rejectsUntrustedHostOnSafeRequests() throws Exception {
        MockHttpServletResponse response = apply("GET", "attacker.example", "attacker.example", 80, null, false);

        assertEquals(403, response.getStatus());
        assertEquals("application/problem+json", response.getContentType());
    }

    @Test
    void rejectsMissingAndNonCanonicalLoopbackHosts() throws Exception {
        MockHttpServletResponse missingHost = apply("GET", null, "localhost", 8080, null, false);
        MockHttpServletResponse nonCanonicalLoopback = apply(
                "GET", "127.000.0.1", "127.000.0.1", 8080, null, false);

        assertEquals(403, missingHost.getStatus());
        assertEquals(403, nonCanonicalLoopback.getStatus());
    }

    @Test
    void rejectsMalformedHostPorts() throws Exception {
        MockHttpServletResponse nonNumericPort = apply(
                "GET", "localhost:invalid", "localhost", 8080, null, false);
        MockHttpServletResponse zeroPort = apply(
                "GET", "localhost:0", "localhost", 8080, null, false);
        MockHttpServletResponse outOfRangePort = apply(
                "GET", "localhost:65536", "localhost", 8080, null, false);

        assertEquals(403, nonNumericPort.getStatus());
        assertEquals(403, zeroPort.getStatus());
        assertEquals(403, outOfRangePort.getStatus());
    }

    @Test
    void rejectsMatchingUntrustedHostAndOriginOnStateChangingRequests() throws Exception {
        MockHttpServletResponse response = apply(
                "POST",
                "attacker.example:8080",
                "attacker.example",
                8080,
                "http://attacker.example:8080",
                false);

        assertEquals(403, response.getStatus());
        assertEquals("application/problem+json", response.getContentType());
    }

    @Test
    void acceptsLocalhostRequestsAndConfiguredDevelopmentProxyOrigins() throws Exception {
        MockHttpServletResponse localResponse = apply(
                "GET", "localhost:8080", "localhost", 8080, null, true);
        MockHttpServletResponse proxyResponse = apply(
                "POST", "127.0.0.1:8081", "127.0.0.1", 8081, "http://localhost:4200", true);
        MockHttpServletResponse ipv6LoopbackResponse = apply(
                "GET", "[::1]:8080", "::1", 8080, null, true);

        assertEquals(200, localResponse.getStatus());
        assertEquals(200, proxyResponse.getStatus());
        assertEquals(200, ipv6LoopbackResponse.getStatus());
    }

    @Test
    void rejectsMultipleHostHeaders() throws Exception {
        MockHttpServletRequest request = request("GET", "localhost", "localhost", 8080, null);
        request.addHeader(HttpHeaders.HOST, "attacker.example");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> chainCalled.set(true));

        assertFalse(chainCalled.get());
        assertEquals(403, response.getStatus());
    }

    @Test
    void rejectsUntrustedDevelopmentProxyOriginsAtConfigurationTime() {
        assertThrows(IllegalArgumentException.class, () -> new OriginValidationFilter(
                new SecurityProperties(List.of("https://attacker.example")),
                new SecurityProblemWriter(new ObjectMapper())));
    }

    private MockHttpServletResponse apply(
            String method,
            String host,
            String serverName,
            int serverPort,
            String origin,
            boolean expectChain) throws Exception {
        MockHttpServletRequest request = request(method, host, serverName, serverPort, origin);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> chainCalled.set(true));

        assertEquals(expectChain, chainCalled.get());
        return response;
    }

    private MockHttpServletRequest request(
            String method,
            String host,
            String serverName,
            int serverPort,
            String origin) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        request.setScheme("http");
        request.setServerName(serverName);
        request.setServerPort(serverPort);
        if (host != null) {
            request.addHeader(HttpHeaders.HOST, host);
        }
        if (origin != null) {
            request.addHeader(HttpHeaders.ORIGIN, origin);
        }
        return request;
    }
}
