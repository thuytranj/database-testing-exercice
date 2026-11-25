package com.example.dbtesting;

import com.example.dbtesting.model.User;
import com.example.dbtesting.support.BaseDatabaseTest;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Task 2: Execute CRUD operations on seeded data and verify final database state against expected-users.xml.
 */
class DBUnitExpectedStateTest extends BaseDatabaseTest {

    private static final String INITIAL_DATASET = "datasets/initial-users.xml";
    private static final String EXPECTED_DATASET = "datasets/expected-users.xml";

    @Test
    void shouldMatchExpectedDatabaseStateAfterCrudOperations() throws Exception {
        IDataSet initialDataSet = loadXmlDataSet(INITIAL_DATASET);
        assertTrue(
                Arrays.asList(initialDataSet.getTableNames()).contains("users"),
                "Task 1 prerequisite missing: datasets/initial-users.xml must define <users .../> rows."
        );

        seedXmlDataSet(INITIAL_DATASET);
        verifyInitialSeededUsers();

        // Preimplemented CRUD operations for students to trace and predict:
        // 1. Delete Bob
        assertTrue(
                userRepository.deleteByEmail("bob@example.com"),
                "Expected Bob (bob@example.com) to be deleted from initial dataset"
        );

        // 2. Update Alice's age to 22
        assertTrue(
                userRepository.updateAgeByEmail("alice@example.com", 22),
                "Expected Alice (alice@example.com) age to be updated to 22"
        );

        // 3. Insert David (david@example.com, age 28)
        User insertedDavid = userRepository.insert(new User("david", "david@example.com", 28));
        assertNotNull(insertedDavid.id(), "Inserted user David should receive a generated ID");

        IDataSet expectedDataSet = loadXmlDataSet(EXPECTED_DATASET);
        assertTrue(
                Arrays.asList(expectedDataSet.getTableNames()).contains("users"),
                "TODO 2.1: datasets/expected-users.xml must define expected <users .../> rows."
        );

        ITable expectedTable = expectedDataSet.getTable("users");
        assertUsersTableEquals(expectedTable, new String[]{"id"}, "email");
    }

    private void verifyInitialSeededUsers() throws Exception {
        List<User> expectedInitial = List.of(
                new User(1, "alice", "alice@example.com", 20),
                new User(2, "bob", "bob@example.com", 25),
                new User(3, "charlie", "charlie@example.com", 30)
        );
        List<User> actualInitial = userRepository.findAll();
        assertEquals(
                expectedInitial.size(),
                actualInitial.size(),
                "Task 1 prerequisite failed: datasets/initial-users.xml must seed exactly 3 users."
        );
        for (int i = 0; i < expectedInitial.size(); i++) {
            User expected = expectedInitial.get(i);
            User actual = actualInitial.get(i);
            assertEquals(expected.id(), actual.id(), "Initial dataset id mismatch; complete Task 1 first.");
            assertEquals(expected.username(), actual.username(), "Initial dataset username mismatch; complete Task 1 first.");
            assertEquals(expected.email(), actual.email(), "Initial dataset email mismatch; complete Task 1 first.");
            assertEquals(expected.age(), actual.age(), "Initial dataset age mismatch; complete Task 1 first.");
        }
    }
}
