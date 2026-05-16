-- liquibase formatted sql

-- changeset poprog:021-add-material-kind-to-lab19-news-item
ALTER TABLE lab19_news_item
    ADD COLUMN material_kind TEXT NOT NULL DEFAULT 'NEWS';

CREATE INDEX idx_lab19_news_item_material_kind ON lab19_news_item(material_kind);

