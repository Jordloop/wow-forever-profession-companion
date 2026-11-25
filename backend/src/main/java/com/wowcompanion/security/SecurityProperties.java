package com.wowcompanion.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(List<String> developmentProxyOrigins) {
    public SecurityProperties {
        developmentProxyOrigins = developmentProxyOrigins == null
                ? List.of()
                : List.copyOf(developmentProxyOrigins);
    }
}
