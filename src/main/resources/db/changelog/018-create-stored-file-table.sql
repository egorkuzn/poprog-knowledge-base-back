--liquibase formatted sql

--changeset poprog:create-stored-file-table
create table if not exists stored_file (
    id uuid primary key,
    category text not null,
    original_filename text not null,
    stored_filename text not null,
    content_type text not null,
    size_bytes bigint not null,
    sha256 text not null,
    content bytea not null,
    created_at timestamptz not null default now()
);

create unique index if not exists ux_stored_file_category_sha256 on stored_file (category, sha256);
create unique index if not exists ux_stored_file_category_stored_filename on stored_file (category, stored_filename);

