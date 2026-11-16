package ar.edu.uade.skillswap.application.port.out;

import ar.edu.uade.skillswap.domain.model.Publication;
import ar.edu.uade.skillswap.application.model.PageResult;
import java.util.List;
import java.util.Optional;

/** Persistence contract for Publication aggregates used by write use cases and domain workflows. */
public interface PublicationRepository {
    Optional<Publication> findById(String publicationId);
    Optional<Publication> findByIdForUpdate(String publicationId);
    List<Publication> findByAuthorUserId(String authorUserId);
    List<Publication> findPublished();

    /** Publicaciones PUBLISHED de un autor, de la más reciente a la más antigua, paginadas en la consulta. */
    PageResult<Publication> findPublishedByAuthor(String authorUserId, int page, int size);

    Publication save(Publication publication);
}
