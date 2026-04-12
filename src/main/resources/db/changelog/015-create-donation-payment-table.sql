--liquibase formatted sql

--changeset codex:015-create-donation-payment-table
CREATE TABLE donation_payment
(
    id                  UUID PRIMARY KEY,
    user_sub            VARCHAR(128),
    amount              NUMERIC(12, 2)              NOT NULL,
    currency            CHAR(3)                     NOT NULL,
    status              VARCHAR(32)                 NOT NULL,
    source              VARCHAR(255),
    message             VARCHAR(1000),
    provider_payment_id VARCHAR(128) UNIQUE,
    confirmation_url    VARCHAR(4096),
    return_url          VARCHAR(2048)               NOT NULL,
    created_at          TIMESTAMPTZ                 NOT NULL,
    updated_at          TIMESTAMPTZ                 NOT NULL,
    paid_at             TIMESTAMPTZ
);

CREATE INDEX idx_donation_payment_user_sub_created_at
    ON donation_payment (user_sub, created_at DESC);

CREATE INDEX idx_donation_payment_status_created_at
    ON donation_payment (status, created_at DESC);
