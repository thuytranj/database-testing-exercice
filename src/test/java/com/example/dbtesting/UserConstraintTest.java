package com.example.dbtesting;

import com.example.dbtesting.model.User;
import com.example.dbtesting.support.BaseDatabaseTest;
import org.dbunit.dataset.ITable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Task 4: Verify database constraints using JUnit 5 parameterized tests and Boundary Value Analysis (BVA).
 */
class UserConstraintTest extends BaseDatabaseTest {

    private static final String EXISTING_DATASET = "datasets/existing-users.xml";
    private static final String EXISTING_EMAIL = "existing@example.com";
    private static final String EXPECTED_BVA_SIGNATURE =
            "d348b18815ae0c1ee20db594a6c16addc17290a703fef38166d0f34a8ba9c0ac";

    @BeforeEach
    void seedExistingData() throws Exception {
        seedXmlDataSet(EXISTING_DATASET);
    }

    @ParameterizedTest(name = "{0}: age={1}, email={2} -> {3}")
    @CsvSource({
            "TC01, 25, tc01@example.com, ACCEPT",
            // TODO 4.1: Determine boundary inputs using Boundary Value Analysis (BVA) and replace -1
            // TODO 4.2: Specify expected outcomes (ACCEPT or REJECT) and replace TODO
            "TC02, -1, tc02@example.com, TODO",
            "TC03, -1, tc03@example.com, TODO",
            "TC04, -1, tc04@example.com, TODO",
            "TC05, -1, tc05@example.com, TODO",
            "TC06, 30, existing@example.com, REJECT",
            "TC07, 30, NULL, REJECT"
    })
    void shouldEnforceUserConstraints(String testId, int age, String rawEmail, String expectedOutcome)
            throws Exception {
        if (age < 0 || "TODO".equalsIgnoreCase(expectedOutcome)) {
            fail(testId + ": TODO 4.1 / 4.2 not completed. Replace placeholder age and outcome in @CsvSource.");
        }

        String email = "NULL".equalsIgnoreCase(rawEmail) ? null : rawEmail;
        User candidate = new User("user_" + testId.toLowerCase(), email, age);

        if ("ACCEPT".equalsIgnoreCase(expectedOutcome)) {
            User inserted = userRepository.insert(candidate);
            assertNotNull(inserted.id(), testId + ": Generated ID should not be null");
            assertEquals(2, countRows("users"), testId + ": Valid user should be persisted alongside existing user");

            User stored = userRepository.findByEmail(email).orElseThrow(
                    () -> new AssertionError(testId + ": Inserted user not found by email: " + email)
            );
            assertEquals(candidate.username(), stored.username());
            assertEquals(candidate.email(), stored.email());
            assertEquals(candidate.age(), stored.age());
        } else if ("REJECT".equalsIgnoreCase(expectedOutcome)) {
            SQLException exception = assertThrows(
                    SQLException.class,
                    () -> userRepository.insert(candidate),
                    () -> testId + ": Expected database to reject invalid user (age=" + age + ", email=" + email + ")"
            );

            String expectedSqlState = resolveExpectedSqlState(email);
            assertEquals(
                    expectedSqlState,
                    exception.getSQLState(),
                    testId + ": Unexpected PostgreSQL SQLSTATE"
            );

            assertEquals(1, countRows("users"), testId + ": Rejected insert must not add any row");
            ITable expectedExisting = loadXmlDataSet(EXISTING_DATASET).getTable("users");
            assertUsersTableEquals(expectedExisting, null, "id");
        } else {
            fail(testId + ": Expected outcome must be ACCEPT or REJECT, got: " + expectedOutcome);
        }
    }

    @Test
    void verifyRequiredBoundaryTestCasesPresent() throws Exception {
        Method method = UserConstraintTest.class.getDeclaredMethod(
                "shouldEnforceUserConstraints",
                String.class, int.class, String.class, String.class
        );
        CsvSource csvSource = method.getAnnotation(CsvSource.class);
        assertNotNull(csvSource, "@CsvSource must be present on shouldEnforceUserConstraints");

        Set<String> targetIds = Set.of("TC02", "TC03", "TC04", "TC05");
        List<String> bvaEntries = new ArrayList<>();

        for (String row : csvSource.value()) {
            String[] parts = row.split(",");
            if (parts.length < 4) {
                continue;
            }
            String id = parts[0].trim();
            String ageStr = parts[1].trim();
            String outcome = parts[3].trim().toUpperCase();

            if (targetIds.contains(id)) {
                int parsedAge = Integer.parseInt(ageStr);
                if (parsedAge < 0 || "TODO".equals(outcome)) {
                    fail("TODO 4.1 / 4.2: Complete all boundary value test cases (TC02-TC05) in @CsvSource.");
                }
                bvaEntries.add(parsedAge + ":" + outcome);
            }
        }

        assertEquals(
                4,
                bvaEntries.size(),
                "Expected all 4 boundary test cases (TC02-TC05) to be present in @CsvSource."
        );

        Collections.sort(bvaEntries);
        String actualSignature = sha256Hex(String.join(",", bvaEntries));
        assertEquals(
                EXPECTED_BVA_SIGNATURE,
                actualSignature,
                "Incomplete or incorrect BVA test cases in @CsvSource. Ensure TC02-TC05 cover all lower and upper boundary values with accurate expected outcomes."
        );
    }

    private String resolveExpectedSqlState(String email) {
        if (email == null) {
            return "23502"; // NOT NULL violation
        }
        if (EXISTING_EMAIL.equals(email)) {
            return "23505"; // UNIQUE violation
        }
        return "23514"; // CHECK constraint violation
    }

    private String sha256Hex(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}
