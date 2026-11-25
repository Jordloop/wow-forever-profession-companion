package com.wowcompanion.security;

import com.wowcompanion.logging.RequestCorrelationFilter;
import java.io.IOException;
import java.net.URI;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
final class SecurityProblemWriter {
    private final ObjectMapper objectMapper;

    SecurityProblemWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    void writeForbidden(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problem.setTitle("Forbidden");
        problem.setDetail("The request could not be completed.");
        problem.setInstance(URI.create(request.getRequestURI()));

        String correlationId = MDC.get(RequestCorrelationFilter.CORRELATION_ID_MDC_KEY);
        if (correlationId != null) {
            problem.setProperty(RequestCorrelationFilter.CORRELATION_ID_MDC_KEY, correlationId);
        }

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
