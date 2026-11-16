package ar.edu.uade.skillswap.application.service;

import ar.edu.uade.skillswap.application.port.out.CatalogRepository;
import ar.edu.uade.skillswap.domain.catalog.CatalogCategory;
import ar.edu.uade.skillswap.domain.catalog.CatalogItem;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Doble del puerto del catálogo para las pruebas de Application: responde desde un árbol fijo. */
final class FixedCatalog implements CatalogRepository {
    private final List<CatalogCategory> roots;
    int reads;

    FixedCatalog(CatalogCategory... roots) {
        this.roots = List.of(roots);
    }

    /** Catálogo con categorías raíz sin hijos, suficiente para validar nombres. */
    static FixedCatalog withRoots(String... names) {
        return new FixedCatalog(Arrays.stream(names)
                .map(name -> new CatalogCategory(name.toLowerCase(Locale.ROOT), name)).toArray(CatalogCategory[]::new));
    }

    @Override
    public List<CatalogCategory> findRootCategories() {
        reads++;
        return roots;
    }

    @Override
    public Optional<String> findCategoryName(String name) {
        reads++;
        return roots.stream().map(root -> root.findCategory(name)).flatMap(Optional::stream)
                .map(CatalogItem::name).findFirst();
    }
}
