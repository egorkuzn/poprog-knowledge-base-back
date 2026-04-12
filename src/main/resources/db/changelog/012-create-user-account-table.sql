--liquibase formatted sql

--changeset codex:012-create-user-account-table
CREATE TABLE user_account
(
    id           BIGSERIAL PRIMARY KEY,
    keycloak_sub VARCHAR(128)                NOT NULL UNIQUE,
    name         VARCHAR(120)                NOT NULL,
    email        VARCHAR(254)                NOT NULL,
    created_at   TIMESTAMPTZ                 NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ                 NOT NULL DEFAULT now()
);

CREATE INDEX idx_user_account_email
    ON user_account (email);
