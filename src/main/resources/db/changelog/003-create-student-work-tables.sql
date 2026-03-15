--liquibase formatted sql

--changeset codex:003-create-student-work-tables
CREATE TABLE project_type (
    id BIGSERIAL PRIMARY KEY,
    title TEXT NOT NULL UNIQUE,
    hash TEXT NOT NULL UNIQUE
);

CREATE TABLE student_work (
    id BIGSERIAL PRIMARY KEY,
    project_type_id BIGINT NOT NULL REFERENCES project_type(id) ON DELETE RESTRICT,
    authors TEXT NOT NULL,
    theme TEXT NOT NULL,
    published TEXT NOT NULL
);
