package ar.edu.uade.skillswap.domain.catalog;

import java.util.List;
import java.util.stream.Stream;

/** Hoja del Composite: una habilidad del catálogo. Siempre pertenece a una categoría. */
public final class CatalogSkill extends CatalogItem {
    public CatalogSkill(String id, String name) {
        super(id, name);
    }

    @Override
    public List<CatalogItem> children() {
        return List.of();
    }

    @Override
    public Stream<CatalogItem> stream() {
        return Stream.of(this);
    }
}
