package ar.edu.uade.skillswap.infrastructure.persistence.repository;

import ar.edu.uade.skillswap.application.port.out.PublicProfileQueriesContract.Repositories;
import ar.edu.uade.skillswap.domain.model.Booking;
import ar.edu.uade.skillswap.domain.model.Profile;
import ar.edu.uade.skillswap.domain.model.Publication;
import ar.edu.uade.skillswap.domain.model.Rating;
import ar.edu.uade.skillswap.infrastructure.persistence.JpaTransactionManager;

import java.util.List;

/** Persiste el escenario del contrato con los repositorios JPA, respetando las claves foráneas. */
final class JpaPublicProfileFixtures {
    private JpaPublicProfileFixtures() {}

    static Repositories load(JpaTransactionManager transactions, List<Profile> profiles,
                             List<Publication> publications, List<Booking> bookings, List<Rating> ratings) {
        var profileRepository = new JpaProfileRepository(transactions);
        var publicationRepository = new JpaPublicationRepository(transactions);
        var bookingRepository = new JpaBookingRepository(transactions);
        var ratingRepository = new JpaRatingRepository(transactions);
        transactions.required(() -> {
            profiles.forEach(profileRepository::save);
            publications.forEach(publicationRepository::save);
            bookings.forEach(bookingRepository::save);
            ratings.forEach(ratingRepository::save);
            return null;
        });
        return new Repositories(publicationRepository, bookingRepository, ratingRepository);
    }
}
