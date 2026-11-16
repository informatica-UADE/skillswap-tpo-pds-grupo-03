package ar.edu.uade.skillswap.application.port.out;

import ar.edu.uade.skillswap.application.model.DiscoveryData.SearchQuery;
import ar.edu.uade.skillswap.application.model.PageResult;
import ar.edu.uade.skillswap.application.model.PublicationData.PublicationView;

/**
 * Read-side projections for discovery; does not load or expose domain aggregates. The categories
 * come from the catalog ({@link CatalogRepository}, #37), not from the published publications.
 */
public interface PublicationQueryPort {
    PageResult<PublicationView> searchPublished(SearchQuery query);
}
