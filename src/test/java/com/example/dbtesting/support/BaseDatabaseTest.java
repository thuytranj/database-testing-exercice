package com.example.dbtesting.support;

import com.example.dbtesting.repository.UserRepository;
import org.dbunit.Assertion;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.SortedTable;
import org.dbunit.dataset.filter.DefaultColumnFilter;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.ext.postgresql.PostgresqlDataTypeFactory;
import org.dbunit.operation.DatabaseOperation;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Base class for database tests.
 * Manages a shared PostgreSQL Testcontainer, Flyway migrations, per-test database reset,
 * and DBUnit helper utilities.
 */
public abstract class BaseDatabaseTest {

    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:17-alpine");

    protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGRES_IMAGE)
            .withDatabaseName("dbtesting")
            .withUsername("testuser")
            .withPassword("testpass");

    static {
        POSTGRES.start();
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .load()
                .migrate();
    }

    protected UserRepository userRepository;

    @BeforeEach
    void resetDatabaseState() throws SQLException {
        userRepository = new UserRepository(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        );
        try (Connection connection = openJdbcConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");
        }
    }

    protected Connection openJdbcConnection() throws SQLException {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        );
    }

    protected IDatabaseConnection openDbUnitConnection() throws Exception {
        Connection jdbcConnection = openJdbcConnection();
        DatabaseConnection dbUnitConnection = new DatabaseConnection(jdbcConnection);
        dbUnitConnection.getConfig().setProperty(
                DatabaseConfig.PROPERTY_DATATYPE_FACTORY,
                new PostgresqlDataTypeFactory()
        );
        return dbUnitConnection;
    }

    protected IDataSet loadXmlDataSet(String resourcePath) throws Exception {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IllegalArgumentException("Dataset resource not found: " + resourcePath);
            }
            return new FlatXmlDataSetBuilder()
                    .setColumnSensing(true)
                    .build(stream);
        }
    }

    protected void seedXmlDataSet(String resourcePath) throws Exception {
        IDataSet dataSet = loadXmlDataSet(resourcePath);
        IDatabaseConnection dbUnitConnection = openDbUnitConnection();
        try {
            DatabaseOperation.CLEAN_INSERT.execute(dbUnitConnection, dataSet);
        } finally {
            dbUnitConnection.close();
        }
        syncUserIdSequence();
    }

    protected void syncUserIdSequence() throws SQLException {
        String sql = "SELECT setval(pg_get_serial_sequence('users', 'id'), COALESCE((SELECT MAX(id) FROM users), 1), (SELECT COUNT(*) > 0 FROM users))";
        try (Connection connection = openJdbcConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    protected int countRows(String tableName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + tableName;
        try (Connection connection = openJdbcConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    protected void assertUsersTableEquals(ITable expectedTable, String[] excludedColumns, String... orderByColumns)
            throws Exception {
        IDatabaseConnection dbUnitConnection = openDbUnitConnection();
        try {
            ITable actualTable = dbUnitConnection.createDataSet().getTable("users");

            ITable filteredExpected = excludedColumns != null && excludedColumns.length > 0
                    ? DefaultColumnFilter.excludedColumnsTable(expectedTable, excludedColumns)
                    : expectedTable;
            ITable filteredActual = excludedColumns != null && excludedColumns.length > 0
                    ? DefaultColumnFilter.excludedColumnsTable(actualTable, excludedColumns)
                    : actualTable;

            if (orderByColumns != null && orderByColumns.length > 0) {
                filteredExpected = new SortedTable(filteredExpected, orderByColumns);
                filteredActual = new SortedTable(filteredActual, orderByColumns);
            }

            Assertion.assertEquals(filteredExpected, filteredActual);
        } finally {
            dbUnitConnection.close();
        }
    }
}
