package ar.edu.uade.skillswap.controllers;

import ar.edu.uade.skillswap.application.model.DiscoveryData.CategoryView;
import ar.edu.uade.skillswap.application.model.DiscoveryData.SearchQuery;
import ar.edu.uade.skillswap.application.model.PageResult;
import ar.edu.uade.skillswap.application.model.ProfileData.AvailabilityView;
import ar.edu.uade.skillswap.application.model.PublicationData.AuthorView;
import ar.edu.uade.skillswap.application.model.PublicationData.PublicationView;
import ar.edu.uade.skillswap.application.port.in.DiscoveryUseCases;
import ar.edu.uade.skillswap.domain.model.Modality;
import ar.edu.uade.skillswap.domain.model.PublicationStatus;
import ar.edu.uade.skillswap.domain.model.PublicationType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/** Records the calls the controllers make to the discovery use cases. */
final class DiscoverySpy implements DiscoveryUseCases {
    SearchQuery query;
    Integer limit;
    int calls;
    PageResult<PublicationView> page = new PageResult<>(List.of(), 0, 20, 0);
    List<PublicationView> recommendations = List.of();
    List<CategoryView> categories = List.of();
    RuntimeException categoriesFailure;

    @Override public PageResult<PublicationView> search(SearchQuery query) {
        calls++;
        this.query = query;
        return page;
    }

    @Override public List<PublicationView> recommend(int limit) {
        calls++;
        this.limit = limit;
        return recommendations;
    }

    @Override public List<CategoryView> categories() {
        calls++;
        if (categoriesFailure != null) throw categoriesFailure;
        return categories;
    }

    static PublicationView view(String id, String authorName, double reputation) {
        LocalDateTime start = LocalDateTime.of(2026, 11, 10, 10, 0);
        return new PublicationView(id, "author-" + id, PublicationType.MENTORSHIP, "Mentoría " + id, "Descripción",
                "Tecnología", Modality.HYBRID, 60, 4, 150, Set.of("Java"), PublicationStatus.PUBLISHED,
                List.of(new AvailabilityView("slot-" + id, start, start.plusHours(1), 4, 1)),
                start.minusDays(1), 2, new AuthorView("author-" + id, authorName, reputation, 3));
    }
}
