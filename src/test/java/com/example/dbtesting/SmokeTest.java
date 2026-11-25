package com.example.dbtesting;

import com.example.dbtesting.model.User;
import com.example.dbtesting.support.BaseDatabaseTest;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that Docker, Testcontainers, PostgreSQL, JDBC, and Flyway migrations work properly.
 */
class SmokeTest extends BaseDatabaseTest {

    @Test
    void databaseContainerAndFlywayMigrationsAreReady() throws Exception {
        assertTrue(POSTGRES.isRunning(), "PostgreSQL Testcontainer should be running");

        try (Connection connection = openJdbcConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet tables = metaData.getTables(null, "public", "users", null)) {
                assertTrue(tables.next(), "Table 'users' should exist after Flyway migration");
            }

            try (Statement statement = connection.createStatement();
                 ResultSet flywayHistory = statement.executeQuery("SELECT COUNT(*) FROM flyway_schema_history WHERE success = true")) {
                assertTrue(flywayHistory.next());
                assertTrue(flywayHistory.getInt(1) >= 1, "At least one Flyway migration should be applied");
            }
        }
    }

    @Test
    void canInsertAndQueryValidUser() throws Exception {
        User inserted = userRepository.insert(new User("smoke_user", "smoke@example.com", 25));

        assertNotNull(inserted.id(), "Generated user ID should not be null");
        assertEquals(1, countRows("users"), "Users table should contain 1 row");

        User loaded = userRepository.findByEmail("smoke@example.com").orElseThrow();
        assertEquals("smoke_user", loaded.username());
        assertEquals("smoke@example.com", loaded.email());
        assertEquals(25, loaded.age());
    }
}
