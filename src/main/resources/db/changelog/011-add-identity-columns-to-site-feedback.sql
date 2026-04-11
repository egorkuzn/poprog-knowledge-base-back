--liquibase formatted sql

--changeset codex:011-add-identity-columns-to-site-feedback
ALTER TABLE site_feedback
    ADD COLUMN user_name VARCHAR(255),
    ADD COLUMN user_email VARCHAR(254),
    ADD COLUMN user_agent VARCHAR(512),
    ADD COLUMN ip_address VARCHAR(64);
