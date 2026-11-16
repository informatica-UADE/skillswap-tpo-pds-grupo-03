package ar.edu.uade.skillswap.application.port.out;

import ar.edu.uade.skillswap.application.model.DiscoveryData.SearchQuery;
import ar.edu.uade.skillswap.application.model.DiscoveryData.AuthorView;
import ar.edu.uade.skillswap.application.model.PageResult;
import ar.edu.uade.skillswap.application.model.PublicationData.PublicationView;
import java.util.List;
import java.util.Optional;

/** Read-side projections for discovery; does not load or expose domain aggregates. */
public interface PublicationQueryPort {
    PageResult<PublicationView> searchPublished(SearchQuery query);
    List<String> categories();
    Optional<AuthorView> findAuthor(String userId);
}
