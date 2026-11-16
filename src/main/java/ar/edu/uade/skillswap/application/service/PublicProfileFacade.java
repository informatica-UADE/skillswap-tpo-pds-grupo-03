package ar.edu.uade.skillswap.application.service;

import ar.edu.uade.skillswap.application.error.ApplicationErrorCode;
import ar.edu.uade.skillswap.application.error.ApplicationException;
import ar.edu.uade.skillswap.application.mapper.PublicProfileApplicationMapper;
import ar.edu.uade.skillswap.application.model.PageResult;
import ar.edu.uade.skillswap.application.model.PublicProfileData.CompletedBookingsSummary;
import ar.edu.uade.skillswap.application.model.PublicProfileData.PublicProfileQuery;
import ar.edu.uade.skillswap.application.model.PublicProfileData.PublicProfileView;
import ar.edu.uade.skillswap.application.port.in.PublicProfileUseCases;
import ar.edu.uade.skillswap.application.port.out.BookingRepository;
import ar.edu.uade.skillswap.application.port.out.ProfileRepository;
import ar.edu.uade.skillswap.application.port.out.PublicationRepository;
import ar.edu.uade.skillswap.application.port.out.RatingRepository;
import ar.edu.uade.skillswap.application.port.out.TransactionManager;
import ar.edu.uade.skillswap.domain.model.Profile;
import ar.edu.uade.skillswap.domain.model.Publication;
import ar.edu.uade.skillswap.domain.model.Rating;
import java.util.List;
import java.util.Objects;

/**
 * Facade del perfil público (#40): un único punto de entrada que coordina los subsistemas de
 * perfiles, publicaciones, contrataciones y calificaciones. Solo compone lecturas; cada regla
 * queda donde ya estaba (el perfil conoce su reputación y su estado, y cada repositorio sabe
 * qué publicaciones son visibles y qué contrataciones están completadas).
 */
public final class PublicProfileFacade implements PublicProfileUseCases {
    static final int RECENT_RATINGS = 3;

    private final ProfileRepository profiles;
    private final PublicationRepository publications;
    private final BookingRepository bookings;
    private final RatingRepository ratings;
    private final TransactionManager transactions;

    public PublicProfileFacade(ProfileRepository profiles, PublicationRepository publications,
                               BookingRepository bookings, RatingRepository ratings,
                               TransactionManager transactions) {
        this.profiles = Objects.requireNonNull(profiles);
        this.publications = Objects.requireNonNull(publications);
        this.bookings = Objects.requireNonNull(bookings);
        this.ratings = Objects.requireNonNull(ratings);
        this.transactions = Objects.requireNonNull(transactions);
    }

    /** Las cuatro lecturas comparten transacción para que la vista sea consistente. */
    @Override
    public PublicProfileView viewPublicProfile(PublicProfileQuery query) {
        if (query == null) {
            throw new ApplicationException(ApplicationErrorCode.VALIDATION, "Faltan los datos de la consulta");
        }
        return transactions.required(() -> {
            Profile profile = profiles.findByUserId(query.userId())
                    .filter(Profile::active)
                    .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.NOT_FOUND,
                            "El perfil no existe o no está disponible"));
            String userId = profile.userId();
            PageResult<Publication> active = publications.findPublishedByAuthor(userId, query.page(), query.size());
            CompletedBookingsSummary completed = new CompletedBookingsSummary(
                    bookings.countCompletedByProvider(userId), bookings.countCompletedByBuyer(userId));
            List<Rating> recent = ratings.findRecentBySubjectUserId(userId, RECENT_RATINGS);
            return PublicProfileApplicationMapper.toView(profile, active, completed, recent);
        });
    }
}
