package com.example.dbtesting;

import com.example.dbtesting.model.User;
import com.example.dbtesting.support.BaseDatabaseTest;
import net.datafaker.Faker;
import org.dbunit.dataset.Column;
import org.dbunit.dataset.DefaultTable;
import org.dbunit.dataset.datatype.DataType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Task 3: Generate 10 reproducible, valid users with Datafaker and verify persistence with DBUnit.
 */
class DataGenerationTest extends BaseDatabaseTest {

    private static final int USER_COUNT = 10;
    private static final long FAKER_SEED = 42L;

    @Test
    void shouldGenerateAndPersistTenValidUniqueUsers() throws Exception {
        Faker faker = new Faker(new Random(FAKER_SEED));
        List<User> generatedUsers = new ArrayList<>();
        Set<String> seenEmails = new HashSet<>();

        for (int i = 0; i < USER_COUNT; i++) {
            String username = generateValidUsername(faker, i);

            // TODO 3.1: Generate a unique email address using Datafaker (hint: combine iteration index with faker.internet().emailAddress())
            String email = null;
            if (email == null) {
                fail("TODO 3.1: Generate unique email addresses using Datafaker.");
            }

            // TODO 3.2: Generate a valid age within the business range using Datafaker (hint: faker.number().numberBetween(minInclusive, maxExclusive))
            int age = -1;
            if (age < 0) {
                fail("TODO 3.2: Generate ages within the valid business range using Datafaker.");
            }

            User user = new User(username, email, age);
            validateGeneratedUser(user, seenEmails);
            generatedUsers.add(user);
        }

        for (User user : generatedUsers) {
            User persisted = userRepository.insert(user);
            assertNotNull(persisted.id(), "Persisted user should receive a generated ID");
        }

        assertEquals(USER_COUNT, countRows("users"), "Exactly 10 users should be persisted in the database");

        DefaultTable expectedTable = buildExpectedUsersTable(generatedUsers);
        assertUsersTableEquals(expectedTable, new String[]{"id"}, "email");
    }

    private String generateValidUsername(Faker faker, int index) {
        String raw = faker.internet().username().replaceAll("[^a-zA-Z0-9_]", "");
        if (raw.length() < 3) {
            raw = "user" + index;
        }
        String suffix = "_" + index;
        int maxPrefixLength = 20 - suffix.length();
        String prefix = raw.length() > maxPrefixLength ? raw.substring(0, maxPrefixLength) : raw;
        return prefix + suffix;
    }

    private void validateGeneratedUser(User user, Set<String> seenEmails) {
        assertNotNull(user.username(), "Username must not be null");
        assertTrue(
                user.username().length() >= 3 && user.username().length() <= 20,
                "Username length must be between 3 and 20: " + user.username()
        );

        assertNotNull(user.email(), "Email must not be null");
        assertFalse(user.email().isBlank(), "Email must not be blank");
        assertTrue(user.email().length() <= 100, "Email length must be <= 100: " + user.email());
        assertTrue(user.email().contains("@"), "Email must contain '@': " + user.email());
        assertTrue(seenEmails.add(user.email()), "Email must be unique: " + user.email());

        assertNotNull(user.age(), "Age must not be null");
        assertTrue(
                user.age() >= 18 && user.age() <= 60,
                "Age must be between 18 and 60 inclusive, got: " + user.age()
        );
    }

    private DefaultTable buildExpectedUsersTable(List<User> users) throws Exception {
        Column[] columns = {
                new Column("id", DataType.INTEGER),
                new Column("username", DataType.VARCHAR),
                new Column("email", DataType.VARCHAR),
                new Column("age", DataType.INTEGER)
        };
        DefaultTable table = new DefaultTable("users", columns);
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            table.addRow(new Object[]{i + 1, user.username(), user.email(), user.age()});
        }
        return table;
    }
}
