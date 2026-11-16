package ar.edu.uade.skillswap.infrastructure.persistence.repository;

import ar.edu.uade.skillswap.application.port.out.CatalogRepository;
import ar.edu.uade.skillswap.application.port.out.CatalogRepositoryContract;
import ar.edu.uade.skillswap.domain.catalog.CatalogCategory;
import ar.edu.uade.skillswap.infrastructure.persistence.JpaTransactionManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;

/**
 * Ejecuta el contrato del catálogo con JPA sobre H2 (modo MySQL). Corre siempre; el catálogo real
 * que carga la migración se verifica contra MySQL en {@link MySqlCatalogRepositoryTest}.
 */
class JpaCatalogRepositoryTest extends CatalogRepositoryContract {
    private EntityManagerFactory factory;

    @AfterEach
    void closeFactory() {
        if (factory != null && factory.isOpen()) factory.close();
    }

    @Override
    protected CatalogRepository createRepository(List<CatalogCategory> roots) {
        String url = "jdbc:h2:mem:catalog_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        factory = Persistence.createEntityManagerFactory("skillswap-persistence", Map.of(
                "jakarta.persistence.jdbc.driver", "org.h2.Driver",
                "jakarta.persistence.jdbc.url", url,
                "jakarta.persistence.jdbc.user", "sa",
                "jakarta.persistence.jdbc.password", "",
                "hibernate.hbm2ddl.auto", "create-drop"));
        JpaCatalogFixtures.persist(factory, roots);
        return new JpaCatalogRepository(new JpaTransactionManager(factory));
    }
}
