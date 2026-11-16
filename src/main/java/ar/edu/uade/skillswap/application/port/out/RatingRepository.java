package ar.edu.uade.skillswap.application.port.out;

import ar.edu.uade.skillswap.domain.model.Rating;
import java.util.List;

/** Persistence contract for verified ratings. */
public interface RatingRepository {
    boolean existsByBookingIdAndAuthorUserId(String bookingId, String authorUserId);
    List<Rating> findBySubjectUserId(String subjectUserId);
    /** Las últimas {@code limit} calificaciones recibidas, de la más reciente a la más antigua. */
    List<Rating> findRecentBySubjectUserId(String subjectUserId, int limit);
    Rating save(Rating rating);
}
