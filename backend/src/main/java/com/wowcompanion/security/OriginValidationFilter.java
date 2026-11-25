package com.wowcompanion.security;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

final class OriginValidationFilter extends OncePerRequestFilter {
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    private final Set<String> developmentProxyOrigins;
    private final SecurityProblemWriter problemWriter;

    OriginValidationFilter(SecurityProperties properties, SecurityProblemWriter problemWriter) {
        this.developmentProxyOrigins = properties.developmentProxyOrigins().stream()
                .filter(StringUtils::hasText)
                .map(OriginValidationFilter::normalizeConfiguredOrigin)
                .collect(Collectors.toUnmodifiableSet());
        this.problemWriter = problemWriter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String method = request.getMethod().toUpperCase(Locale.ROOT);
        if (SAFE_METHODS.contains(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        String origin = normalizeOrigin(request.getHeader(HttpHeaders.ORIGIN));
        String requestOrigin = normalizeOrigin(
                request.getScheme(), request.getServerName(), request.getServerPort());
        if (origin != null && (origin.equals(requestOrigin) || developmentProxyOrigins.contains(origin))) {
            filterChain.doFilter(request, response);
            return;
        }

        problemWriter.writeForbidden(request, response);
    }

    private static String normalizeConfiguredOrigin(String origin) {
        String normalized = normalizeOrigin(origin);
        if (normalized == null) {
            throw new IllegalArgumentException("Invalid development proxy origin configured.");
        }
        return normalized;
    }

    private static String normalizeOrigin(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            URI origin = URI.create(value);
            if (!origin.isAbsolute()
                    || origin.getHost() == null
                    || origin.getRawUserInfo() != null
                    || (origin.getRawPath() != null && !origin.getRawPath().isEmpty())
                    || origin.getRawQuery() != null
                    || origin.getRawFragment() != null) {
                return null;
            }
            return normalizeOrigin(origin.getScheme(), origin.getHost(), origin.getPort());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static String normalizeOrigin(String scheme, String host, int port) {
        if (!StringUtils.hasText(scheme) || !StringUtils.hasText(host)) {
            return null;
        }
        String normalizedScheme = scheme.toLowerCase(Locale.ROOT);
        if (!normalizedScheme.equals("http") && !normalizedScheme.equals("https")) {
            return null;
        }
        int effectivePort = port;
        if (effectivePort == -1) {
            effectivePort = switch (normalizedScheme) {
                case "http" -> 80;
                case "https" -> 443;
                default -> -1;
            };
        }
        if (effectivePort < 1 || effectivePort > 65535) {
            return null;
        }

        String normalizedHost = host.toLowerCase(Locale.ROOT);
        if (normalizedHost.contains(":")
                && !(normalizedHost.startsWith("[") && normalizedHost.endsWith("]"))) {
            normalizedHost = "[" + normalizedHost + "]";
        }
        return normalizedScheme + "://" + normalizedHost + ":" + effectivePort;
    }
}
