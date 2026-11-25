package com.example.dbtesting.model;

/**
 * Represents a user record in the user registration system.
 */
public record User(Integer id, String username, String email, Integer age) {

    /**
     * Creates a new User without an assigned database ID.
     */
    public User(String username, String email, Integer age) {
        this(null, username, email, age);
    }
}
