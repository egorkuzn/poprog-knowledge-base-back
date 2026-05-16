--liquibase formatted sql

--changeset codex:019-fix-publication-13-doi
-- The previously stored DOI was incorrect and returned 404. Replace it with the
-- DOI from the journal page (MathNet) and fix year/venue to keep publications
-- page free of dead links.
UPDATE publication
SET
    publication_year = 2020,
    published = 'Моделирование и анализ информационных систем. 2020. Т. 27, № 4. С. 412-427. DOI: 10.18255/1818-1015-2020-4-412-427.',
    link = 'https://doi.org/10.18255/1818-1015-2020-4-412-427'
WHERE id = 13;

