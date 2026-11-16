package ar.edu.uade.skillswap.infrastructure.persistence.repository;

import ar.edu.uade.skillswap.application.model.DiscoveryData.SearchQuery;
import ar.edu.uade.skillswap.domain.model.ActorType;
import ar.edu.uade.skillswap.domain.model.AvailabilitySlot;
import ar.edu.uade.skillswap.domain.model.Modality;
import ar.edu.uade.skillswap.domain.model.NotificationChannelType;
import ar.edu.uade.skillswap.domain.model.Profile;
import ar.edu.uade.skillswap.domain.model.Publication;
import ar.edu.uade.skillswap.domain.model.PublicationType;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import ar.edu.uade.skillswap.infrastructure.persistence.JpaTransactionManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JpaPublicationQueryAdapterTest {
    @Test
    void projectsPagedPublicationWithAuthorTagsAndAvailability() {
        String databaseName = "publication_query_" + UUID.randomUUID().toString().replace("-", "");
        EntityManagerFactory factory = Persistence.createEntityManagerFactory(
                "skillswap-persistence", Map.of(
                        "jakarta.persistence.jdbc.driver", "org.h2.Driver",
                        "jakarta.persistence.jdbc.url", "jdbc:h2:mem:" + databaseName
                                + ";MODE=MySQL;DB_CLOSE_DELAY=-1",
                        "jakarta.persistence.jdbc.user", "sa",
                        "jakarta.persistence.jdbc.password", "",
                        "hibernate.hbm2ddl.auto", "create-drop",
                        "hibernate.show_sql", "false"));
        try {
            var transactions = new JpaTransactionManager(factory);
            var profiles = new JpaProfileRepository(transactions);
            var publications = new JpaPublicationRepository(transactions);
            var queries = new JpaPublicationQueryAdapter(transactions);
            LocalDateTime startsAt = LocalDateTime.of(2026, 11, 12, 10, 0);
            var profile = new Profile(null, "mentor-1", ActorType.PROFESSIONAL, "Ana", "", "UTC",
                    Set.of(), Set.of(), Set.of(NotificationChannelType.IN_APP), List.of(),
                    4.5, 2, true);
            var publication = Publication.builder("mentor-1", ActorType.PROFESSIONAL,
                            PublicationType.MENTORSHIP)
                    .title("Mentoría de Java")
                    .description("Práctica individual")
                    .category("Tecnología")
                    .modality(Modality.REMOTE)
                    .durationMinutes(60)
                    .capacity(2)
                    .basePrice(new SkillCoins(100))
                    .tags(Set.of("Java", "Backend"))
                    .addAvailability(AvailabilitySlot.of(startsAt, startsAt.plusHours(1), 2))
                    .build();
            publication.publish("mentor-1");

            transactions.required(() -> {
                profiles.save(profile);
                publications.save(publication);
                return null;
            });

            var page = queries.searchPublished(new SearchQuery(
                    "Java", null, null, null, null, null, null, null, 0, 10));

            assertEquals(1, page.totalElements());
            assertEquals("Mentoría de Java", page.items().get(0).title());
            assertEquals(Set.of("Java", "Backend"), page.items().get(0).tags());
            assertEquals("Ana", page.items().get(0).author().displayName());
            assertEquals(4.5, page.items().get(0).author().reputation());
            assertEquals(2, page.items().get(0).author().ratingCount());
            assertEquals(1, page.items().get(0).availability().size());
            assertEquals(2, page.items().get(0).availability().get(0).capacity());
        } finally {
            factory.close();
        }
    }
}
