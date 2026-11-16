package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.application.port.out.CatalogRepository;
import ar.edu.uade.skillswap.application.port.out.CatalogRepositoryContract;
import ar.edu.uade.skillswap.domain.catalog.CatalogCategory;
import java.util.List;

/** Ejecuta el contrato del catálogo contra el repositorio en memoria. */
class InMemoryCatalogRepositoryTest extends CatalogRepositoryContract {
    @Override
    protected CatalogRepository createRepository(List<CatalogCategory> roots) {
        return new InMemoryCatalogRepository(roots);
    }
}
