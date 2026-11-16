package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.application.port.out.PublicProfileQueriesContract;
import ar.edu.uade.skillswap.domain.model.Booking;
import ar.edu.uade.skillswap.domain.model.Profile;
import ar.edu.uade.skillswap.domain.model.Publication;
import ar.edu.uade.skillswap.domain.model.Rating;
import java.util.List;

/** Ejecuta el contrato de las consultas del perfil público contra los repositorios en memoria. */
class InMemoryPublicProfileQueriesTest extends PublicProfileQueriesContract {
    @Override
    protected Repositories createRepositories(List<Profile> profiles, List<Publication> publications,
                                              List<Booking> bookings, List<Rating> ratings) {
        var profileRepository = new InMemoryProfileRepository();
        profiles.forEach(profileRepository::save);
        var publicationRepository = new InMemoryPublicationRepository(profileRepository,
                new InMemoryCatalogRepository(DemoCatalog.tree()));
        publications.forEach(publicationRepository::save);
        var bookingRepository = new InMemoryBookingRepository();
        bookings.forEach(bookingRepository::save);
        var ratingRepository = new InMemoryRatingRepository();
        ratings.forEach(ratingRepository::save);
        return new Repositories(publicationRepository, bookingRepository, ratingRepository);
    }
}
