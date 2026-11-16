package ar.edu.uade.skillswap.infrastructure.persistence;

import ar.edu.uade.skillswap.infrastructure.SkillSwapApplicationWiring;
import ar.edu.uade.skillswap.infrastructure.identity.MockIdentityClient;
import ar.edu.uade.skillswap.infrastructure.notification.email.MockEmailClient;
import ar.edu.uade.skillswap.infrastructure.notification.push.MockPushClient;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.Map;
import java.util.UUID;

/**
 * Base H2 en memoria para las pruebas que no necesitan MySQL: cada llamada crea una base nueva y
 * aislada, con el esquema que Hibernate genera desde las entidades JPA. Lo propio de MySQL (las
 * migraciones de Flyway, la colación sin acentos) se prueba con el perfil {@code mysql-it}.
 */
public final class H2Database {
    private H2Database() {}

    public static EntityManagerFactory factory(String name) {
        String url = "jdbc:h2:mem:" + name + "_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        return Persistence.createEntityManagerFactory("skillswap-persistence", Map.of(
                "jakarta.persistence.jdbc.driver", "org.h2.Driver",
                "jakarta.persistence.jdbc.url", url,
                "jakarta.persistence.jdbc.user", "sa",
                "jakarta.persistence.jdbc.password", "",
                "hibernate.hbm2ddl.auto", "create-drop",
                "hibernate.show_sql", "false"));
    }

    /** La aplicación completa, con los datos de demostración, sobre una base nueva. */
    public static SkillSwapApplicationWiring wiring() {
        return wiring(new MockIdentityClient("user-mateo"), new MockEmailClient(), new MockPushClient());
    }

    public static SkillSwapApplicationWiring wiring(MockIdentityClient identity, MockEmailClient email,
                                                    MockPushClient push) {
        return SkillSwapApplicationWiring.usingJpa(factory("app"), identity, email, push);
    }
}
