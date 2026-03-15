--liquibase formatted sql

--changeset codex:001-create-publication-table
CREATE TABLE publication (
    id BIGSERIAL PRIMARY KEY,
    publication_year INTEGER NOT NULL,
    authors TEXT NOT NULL,
    theme TEXT NOT NULL,
    published TEXT NOT NULL,
    link TEXT NOT NULL DEFAULT ''
);
