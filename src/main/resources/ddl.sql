CREATE TABLE tasks
(
    id          INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title       VARCHAR NOT NULL UNIQUE,
    description TEXT    NOT NULL,
    status      VARCHAR NOT NULL DEFAULT 'PENDING',
    duedate     TIMESTAMP WITH TIME ZONE NOT NULL,
    username_id INT NOT NULL,
    CONSTRAINT title_length_check CHECK (char_length(title) BETWEEN 5 AND 100),
    CONSTRAINT description_length_check CHECK (char_length(description) <= 500),
    CONSTRAINT duedate_in_future CHECK (duedate > current_date),
    CONSTRAINT fk_username
        FOREIGN KEY (username_id)
            REFERENCES users (id)
            ON DELETE CASCADE
);

CREATE TABLE subtasks
(
    id         INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    task_id    INT     NOT NULL,
    title      VARCHAR NOT NULL UNIQUE,
    completed  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_task
        FOREIGN KEY (task_id)
            REFERENCES tasks (id)
            ON DELETE CASCADE
);

CREATE TABLE users(
                      id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                      username VARCHAR NOT NULL UNIQUE,
                      password VARCHAR NOT NULL
)