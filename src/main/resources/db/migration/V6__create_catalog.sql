-- Catálogo jerárquico de categorías y habilidades (#37). La jerarquía se guarda con parent_id y
-- con la ruta materializada (path), que permite filtrar una rama completa con un LIKE indexado.
-- Los nombres usan la misma colación que publications.category para comparar sin mayúsculas ni
-- acentos, igual que el dominio y el repositorio en memoria.
CREATE TABLE catalog_categories (
    id VARCHAR(80) PRIMARY KEY,
    parent_id VARCHAR(80) NULL,
    name VARCHAR(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
    path VARCHAR(500) NOT NULL,
    sort_order INT NOT NULL,
    CONSTRAINT uq_catalog_categories_name UNIQUE (name),
    CONSTRAINT uq_catalog_categories_path UNIQUE (path),
    CONSTRAINT fk_catalog_categories_parent
        FOREIGN KEY (parent_id) REFERENCES catalog_categories(id),
    CONSTRAINT chk_catalog_category_not_own_parent
        CHECK (parent_id IS NULL OR parent_id <> id),
    CONSTRAINT chk_catalog_category_order
        CHECK (sort_order >= 0)
);

CREATE TABLE catalog_skills (
    id VARCHAR(80) PRIMARY KEY,
    category_id VARCHAR(80) NOT NULL,
    name VARCHAR(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
    sort_order INT NOT NULL,
    CONSTRAINT uq_catalog_skill_category_name UNIQUE (category_id, name),
    CONSTRAINT fk_catalog_skills_category
        FOREIGN KEY (category_id) REFERENCES catalog_categories(id),
    CONSTRAINT chk_catalog_skill_order
        CHECK (sort_order >= 0),
    INDEX idx_catalog_skills_name (name)
);

-- Mismo árbol que DemoCatalog (modo en memoria); MySqlCatalogRepositoryTest verifica que coincidan.
INSERT INTO catalog_categories (id, parent_id, name, path, sort_order) VALUES
    ('tecnologia', NULL, 'Tecnología', '/tecnologia/', 0),
    ('desarrollo-software', 'tecnologia', 'Desarrollo de software', '/tecnologia/desarrollo-software/', 0),
    ('arquitectura-software', 'desarrollo-software', 'Arquitectura de software', '/tecnologia/desarrollo-software/arquitectura-software/', 0),
    ('carrera-tecnologia', 'tecnologia', 'Carrera en tecnología', '/tecnologia/carrera-tecnologia/', 1),
    ('diseno', NULL, 'Diseño', '/diseno/', 1),
    ('diseno-ux', 'diseno', 'Diseño UX', '/diseno/diseno-ux/', 0),
    ('creatividad', NULL, 'Creatividad', '/creatividad/', 2),
    ('negocios', NULL, 'Negocios', '/negocios/', 3),
    ('productividad', NULL, 'Productividad', '/productividad/', 4);

INSERT INTO catalog_skills (id, category_id, name, sort_order) VALUES
    ('arquitectura', 'arquitectura-software', 'Arquitectura', 0),
    ('clean-architecture', 'arquitectura-software', 'Clean Architecture', 1),
    ('clean-code', 'arquitectura-software', 'Clean Code', 2),
    ('java', 'desarrollo-software', 'Java', 0),
    ('sql', 'desarrollo-software', 'SQL', 1),
    ('backend', 'desarrollo-software', 'Backend', 2),
    ('entrevistas', 'carrera-tecnologia', 'Entrevistas', 0),
    ('carrera', 'carrera-tecnologia', 'Carrera', 1),
    ('ux', 'diseno-ux', 'UX', 0),
    ('portfolio', 'diseno-ux', 'Portfolio', 1),
    ('fotografia', 'creatividad', 'Fotografía', 0),
    ('contenido', 'creatividad', 'Contenido', 1),
    ('liderazgo', 'negocios', 'Liderazgo', 0),
    ('equipos', 'negocios', 'Equipos', 1),
    ('excel', 'productividad', 'Excel', 0),
    ('datos', 'productividad', 'Datos', 1);

-- Publicaciones existentes: un borrador sin categoría pasa a NULL, y una categoría que no está en
-- el catálogo se conserva como categoría raíz para no perder datos ni romper la clave foránea.
ALTER TABLE publications
    MODIFY category VARCHAR(150)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL;

UPDATE publications SET category = NULL WHERE TRIM(category) = '';

INSERT INTO catalog_categories (id, parent_id, name, path, sort_order)
SELECT CONCAT('anterior-', LEFT(SHA2(legacy.category, 256), 16)), NULL, legacy.category,
       CONCAT('/anterior-', LEFT(SHA2(legacy.category, 256), 16), '/'), 100
FROM (SELECT DISTINCT p.category FROM publications p
      WHERE p.category IS NOT NULL
        AND NOT EXISTS (SELECT 1 FROM catalog_categories c WHERE c.name = p.category)) legacy;

ALTER TABLE publications
    ADD CONSTRAINT fk_publications_category
        FOREIGN KEY (category) REFERENCES catalog_categories(name);
