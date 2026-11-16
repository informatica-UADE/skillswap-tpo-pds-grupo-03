package ar.edu.uade.skillswap.infrastructure.persistence.repository;

import ar.edu.uade.skillswap.application.port.out.PublicProfileQueriesContract;
import ar.edu.uade.skillswap.domain.model.Booking;
import ar.edu.uade.skillswap.domain.model.Profile;
import ar.edu.uade.skillswap.domain.model.Publication;
import ar.edu.uade.skillswap.domain.model.Rating;
import ar.edu.uade.skillswap.infrastructure.persistence.JpaTransactionManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.sql.DriverManager;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ejecuta el contrato de las consultas del perfil público contra MySQL real, con el esquema creado
 * por las migraciones de Flyway (claves foráneas e índices incluidos) en una base aislada por caso.
 */
@EnabledIfSystemProperty(named = "skillswap.mysql.it", matches = "true")
@Timeout(60)
class MySqlPublicProfileQueriesTest extends PublicProfileQueriesContract {
    private static final String SCHEMA_PATTERN = "skillswap_profile_it_[a-f0-9]{32}";

    private String schema;
    private String baseUrl;
    private String user;
    private String password;
    private EntityManagerFactory factory;

    @AfterEach
    void removeOnlyOwnedSchema() throws Exception {
        try {
            if (factory != null && factory.isOpen()) factory.close();
        } finally {
            if (schema != null && schema.matches(SCHEMA_PATTERN)) {
                try (var connection = DriverManager.getConnection(baseUrl, user, password);
                     var statement = connection.createStatement()) {
                    statement.execute("DROP DATABASE IF EXISTS `" + schema + "`");
                }
            }
        }
    }

    @Override
    protected Repositories createRepositories(List<Profile> profiles, List<Publication> publications,
                                              List<Booking> bookings, List<Rating> ratings) {
        baseUrl = requireEnvironment("SKILLSWAP_MYSQL_TEST_URL");
        user = requireEnvironment("SKILLSWAP_MYSQL_TEST_USER");
        password = requireEnvironment("SKILLSWAP_MYSQL_TEST_PASSWORD");
        if (!baseUrl.matches("jdbc:mysql://[^/]+/\\?.+")) {
            throw new IllegalArgumentException("Use jdbc:mysql://host:port/?opciones, sin base de datos");
        }
        schema = "skillswap_profile_it_" + UUID.randomUUID().toString().replace("-", "");
        String url = baseUrl.replace("/?", "/" + schema + "?");
        try (var connection = DriverManager.getConnection(baseUrl, user, password);
             var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE `" + schema + "` CHARACTER SET utf8mb4");
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo crear la base de prueba", exception);
        }

        Flyway.configure().dataSource(url, user, password)
                .locations("classpath:db/migration").load().migrate();
        factory = Persistence.createEntityManagerFactory("skillswap-persistence", Map.of(
                "jakarta.persistence.jdbc.driver", "com.mysql.cj.jdbc.Driver",
                "jakarta.persistence.jdbc.url", url,
                "jakarta.persistence.jdbc.user", user,
                "jakarta.persistence.jdbc.password", password,
                "hibernate.hbm2ddl.auto", "validate"));
        return JpaPublicProfileFixtures.load(new JpaTransactionManager(factory), profiles, publications,
                bookings, ratings);
    }

    private static String requireEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Falta configurar la variable de entorno: " + name);
        }
        return value;
    }
}
