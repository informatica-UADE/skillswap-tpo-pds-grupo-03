package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.application.port.out.CatalogRepository;
import ar.edu.uade.skillswap.domain.catalog.CatalogCategory;
import ar.edu.uade.skillswap.domain.catalog.CatalogItem;
import ar.edu.uade.skillswap.domain.catalog.CatalogSkill;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Catálogo en memoria para el modo demo y los tests. Devuelve copias del árbol, igual que JPA
 * reconstruye uno nuevo en cada lectura, así nadie modifica el catálogo desde afuera.
 */
public final class InMemoryCatalogRepository implements CatalogRepository {
    private final List<CatalogCategory> roots;

    public InMemoryCatalogRepository(List<CatalogCategory> roots) {
        this.roots = List.copyOf(Objects.requireNonNull(roots, "Falta el catálogo"));
    }

    @Override
    public List<CatalogCategory> findRootCategories() {
        return roots.stream().map(root -> (CatalogCategory) copy(root)).toList();
    }

    @Override
    public Optional<String> findCategoryName(String name) {
        Objects.requireNonNull(name, "Falta el nombre de la categoría");
        return findCategory(name).map(CatalogItem::name);
    }

    /** Rama del catálogo, con sus descendientes; la usa la búsqueda en memoria. */
    Optional<CatalogCategory> findCategory(String name) {
        return roots.stream().map(root -> root.findCategory(name)).flatMap(Optional::stream).findFirst();
    }

    private static CatalogItem copy(CatalogItem item) {
        if (item instanceof CatalogSkill skill) return new CatalogSkill(skill.id(), skill.name());
        CatalogCategory copy = new CatalogCategory(item.id(), item.name());
        item.children().forEach(child -> copy.add(copy(child)));
        return copy;
    }
}
