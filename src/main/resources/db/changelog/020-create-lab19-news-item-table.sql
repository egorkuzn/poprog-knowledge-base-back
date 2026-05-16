-- liquibase formatted sql

-- changeset poprog:020-create-lab19-news-item-table
CREATE TABLE lab19_news_item (
    id BIGSERIAL PRIMARY KEY,
    title TEXT NOT NULL,
    source_url TEXT NOT NULL,
    source_page TEXT NOT NULL,
    year INTEGER,
    content_type TEXT,
    status TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_lab19_news_item_source_url ON lab19_news_item(source_url);
CREATE INDEX idx_lab19_news_item_year ON lab19_news_item(year);
