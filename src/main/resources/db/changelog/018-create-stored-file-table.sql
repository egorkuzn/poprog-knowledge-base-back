-- liquibase formatted sql

-- changeset poprog:018-create-stored-file-table
CREATE TABLE stored_file (
    id UUID PRIMARY KEY,
    category TEXT NOT NULL,
    original_filename TEXT NOT NULL,
    stored_filename TEXT NOT NULL,
    content_type TEXT NOT NULL,
    size_bytes BIGINT NOT NULL,
    sha256 TEXT NOT NULL,
    content BYTEA NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_stored_file_category ON stored_file(category);
CREATE UNIQUE INDEX idx_stored_file_sha256_category ON stored_file(category, sha256);
