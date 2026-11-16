package ar.edu.uade.skillswap.application.port.out;

import ar.edu.uade.skillswap.domain.catalog.CatalogCategory;
import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida del catálogo jerárquico (#37). El catálogo es fijo: lo carga una migración y
 * Application solo lo consulta. Filtrar publicaciones por una rama no usa este puerto: lo resuelve
 * {@link PublicationQueryPort#searchPublished} en la consulta paginada.
 */
public interface CatalogRepository {
    /** Árbol completo, con las categorías raíz y sus hijos en el orden de presentación. */
    List<CatalogCategory> findRootCategories();

    /** Nombre con el que el catálogo registra una categoría, comparando sin mayúsculas ni acentos. */
    Optional<String> findCategoryName(String name);
}
