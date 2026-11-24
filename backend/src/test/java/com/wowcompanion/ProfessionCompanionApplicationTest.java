package com.wowcompanion;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfessionCompanionApplicationTest {
    @Test
    void applicationHasSpringBootEntryPoint() {
        assertTrue(ProfessionCompanionApplication.class.isAnnotationPresent(SpringBootApplication.class));
    }
}
