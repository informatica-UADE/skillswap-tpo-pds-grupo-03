package ar.edu.uade.skillswap.application.service;

import ar.edu.uade.skillswap.application.mapper.CatalogApplicationMapper;
import ar.edu.uade.skillswap.application.model.CatalogData.CatalogNodeView;
import ar.edu.uade.skillswap.application.port.in.CatalogUseCases;
import ar.edu.uade.skillswap.application.port.out.CatalogRepository;
import ar.edu.uade.skillswap.application.port.out.TransactionManager;
import java.util.List;
import java.util.Objects;

/** Caso de uso "consultar el catálogo": lee el árbol en una transacción y lo devuelve como vistas. */
public final class CatalogApplicationService implements CatalogUseCases {
    private final CatalogRepository catalog;
    private final TransactionManager transactions;

    public CatalogApplicationService(CatalogRepository catalog, TransactionManager transactions) {
        this.catalog = Objects.requireNonNull(catalog);
        this.transactions = Objects.requireNonNull(transactions);
    }

    @Override
    public List<CatalogNodeView> viewCatalog() {
        return transactions.required(() -> catalog.findRootCategories().stream()
                .map(CatalogApplicationMapper::toView).toList());
    }
}
