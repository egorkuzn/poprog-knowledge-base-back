--liquibase formatted sql

--changeset codex:005-add-document-link-to-student-work
ALTER TABLE student_work
ADD COLUMN document_link TEXT;
