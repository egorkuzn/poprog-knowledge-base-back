ALTER TABLE publication
    ADD COLUMN IF NOT EXISTS pdf_text text;

ALTER TABLE student_work
    ADD COLUMN IF NOT EXISTS pdf_text text;
