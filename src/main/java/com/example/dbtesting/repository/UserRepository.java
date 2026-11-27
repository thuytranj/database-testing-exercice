package com.example.dbtesting.repository;

import com.example.dbtesting.model.User;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC repository for managing {@link User} records in PostgreSQL.
 */
public class UserRepository {

    private final String jdbcUrl;
    private final String dbUsername;
    private final String dbPassword;

    public UserRepository(String jdbcUrl, String dbUsername, String dbPassword) {
        this.jdbcUrl = jdbcUrl;
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
    }

    /**
     * Inserts a new user into the {@code users} table and returns the persisted user with its generated ID.
     */
    public User insert(User user) throws SQLException {
        String sql = "INSERT INTO users (username, email, age) VALUES (?, ?, ?)";
        try (Connection connection = DriverManager.getConnection(jdbcUrl, dbUsername, dbPassword);
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, user.username());
            statement.setString(2, user.email());
            if (user.age() == null) {
                statement.setNull(3, Types.INTEGER);
            } else {
                statement.setInt(3, user.age());
            }

            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Expected generated ID after inserting user, but none was returned.");
                }
                int generatedId = keys.getInt(1);
                return new User(generatedId, user.username(), user.email(), user.age());
            }
        }
    }

    /**
     * Finds a user by email address.
     */
    public Optional<User> findByEmail(String email) throws SQLException {
        String sql = "SELECT id, username, email, age FROM users WHERE email = ?";
        try (Connection connection = DriverManager.getConnection(jdbcUrl, dbUsername, dbPassword);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
                return Optional.empty();
            }
        }
    }

    /**
     * Returns all users ordered by ID ascending.
     */
    public List<User> findAll() throws SQLException {
        String sql = "SELECT id, username, email, age FROM users ORDER BY id ASC";
        List<User> users = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(jdbcUrl, dbUsername, dbPassword);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                users.add(mapRow(resultSet));
            }
        }
        return users;
    }

    /**
     * Updates a user's age by email address.
     *
     * @return {@code true} if a row was updated, {@code false} otherwise
     */
    public boolean updateAgeByEmail(String email, int newAge) throws SQLException {
        String sql = "UPDATE users SET age = ? WHERE email = ?";
        try (Connection connection = DriverManager.getConnection(jdbcUrl, dbUsername, dbPassword);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, newAge);
            statement.setString(2, email);
            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Deletes a user by email address.
     *
     * @return {@code true} if a row was deleted, {@code false} otherwise
     */
    public boolean deleteByEmail(String email) throws SQLException {
        String sql = "DELETE FROM users WHERE email = ?";
        try (Connection connection = DriverManager.getConnection(jdbcUrl, dbUsername, dbPassword);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, email);
            return statement.executeUpdate() > 0;
        }
    }

    private User mapRow(ResultSet resultSet) throws SQLException {
        return new User(
                resultSet.getInt("id"),
                resultSet.getString("username"),
                resultSet.getString("email"),
                resultSet.getInt("age")
        );
    }
}
