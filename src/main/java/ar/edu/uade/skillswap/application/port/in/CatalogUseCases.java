package ar.edu.uade.skillswap.application.port.in;

import ar.edu.uade.skillswap.application.model.CatalogData.CatalogNodeView;
import java.util.List;

/** Consulta del catálogo de categorías y habilidades (#37); no modifica información. */
public interface CatalogUseCases {
    /** Categorías raíz con sus subcategorías y habilidades, en el orden de presentación. */
    List<CatalogNodeView> viewCatalog();
}
