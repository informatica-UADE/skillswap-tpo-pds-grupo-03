package ar.edu.uade.skillswap.application.service;

import ar.edu.uade.skillswap.application.error.ApplicationErrorCode;
import ar.edu.uade.skillswap.application.error.ApplicationException;
import ar.edu.uade.skillswap.application.mapper.PublicationApplicationMapper;
import ar.edu.uade.skillswap.application.model.DiscoveryData.AuthorView;
import ar.edu.uade.skillswap.application.model.DiscoveryData.SearchQuery;
import ar.edu.uade.skillswap.application.model.PageResult;
import ar.edu.uade.skillswap.application.model.PublicationData.PublicationView;
import ar.edu.uade.skillswap.application.port.in.DiscoveryUseCases;
import ar.edu.uade.skillswap.application.port.out.CatalogRepository;
import ar.edu.uade.skillswap.application.port.out.CurrentUserProvider;
import ar.edu.uade.skillswap.application.port.out.ProfileRepository;
import ar.edu.uade.skillswap.application.port.out.PublicationRepository;
import ar.edu.uade.skillswap.domain.model.Profile;
import ar.edu.uade.skillswap.domain.model.Publication;
import ar.edu.uade.skillswap.domain.model.RecommendationStrategy;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Orchestrates search and personalized discovery through application ports. */
public final class DiscoveryApplicationService implements DiscoveryUseCases {
    private final PublicationRepository publications;
    private final ProfileRepository profiles;
    private final CatalogRepository catalog;
    private final CurrentUserProvider currentUser;
    private final RecommendationStrategy recommendationStrategy;

    public DiscoveryApplicationService(PublicationRepository publications, ProfileRepository profiles,
                                       CatalogRepository catalog, CurrentUserProvider currentUser,
                                       RecommendationStrategy recommendationStrategy) {
        this.publications = Objects.requireNonNull(publications);
        this.profiles = Objects.requireNonNull(profiles);
        this.catalog = Objects.requireNonNull(catalog);
        this.currentUser = Objects.requireNonNull(currentUser);
        this.recommendationStrategy = Objects.requireNonNull(recommendationStrategy);
    }

    /**
     * Valida que haya criterios y que la categoría exista en el catálogo, delega filtros, orden y
     * paginación en el repositorio y completa cada resultado con los datos públicos de su autor.
     * Una categoría incluye toda su rama del catálogo (#37); el repositorio la resuelve en la consulta.
     */
    @Override
    public PageResult<PublicationView> search(SearchQuery query) {
        if (query == null) {
            throw new ApplicationException(ApplicationErrorCode.VALIDATION, "Faltan los criterios de búsqueda");
        }
        if (query.category() != null && catalog.findCategoryName(query.category()).isEmpty()) {
            throw new ApplicationException(ApplicationErrorCode.VALIDATION,
                    "La categoría \"" + query.category() + "\" no existe en el catálogo");
        }
        PageResult<Publication> result = publications.searchPublished(query);
        return new PageResult<>(toViews(result.items()), result.page(), result.size(), result.totalElements());
    }

    @Override
    public List<PublicationView> recommend(int limit) {
        if (limit < 1 || limit > 100) {
            throw new ApplicationException(ApplicationErrorCode.VALIDATION,
                    "El límite de recomendaciones debe estar entre 1 y 100");
        }
        String actor = UseCaseSupport.currentUserId(currentUser);
        Profile profile = profiles.findByUserId(actor)
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.NOT_FOUND,
                        "No existe un perfil para el usuario autenticado"));
        if (!profile.active()) {
            throw new ApplicationException(ApplicationErrorCode.INVALID_STATE,
                    "El perfil debe estar activo para recibir recomendaciones");
        }
        return toViews(recommendationStrategy.recommend(profile, publications.findPublished(), limit));
    }

    /** Arma las vistas leyendo los perfiles de todos los autores en una sola lectura. */
    private List<PublicationView> toViews(List<Publication> items) {
        Map<String, Profile> authors = profiles.findByUserIds(items.stream()
                        .map(Publication::authorUserId)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Profile::userId, Function.identity()));
        return items.stream()
                .map(publication -> PublicationApplicationMapper.toView(publication, authorOf(publication, authors)))
                .toList();
    }

    /** La base garantiza que todo autor tiene perfil (FK); si falta, los datos están inconsistentes. */
    private static Profile authorOf(Publication publication, Map<String, Profile> authors) {
        Profile author = authors.get(publication.authorUserId());
        if (author == null) {
            throw new IllegalStateException("La publicación " + publication.id() + " no tiene perfil de autor");
        }
        return author;
    }

    @Override
    public AuthorView author(String userId) {
        String id = UseCaseSupport.requiredId(userId, "autor");
        Profile profile = profiles.findByUserId(id).orElseThrow(() ->
                new ApplicationException(ApplicationErrorCode.NOT_FOUND, "No existe el perfil del autor"));
        return new AuthorView(
                profile.userId(), profile.displayName(), profile.reputation());
    }
}
