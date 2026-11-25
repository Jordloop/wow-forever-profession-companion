package com.wowcompanion.security;

import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("browser-test")
@RestController
@RequestMapping("/api/test-security")
final class BrowserSecurityTestController {
    @GetMapping("/csrf")
    void csrf() { }

    @PostMapping("/write")
    Map<String, String> write() {
        return Map.of("result", "ok");
    }
}
