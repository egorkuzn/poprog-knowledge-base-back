--liquibase formatted sql

--changeset codex:014-create-favorite-item-table
CREATE TABLE favorite_item
(
    id         BIGSERIAL PRIMARY KEY,
    user_sub   VARCHAR(128)                NOT NULL,
    item_type  VARCHAR(32)                 NOT NULL,
    item_id    VARCHAR(128)                NOT NULL,
    title      VARCHAR(255)                NOT NULL,
    link       VARCHAR(2048),
    created_at TIMESTAMPTZ                 NOT NULL DEFAULT now(),
    UNIQUE (user_sub, item_type, item_id)
);

CREATE INDEX idx_favorite_item_user_sub_created_at
    ON favorite_item (user_sub, created_at DESC);
