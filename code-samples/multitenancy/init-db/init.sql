CREATE SEQUENCE IF NOT EXISTS users_seq START 1;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT DEFAULT nextval('users_seq') PRIMARY KEY,
    name VARCHAR(255),
    email VARCHAR(255)
);

INSERT INTO users (id, name, email) VALUES (1, 'admin', 'admin@admin.com');
INSERT INTO users (id, name, email) VALUES (2, 'user','user@user.com');