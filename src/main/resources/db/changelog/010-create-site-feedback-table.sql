--liquibase formatted sql

--changeset codex:010-create-site-feedback-table
CREATE TABLE site_feedback
(
    id          BIGSERIAL PRIMARY KEY,
    helpful     BOOLEAN      NOT NULL,
    source      VARCHAR(255),
    comment     TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
