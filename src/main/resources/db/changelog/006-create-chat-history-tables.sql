CREATE TABLE chat_conversation
(
    id         UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE chat_message
(
    id         BIGSERIAL PRIMARY KEY,
    chat_id    UUID         NOT NULL REFERENCES chat_conversation (id) ON DELETE CASCADE,
    role       VARCHAR(16)  NOT NULL,
    content    TEXT         NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_chat_message_chat_id_created_at_id
    ON chat_message (chat_id, created_at, id);
