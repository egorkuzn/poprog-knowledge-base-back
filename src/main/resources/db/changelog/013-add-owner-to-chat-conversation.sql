--liquibase formatted sql

--changeset codex:013-add-owner-to-chat-conversation
ALTER TABLE chat_conversation
    ADD COLUMN owner_sub VARCHAR(128);

CREATE INDEX idx_chat_conversation_owner_sub_created_at
    ON chat_conversation (owner_sub, created_at DESC);
