package ar.edu.uade.skillswap.infrastructure.persistence.repository;

import ar.edu.uade.skillswap.application.port.out.PublicProfileQueriesContract;
import ar.edu.uade.skillswap.domain.model.Booking;
import ar.edu.uade.skillswap.domain.model.Profile;
import ar.edu.uade.skillswap.domain.model.Publication;
import ar.edu.uade.skillswap.domain.model.Rating;
import ar.edu.uade.skillswap.infrastructure.persistence.JpaTransactionManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.AfterEach;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ejecuta el contrato de las consultas del perfil público con las implementaciones JPA sobre H2
 * (modo MySQL). Corre siempre; la misma verificación contra MySQL real está en
 * {@link MySqlPublicProfileQueriesTest}.
 */
class JpaPublicProfileQueriesTest extends PublicProfileQueriesContract {
    private EntityManagerFactory factory;

    @AfterEach
    void closeFactory() {
        if (factory != null && factory.isOpen()) factory.close();
    }

    @Override
    protected Repositories createRepositories(List<Profile> profiles, List<Publication> publications,
                                              List<Booking> bookings, List<Rating> ratings) {
        String url = "jdbc:h2:mem:public_profile_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        factory = Persistence.createEntityManagerFactory("skillswap-persistence", Map.of(
                "jakarta.persistence.jdbc.driver", "org.h2.Driver",
                "jakarta.persistence.jdbc.url", url,
                "jakarta.persistence.jdbc.user", "sa",
                "jakarta.persistence.jdbc.password", "",
                "hibernate.hbm2ddl.auto", "create-drop"));
        return JpaPublicProfileFixtures.load(new JpaTransactionManager(factory), profiles, publications,
                bookings, ratings);
    }
}
