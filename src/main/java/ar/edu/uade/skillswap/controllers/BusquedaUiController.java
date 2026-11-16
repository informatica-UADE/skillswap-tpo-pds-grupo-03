package ar.edu.uade.skillswap.controllers;

import ar.edu.uade.skillswap.application.port.in.CatalogUseCases;
import ar.edu.uade.skillswap.application.port.in.DiscoveryUseCases;
import ar.edu.uade.skillswap.application.port.out.CurrentUserProvider;
import ar.edu.uade.skillswap.controllers.mapper.UiModelMapper;
import ar.edu.uade.skillswap.model.Models;
import java.util.List;
import java.util.Objects;

/**
 * Explore and Home screens: converts the search filters to the use case, returns a page of simple
 * view models and the personal recommendations of the active user.
 */
public final class BusquedaUiController implements UiControllers.BusquedaController {
    static final int CANTIDAD_RECOMENDACIONES = 3;

    private final DiscoveryUseCases discovery;
    private final CatalogUseCases catalog;
    private final CurrentUserProvider identity;
    private final ControllerExecutor operations;
    private final UiModelMapper mapper = new UiModelMapper();

    public BusquedaUiController(DiscoveryUseCases discovery, CatalogUseCases catalog, CurrentUserProvider identity,
                                ControllerExecutor operations) {
        this.discovery = Objects.requireNonNull(discovery);
        this.catalog = Objects.requireNonNull(catalog);
        this.identity = Objects.requireNonNull(identity);
        this.operations = Objects.requireNonNull(operations);
    }

    /** Public search: it does not modify information, so it does not require the active user. */
    @Override
    public Models.PaginaPublicaciones buscar(Models.FiltrosBusqueda filtros) {
        return operations.execute(() -> mapper.toPaginaPublicaciones(discovery.search(mapper.toSearchQuery(filtros))));
    }

    /** Top results for the active user chosen by the configured strategy; not a paged listing. */
    @Override
    public List<Models.Publicacion> recomendar(String userId) {
        return operations.execute(() -> {
            ControllerUser.requireActive(identity, userId);
            return discovery.recommend(CANTIDAD_RECOMENDACIONES).stream().map(mapper::toPublicacion).toList();
        });
    }

    /** Categorías del catálogo (#37), no las que aparecen en las publicaciones; no requiere el usuario activo. */
    @Override
    public List<Models.Categoria> categorias() {
        return operations.execute(() -> mapper.toCategorias(catalog.viewCatalog()));
    }
}
