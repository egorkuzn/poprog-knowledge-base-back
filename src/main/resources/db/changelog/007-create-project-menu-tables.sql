CREATE TABLE project_menu_section
(
    id          BIGSERIAL PRIMARY KEY,
    hash        VARCHAR(128) NOT NULL UNIQUE,
    title       VARCHAR(255) NOT NULL,
    description TEXT         NOT NULL,
    cta_title   VARCHAR(255) NOT NULL,
    cta_url     VARCHAR(512) NOT NULL,
    sort_order  INTEGER      NOT NULL
);

CREATE TABLE project_menu_item
(
    id          BIGSERIAL PRIMARY KEY,
    section_id  BIGINT       NOT NULL REFERENCES project_menu_section (id) ON DELETE CASCADE,
    title       VARCHAR(255) NOT NULL,
    description TEXT         NOT NULL,
    url         VARCHAR(512) NOT NULL,
    image_url   VARCHAR(512),
    highlighted BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order  INTEGER      NOT NULL
);

CREATE TABLE project_menu_promo
(
    id          BIGSERIAL PRIMARY KEY,
    section_id  BIGINT       NOT NULL REFERENCES project_menu_section (id) ON DELETE CASCADE,
    title       VARCHAR(255) NOT NULL,
    description TEXT         NOT NULL,
    url         VARCHAR(512) NOT NULL,
    image_url   VARCHAR(512) NOT NULL,
    sort_order  INTEGER      NOT NULL
);

CREATE INDEX idx_project_menu_item_section_order
    ON project_menu_item (section_id, sort_order, id);

CREATE INDEX idx_project_menu_promo_section_order
    ON project_menu_promo (section_id, sort_order, id);
