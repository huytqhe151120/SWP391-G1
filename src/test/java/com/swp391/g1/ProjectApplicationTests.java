package com.swp391.g1;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifies that the entire Spring application context can be loaded.
 *
 * The context starts WITHOUT a live database because the Hibernate dialect is
 * configured explicitly and ddl-auto is "none" (see application.properties).
 */
@SpringBootTest
class ProjectApplicationTests {

    @Test
    void contextLoads() {
    }
}