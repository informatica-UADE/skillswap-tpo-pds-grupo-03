package ar.edu.uade.skillswap.application.model;

import ar.edu.uade.skillswap.application.model.PublicationData.PublicationView;
import ar.edu.uade.skillswap.domain.model.ActorType;
import java.time.LocalDateTime;
import java.util.List;

/** Consulta y modelo de lectura del perfil público (#40); no expone entidades ni datos privados. */
public final class PublicProfileData {
    private PublicProfileData() {}

    /** Perfil a consultar y página de sus publicaciones activas. */
    public record PublicProfileQuery(String userId, int page, int size) {
        public static final int MAX_PAGE_SIZE = 50;

        public PublicProfileQuery {
            if (userId == null || userId.isBlank()) throw new IllegalArgumentException("Falta el usuario del perfil");
            userId = userId.trim();
            if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
                throw new IllegalArgumentException("La página debe ser positiva y el tamaño máximo es " + MAX_PAGE_SIZE);
            }
            if ((long) page * size > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("La página solicitada excede el máximo permitido");
            }
        }
    }

    /** Contrataciones completadas en las que participó el titular, según su rol. */
    public record CompletedBookingsSummary(long asProvider, long asClient) {
        public CompletedBookingsSummary {
            if (asProvider < 0 || asClient < 0) throw new IllegalArgumentException("El historial no puede ser negativo");
        }

        public long total() {
            return asProvider + asClient;
        }
    }

    /** Calificación recibida, sin identificar a quien la escribió. */
    public record ReceivedRatingView(int score, String comment, LocalDateTime createdAt) {}

    /**
     * Vista pública: datos de presentación del titular, reputación verificada, una página de
     * publicaciones activas y el historial resumido. Quedan afuera los intereses, la zona horaria,
     * los canales de notificación, la disponibilidad y la billetera.
     */
    public record PublicProfileView(String userId, ActorType actorType, String displayName, String biography,
                                    List<String> skills, double reputation, int ratingCount,
                                    PageResult<PublicationView> activePublications,
                                    CompletedBookingsSummary completedBookings,
                                    List<ReceivedRatingView> recentRatings) {
        public PublicProfileView {
            skills = List.copyOf(skills == null ? List.of() : skills);
            recentRatings = List.copyOf(recentRatings == null ? List.of() : recentRatings);
        }
    }
}
