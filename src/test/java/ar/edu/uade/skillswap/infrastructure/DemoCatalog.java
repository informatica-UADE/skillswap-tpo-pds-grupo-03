package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.domain.catalog.CatalogCategory;
import ar.edu.uade.skillswap.domain.catalog.CatalogSkill;
import java.util.List;

/**
 * Catálogo de demostración para las pruebas sobre H2 (#37), donde no corre Flyway. Es el mismo árbol
 * que carga la migración {@code V6__create_catalog.sql} en MySQL; {@code MySqlCatalogRepositoryTest}
 * verifica que no se separen. Cada llamada arma un árbol nuevo, porque las categorías del Composite son
 * mutables.
 */
public final class DemoCatalog {
    private DemoCatalog() {}

    public static List<CatalogCategory> tree() {
        CatalogCategory architecture = new CatalogCategory("arquitectura-software", "Arquitectura de software")
                .add(skill("arquitectura", "Arquitectura"))
                .add(skill("clean-architecture", "Clean Architecture"))
                .add(skill("clean-code", "Clean Code"));
        CatalogCategory development = new CatalogCategory("desarrollo-software", "Desarrollo de software")
                .add(architecture)
                .add(skill("java", "Java"))
                .add(skill("sql", "SQL"))
                .add(skill("backend", "Backend"));
        CatalogCategory career = new CatalogCategory("carrera-tecnologia", "Carrera en tecnología")
                .add(skill("entrevistas", "Entrevistas"))
                .add(skill("carrera", "Carrera"));
        CatalogCategory technology = new CatalogCategory("tecnologia", "Tecnología")
                .add(development)
                .add(career);

        CatalogCategory ux = new CatalogCategory("diseno-ux", "Diseño UX")
                .add(skill("ux", "UX"))
                .add(skill("portfolio", "Portfolio"));
        CatalogCategory design = new CatalogCategory("diseno", "Diseño").add(ux);

        CatalogCategory creativity = new CatalogCategory("creatividad", "Creatividad")
                .add(skill("fotografia", "Fotografía"))
                .add(skill("contenido", "Contenido"));
        CatalogCategory business = new CatalogCategory("negocios", "Negocios")
                .add(skill("liderazgo", "Liderazgo"))
                .add(skill("equipos", "Equipos"));
        CatalogCategory productivity = new CatalogCategory("productividad", "Productividad")
                .add(skill("excel", "Excel"))
                .add(skill("datos", "Datos"));
        return List.of(technology, design, creativity, business, productivity);
    }

    private static CatalogSkill skill(String id, String name) {
        return new CatalogSkill(id, name);
    }
}
