package com.example.dbtesting;

import com.example.dbtesting.model.User;
import com.example.dbtesting.support.BaseDatabaseTest;
import org.dbunit.dataset.IDataSet;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Task 1: Seed initial user records from a DBUnit Flat XML Dataset and verify them independently.
 */
class DBUnitSeedTest extends BaseDatabaseTest {

    private static final String INITIAL_DATASET = "datasets/initial-users.xml";

    @Test
    void shouldSeedInitialUsersFromFlatXmlDataset() throws Exception {
        IDataSet dataSet = loadXmlDataSet(INITIAL_DATASET);
        assertTrue(
                Arrays.asList(dataSet.getTableNames()).contains("users"),
                "TODO 1.1: datasets/initial-users.xml must define <users .../> rows."
        );

        seedXmlDataSet(INITIAL_DATASET);

        List<User> expectedUsers = List.of(
                new User(1, "alice", "alice@example.com", 20),
                new User(2, "bob", "bob@example.com", 25),
                new User(3, "charlie", "charlie@example.com", 30)
        );

        assertEquals(
                expectedUsers.size(),
                countRows("users"),
                "Expected 3 users in 'users' table after seeding datasets/initial-users.xml"
        );

        List<User> actualUsers = userRepository.findAll();
        assertEquals(expectedUsers.size(), actualUsers.size(), "Unexpected number of users returned by findAll()");

        for (int i = 0; i < expectedUsers.size(); i++) {
            User expected = expectedUsers.get(i);
            User actual = actualUsers.get(i);
            assertEquals(expected.id(), actual.id(), "Mismatch on id at row " + (i + 1));
            assertEquals(expected.username(), actual.username(), "Mismatch on username for id=" + expected.id());
            assertEquals(expected.email(), actual.email(), "Mismatch on email for id=" + expected.id());
            assertEquals(expected.age(), actual.age(), "Mismatch on age for id=" + expected.id());
        }
    }
}
