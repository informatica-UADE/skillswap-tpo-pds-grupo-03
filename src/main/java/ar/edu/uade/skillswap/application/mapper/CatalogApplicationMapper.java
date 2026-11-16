package ar.edu.uade.skillswap.application.mapper;

import ar.edu.uade.skillswap.application.model.CatalogData.CatalogItemKind;
import ar.edu.uade.skillswap.application.model.CatalogData.CatalogNodeView;
import ar.edu.uade.skillswap.domain.catalog.CatalogCategory;
import ar.edu.uade.skillswap.domain.catalog.CatalogItem;

/** Convierte el Composite del dominio en vistas; trata igual a hojas y compuestos. */
public final class CatalogApplicationMapper {
    private CatalogApplicationMapper() {}

    public static CatalogNodeView toView(CatalogItem item) {
        CatalogItemKind kind = item instanceof CatalogCategory ? CatalogItemKind.CATEGORY : CatalogItemKind.SKILL;
        return new CatalogNodeView(item.id(), item.name(), kind,
                item.children().stream().map(CatalogApplicationMapper::toView).toList());
    }
}
