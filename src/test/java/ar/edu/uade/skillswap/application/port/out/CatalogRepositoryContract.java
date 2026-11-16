package ar.edu.uade.skillswap.application.port.out;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.uade.skillswap.domain.catalog.CatalogCategory;
import ar.edu.uade.skillswap.domain.catalog.CatalogItem;
import ar.edu.uade.skillswap.domain.catalog.CatalogSkill;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Contrato ejecutable de {@link CatalogRepository} (#37). Cada implementación (en memoria, JPA)
 * lo extiende e implementa {@link #createRepository}, que debe dejar guardado exactamente el árbol
 * recibido. Así el catálogo se reconstruye igual con cualquier almacenamiento.
 */
public abstract class CatalogRepositoryContract {

    protected abstract CatalogRepository createRepository(List<CatalogCategory> roots);

    private CatalogRepository repository;

    @BeforeEach
    void loadCatalog() {
        repository = createRepository(tree());
    }

    @Test
    void reconstruyeElArbolCompletoConSuOrdenYSusNivelesAnidados() {
        assertEquals(describe(tree()), describe(repository.findRootCategories()));
    }

    @Test
    void cadaLecturaDevuelveUnArbolNuevoQueNoModificaElGuardado() {
        CatalogCategory read = repository.findRootCategories().get(0);
        read.add(new CatalogCategory("agregada", "Agregada afuera"));

        assertEquals(describe(tree()), describe(repository.findRootCategories()));
    }

    @Test
    void encuentraUnaCategoriaDeCualquierNivelConElNombreRegistrado() {
        assertEquals(Optional.of("Tecnología"), repository.findCategoryName("Tecnología"));
        assertEquals(Optional.of("Arquitectura de software"), repository.findCategoryName(" ARQUITECTURA DE SOFTWARE "));
        assertEquals(Optional.of("Diseño"), repository.findCategoryName("diseño"));
    }

    @Test
    void unaHabilidadOUnNombreDesconocidoNoSonCategorias() {
        assertTrue(repository.findCategoryName("Java").isEmpty());
        assertTrue(repository.findCategoryName("Astrología").isEmpty());
    }

    /** Tres niveles, hojas en distintos niveles y una raíz sin hijos. */
    protected static List<CatalogCategory> tree() {
        CatalogCategory architecture = new CatalogCategory("arquitectura-software", "Arquitectura de software")
                .add(new CatalogSkill("clean-code", "Clean Code"));
        CatalogCategory development = new CatalogCategory("desarrollo-software", "Desarrollo de software")
                .add(architecture)
                .add(new CatalogSkill("java", "Java"))
                .add(new CatalogSkill("sql", "SQL"));
        CatalogCategory technology = new CatalogCategory("tecnologia", "Tecnología").add(development);
        CatalogCategory design = new CatalogCategory("diseno", "Diseño").add(new CatalogSkill("ux", "UX"));
        return List.of(technology, design, new CatalogCategory("negocios", "Negocios"));
    }

    /**
     * El árbol como texto, en preorden: una línea por elemento con su sangría, tipo, id y nombre.
     * Las entidades del Composite no definen igualdad, así que se comparan así.
     */
    public static List<String> describe(List<CatalogCategory> roots) {
        return roots.stream().flatMap(CatalogItem::stream)
                .map(item -> "  ".repeat(item.depth()) + (item instanceof CatalogSkill ? "habilidad " : "categoría ")
                        + item.id() + " " + item.name())
                .toList();
    }
}
