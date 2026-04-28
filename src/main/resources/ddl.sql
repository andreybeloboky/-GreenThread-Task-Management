CREATE TABLE tasks (
                       id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       title VARCHAR NOT NULL UNIQUE,
                       description TEXT NOT NULL,
                       status VARCHAR NOT NULL DEFAULT 'PENDING',
                       duedate DATE NOT NULL,
                       CONSTRAINT title_length_check CHECK (char_length(title) BETWEEN 5 AND 100),
                       CONSTRAINT description_length_check CHECK (char_length(description) <= 500),
                       CONSTRAINT duedate_in_future CHECK (duedate > current_date)
);
