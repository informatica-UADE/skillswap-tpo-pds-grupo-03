package ar.edu.uade.skillswap.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ar.edu.uade.skillswap.application.model.CatalogData.CatalogItemKind;
import ar.edu.uade.skillswap.application.model.CatalogData.CatalogNodeView;
import ar.edu.uade.skillswap.domain.catalog.CatalogCategory;
import ar.edu.uade.skillswap.domain.catalog.CatalogSkill;
import java.util.List;
import org.junit.jupiter.api.Test;

class CatalogApplicationServiceTest {

    @Test
    void devuelveElArbolComoVistasConCategoriasYHabilidadesEnOrden() {
        var backend = new CatalogCategory("backend", "Backend").add(new CatalogSkill("java", "Java"));
        var technology = new CatalogCategory("tecnologia", "Tecnología").add(backend)
                .add(new CatalogSkill("carrera", "Carrera"));
        var design = new CatalogCategory("diseno", "Diseño");
        var transactions = new ImmediateTransactionManager();
        var service = new CatalogApplicationService(new FixedCatalog(technology, design), transactions);

        List<CatalogNodeView> catalog = service.viewCatalog();

        assertEquals(List.of(
                new CatalogNodeView("tecnologia", "Tecnología", CatalogItemKind.CATEGORY, List.of(
                        new CatalogNodeView("backend", "Backend", CatalogItemKind.CATEGORY, List.of(
                                new CatalogNodeView("java", "Java", CatalogItemKind.SKILL, List.of()))),
                        new CatalogNodeView("carrera", "Carrera", CatalogItemKind.SKILL, List.of()))),
                new CatalogNodeView("diseno", "Diseño", CatalogItemKind.CATEGORY, List.of())), catalog);
        assertEquals(1, transactions.operations);
    }

    @Test
    void unCatalogoVacioDevuelveUnaListaVacia() {
        var service = new CatalogApplicationService(new FixedCatalog(), new ImmediateTransactionManager());

        assertEquals(List.of(), service.viewCatalog());
    }
}
