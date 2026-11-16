package ar.edu.uade.skillswap.application.port.out;

import ar.edu.uade.skillswap.application.model.DiscoveryData.SearchQuery;
import ar.edu.uade.skillswap.application.model.PageResult;
import ar.edu.uade.skillswap.application.model.PublicationData.PublicationView;
import java.util.List;

/** Read-side projections for discovery; does not load or expose domain aggregates. */
public interface PublicationQueryPort {
    PageResult<PublicationView> searchPublished(SearchQuery query);
    List<String> categories();
}
