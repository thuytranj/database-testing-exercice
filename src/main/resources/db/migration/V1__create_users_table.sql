CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(20) NOT NULL CHECK (char_length(username) BETWEEN 3 AND 20),
    email VARCHAR(100) NOT NULL UNIQUE,
    age INTEGER NOT NULL CHECK (age >= 18)
);
