-- liquibase formatted sql

-- changeset poprog:021-add-kb-entity-refs-to-lab19-news-item
ALTER TABLE lab19_news_item
    ADD COLUMN kb_publication_id BIGINT,
    ADD COLUMN kb_student_work_id BIGINT;

CREATE INDEX idx_lab19_news_item_kb_publication_id ON lab19_news_item(kb_publication_id);
CREATE INDEX idx_lab19_news_item_kb_student_work_id ON lab19_news_item(kb_student_work_id);

