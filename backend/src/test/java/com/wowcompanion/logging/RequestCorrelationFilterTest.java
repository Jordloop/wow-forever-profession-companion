package com.wowcompanion.logging;

import java.util.UUID;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequestCorrelationFilterTest {
    private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

    @AfterEach
    void clearDiagnosticContext() {
        MDC.clear();
    }

    @Test
    void assignsGeneratedCorrelationIdToResponseAndLoggingContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestCorrelationFilter.CORRELATION_ID_HEADER, "caller-supplied-value");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            String correlationId = response.getHeader(RequestCorrelationFilter.CORRELATION_ID_HEADER);
            assertNotNull(correlationId);
            assertNotEquals("caller-supplied-value", correlationId);
            assertEquals(correlationId, MDC.get("correlationId"));
            UUID.fromString(correlationId);
        });

        assertNull(MDC.get("correlationId"));
    }

    @Test
    void clearsCorrelationIdWhenRequestProcessingFails() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThrows(ServletException.class, () ->
                filter.doFilter(request, response, (servletRequest, servletResponse) -> {
                    throw new ServletException("expected test failure");
                }));

        assertNull(MDC.get("correlationId"));
    }

    @Test
    void includesCorrelationIdInRequestLogContext() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(RequestCorrelationFilter.class);
        Level previousLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.DEBUG);

        try {
            filter.doFilter(
                    new MockHttpServletRequest(),
                    new MockHttpServletResponse(),
                    (servletRequest, servletResponse) -> { });
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previousLevel);
            appender.stop();
        }

        assertEquals(1, appender.list.size());
        ILoggingEvent event = appender.list.get(0);
        assertEquals("HTTP request completed with status 200", event.getFormattedMessage());
        assertNotNull(event.getMDCPropertyMap().get("correlationId"));
        UUID.fromString(event.getMDCPropertyMap().get("correlationId"));
    }
}
