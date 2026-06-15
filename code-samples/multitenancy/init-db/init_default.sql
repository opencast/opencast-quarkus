CREATE SEQUENCE IF NOT EXISTS users_seq START 1;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT DEFAULT nextval('users_seq') PRIMARY KEY,
    username VARCHAR(255),
    password VARCHAR(255),
    role VARCHAR(255)
);

INSERT INTO users (id, username, password, role) VALUES (1, 'default_admin', '$2a$10$Uc.SZ0hvGJQlYdsAp7be1.lFjmOnc7aAr4L0YY3/VN3oK.F8zJHRG', 'admin');
INSERT INTO users (id, username, password, role) VALUES (2, 'default_user', '$2a$10$Uc.SZ0hvGJQlYdsAp7be1.lFjmOnc7aAr4L0YY3/VN3oK.F8zJHRG', 'user');
