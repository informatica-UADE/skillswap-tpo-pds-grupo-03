package ar.edu.uade.skillswap.infrastructure.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.uade.skillswap.application.mapper.CatalogApplicationMapper;
import ar.edu.uade.skillswap.application.model.DiscoveryData.SearchQuery;
import ar.edu.uade.skillswap.domain.catalog.CatalogCategory;
import ar.edu.uade.skillswap.infrastructure.DemoCatalog;
import ar.edu.uade.skillswap.infrastructure.persistence.JpaTransactionManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Catálogo jerárquico (#37) contra MySQL real con la migración V6: el árbol que carga coincide con
 * {@link DemoCatalog}, la colación compara sin acentos, la clave foránea protege la categoría de las
 * publicaciones y la migración conserva las categorías que ya existían.
 */
@EnabledIfSystemProperty(named = "skillswap.mysql.it", matches = "true")
@Timeout(60)
class MySqlCatalogRepositoryTest {
    private static final String SCHEMA_PATTERN = "skillswap_catalog_it_[a-f0-9]{32}";

    private String schema;
    private String baseUrl;
    private String url;
    private String user;
    private String password;
    private EntityManagerFactory factory;

    @BeforeEach
    void createIsolatedSchema() throws Exception {
        baseUrl = requireEnvironment("SKILLSWAP_MYSQL_TEST_URL");
        user = requireEnvironment("SKILLSWAP_MYSQL_TEST_USER");
        password = requireEnvironment("SKILLSWAP_MYSQL_TEST_PASSWORD");
        if (!baseUrl.matches("jdbc:mysql://[^/]+/\\?.+")) {
            throw new IllegalArgumentException("Use jdbc:mysql://host:port/?opciones, sin base de datos");
        }
        schema = "skillswap_catalog_it_" + UUID.randomUUID().toString().replace("-", "");
        url = baseUrl.replace("/?", "/" + schema + "?");
        try (var connection = DriverManager.getConnection(baseUrl, user, password);
             var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE `" + schema + "` CHARACTER SET utf8mb4");
        }
    }

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

    @Test
    void laMigracionCargaElMismoArbolQueElCatalogoDeDemostracion() {
        var catalog = new JpaCatalogRepository(transactions("latest"));

        assertEquals(views(DemoCatalog.tree()), views(catalog.findRootCategories()));
        assertEquals(Optional.of("Tecnología"), catalog.findCategoryName("tecnologia"));
        assertEquals(Optional.of("Diseño UX"), catalog.findCategoryName("DISENO ux"));
    }

    @Test
    void laClaveForaneaRechazaUnaCategoriaQueNoEstaEnElCatalogo() throws Exception {
        transactions("latest");
        try (var connection = DriverManager.getConnection(url, user, password);
             var statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO profiles (id,user_id,actor_type,display_name,time_zone) "
                    + "VALUES ('p1','ana','PROFESSIONAL','Ana','UTC')");
            assertThrows(SQLException.class, () -> statement.executeUpdate(publicationInsert("pub", "Astrología")));
            statement.executeUpdate(publicationInsert("pub", "tecnologia"));
            statement.executeUpdate("INSERT INTO publications (id,author_user_id,publication_type,title,"
                    + "description,category,modality,duration_minutes,capacity,base_price,status,created_at) "
                    + "VALUES ('draft','ana','MENTORSHIP','Borrador','Sin categoría',NULL,'REMOTE',60,1,0,"
                    + "'DRAFT',NOW())");
        }
    }

    @Test
    void laMigracionConservaLasCategoriasExistentesYDejaSinCategoriaLosBorradoresVacios() throws Exception {
        flyway("5").migrate();
        try (var connection = DriverManager.getConnection(url, user, password);
             var statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO profiles (id,user_id,actor_type,display_name,time_zone) "
                    + "VALUES ('p1','ana','PROFESSIONAL','Ana','UTC')");
            statement.executeUpdate(publicationInsert("catalogada", "tecnologia"));
            statement.executeUpdate(publicationInsert("anterior", "Fotografía analógica"));
            statement.executeUpdate(publicationInsert("vacia", " "));
        }

        var transactions = transactions("latest");
        var catalog = new JpaCatalogRepository(transactions);
        var publications = new JpaPublicationRepository(transactions);

        List<CatalogCategory> roots = catalog.findRootCategories();
        CatalogCategory preserved = roots.get(roots.size() - 1);
        assertEquals("Fotografía analógica", preserved.name());
        assertTrue(preserved.id().startsWith("anterior-"));
        assertEquals(views(DemoCatalog.tree()), views(roots.subList(0, roots.size() - 1)));
        assertEquals("", publications.findById("vacia").orElseThrow().category());
        assertEquals(List.of("anterior"), publications.searchPublished(new SearchQuery(null,
                "Fotografía analógica", null, null, null, null, null, null, 0, 10)).items().stream()
                .map(publication -> publication.id()).toList());
        try (var connection = DriverManager.getConnection(url, user, password);
             var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT category FROM publications WHERE id='vacia'")) {
            result.next();
            assertNull(result.getString(1));
        }
    }

    private JpaTransactionManager transactions(String target) {
        flyway(target).migrate();
        factory = Persistence.createEntityManagerFactory("skillswap-persistence", Map.of(
                "jakarta.persistence.jdbc.driver", "com.mysql.cj.jdbc.Driver",
                "jakarta.persistence.jdbc.url", url,
                "jakarta.persistence.jdbc.user", user,
                "jakarta.persistence.jdbc.password", password,
                "hibernate.hbm2ddl.auto", "validate"));
        return new JpaTransactionManager(factory);
    }

    private Flyway flyway(String target) {
        return Flyway.configure().dataSource(url, user, password)
                .locations("classpath:db/migration").target(target).load();
    }

    /** Publicación publicada con el esquema de V5, la versión anterior al catálogo. */
    private static String publicationInsert(String id, String category) {
        return "INSERT INTO publications (id,author_user_id,publication_type,title,description,category,"
                + "modality,duration_minutes,capacity,base_price,status,created_at) VALUES ('" + id
                + "','ana','MENTORSHIP','Título " + id + "','Descripción','" + category
                + "','REMOTE',60,1,100,'PUBLISHED',NOW())";
    }

    private static List<?> views(List<CatalogCategory> roots) {
        return roots.stream().map(CatalogApplicationMapper::toView).toList();
    }

    private static String requireEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Falta configurar la variable de entorno: " + name);
        }
        return value;
    }
}
