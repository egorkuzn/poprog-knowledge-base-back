--liquibase formatted sql

--changeset codex:016-create-product-metric-event-table
CREATE TABLE product_metric_event
(
    id               BIGSERIAL PRIMARY KEY,
    event_type       VARCHAR(64)                  NOT NULL,
    route            VARCHAR(255)                 NOT NULL,
    referrer         VARCHAR(2048),
    session_id       VARCHAR(128)                 NOT NULL,
    user_key         VARCHAR(128)                 NOT NULL,
    timestamp_client TIMESTAMPTZ                  NOT NULL,
    timestamp_server TIMESTAMPTZ                  NOT NULL,
    payload_json     TEXT                         NOT NULL DEFAULT '{}'
);

CREATE INDEX idx_product_metric_event_server_time
    ON product_metric_event (timestamp_server ASC);

CREATE INDEX idx_product_metric_event_type_server_time
    ON product_metric_event (event_type, timestamp_server ASC);

CREATE INDEX idx_product_metric_event_user_key_server_time
    ON product_metric_event (user_key, timestamp_server ASC);
