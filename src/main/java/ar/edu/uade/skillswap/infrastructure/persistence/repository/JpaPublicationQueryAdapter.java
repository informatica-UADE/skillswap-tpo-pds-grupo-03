package ar.edu.uade.skillswap.infrastructure.persistence.repository;

import ar.edu.uade.skillswap.application.model.DiscoveryData.SearchOrder;
import ar.edu.uade.skillswap.application.model.DiscoveryData.SearchQuery;
import ar.edu.uade.skillswap.application.model.PageResult;
import ar.edu.uade.skillswap.application.model.ProfileData.AvailabilityView;
import ar.edu.uade.skillswap.application.model.PublicationData;
import ar.edu.uade.skillswap.application.model.PublicationData.PublicationView;
import ar.edu.uade.skillswap.application.port.out.PublicationQueryPort;
import ar.edu.uade.skillswap.domain.model.Modality;
import ar.edu.uade.skillswap.domain.model.PublicationStatus;
import ar.edu.uade.skillswap.domain.model.PublicationType;
import ar.edu.uade.skillswap.infrastructure.persistence.JpaRepositorySupport;
import ar.edu.uade.skillswap.infrastructure.persistence.JpaTransactionManager;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.CatalogCategoryEntity;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.CatalogSkillEntity;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.ProfileEntity;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.PublicationAvailabilityEntity;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.PublicationEntity;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.PublicationTagEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** JPA read adapter for catalog projections; it never materializes Publication aggregates. */
public final class JpaPublicationQueryAdapter extends JpaRepositorySupport implements PublicationQueryPort {
    public JpaPublicationQueryAdapter(JpaTransactionManager transactions) {
        super(transactions);
    }

    @Override
    public PageResult<PublicationView> searchPublished(SearchQuery search) {
        Objects.requireNonNull(search, "Faltan los criterios de búsqueda");
        return query(em -> {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            Optional<String> branchPath = categoryPath(em, search);
            CriteriaQuery<Object[]> resultQuery = cb.createQuery(Object[].class);
            Root<PublicationEntity> publication = resultQuery.from(PublicationEntity.class);
            Root<ProfileEntity> author = resultQuery.from(ProfileEntity.class);
            Predicate[] predicates = buildPredicates(cb, resultQuery, publication, author, search, branchPath);

            resultQuery.multiselect(
                            publication.get("id"), publication.get("authorUserId"), publication.get("type"),
                            publication.get("title"), publication.get("description"), publication.get("category"),
                            publication.get("modality"), publication.get("durationMinutes"),
                            publication.get("capacity"), publication.get("basePrice"), publication.get("status"),
                            publication.get("createdAt"), publication.get("requestCount"),
                            author.get("userId"), author.get("displayName"), author.get("reputation"),
                            author.get("ratingCount"))
                    .where(predicates)
                    .orderBy(orderBy(cb, publication, author, search.order()));

            int firstResult = Math.multiplyExact(search.page(), search.size());
            List<Object[]> rows = em.createQuery(resultQuery)
                    .setFirstResult(firstResult)
                    .setMaxResults(search.size())
                    .getResultList();

            CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
            Root<PublicationEntity> countRoot = countQuery.from(PublicationEntity.class);
            Root<ProfileEntity> countAuthor = countQuery.from(ProfileEntity.class);
            countQuery.select(cb.countDistinct(countRoot))
                    .where(buildPredicates(cb, countQuery, countRoot, countAuthor, search, branchPath));
            long total = em.createQuery(countQuery).getSingleResult();

            List<String> ids = rows.stream().map(row -> (String) row[0]).toList();
            Map<String, Set<String>> tagsByPublication = loadTags(em, ids);
            Map<String, List<AvailabilityView>> availabilityByPublication = loadAvailability(em, ids);
            List<PublicationView> items = rows.stream()
                    .map(row -> toView(row, tagsByPublication, availabilityByPublication))
                    .toList();
            return new PageResult<>(items, search.page(), search.size(), total);
        });
    }

    private static PublicationView toView(
            Object[] row,
            Map<String, Set<String>> tagsByPublication,
            Map<String, List<AvailabilityView>> availabilityByPublication) {
        String id = (String) row[0];
        return new PublicationView(
                id,
                (String) row[1],
                PublicationType.valueOf((String) row[2]),
                (String) row[3],
                (String) row[4],
                (String) row[5],
                Modality.valueOf((String) row[6]),
                (Integer) row[7],
                (Integer) row[8],
                (Integer) row[9],
                tagsByPublication.getOrDefault(id, Set.of()),
                PublicationStatus.valueOf((String) row[10]),
                availabilityByPublication.getOrDefault(id, List.of()),
                (LocalDateTime) row[11],
                (Integer) row[12],
                new PublicationData.AuthorView(
                        (String) row[13],
                        (String) row[14],
                        ((BigDecimal) row[15]).doubleValue(),
                        (Integer) row[16]));
    }

    private static Map<String, Set<String>> loadTags(
            jakarta.persistence.EntityManager em, List<String> ids) {
        if (ids.isEmpty()) return Map.of();
        Map<String, Set<String>> result = new LinkedHashMap<>();
        em.createQuery(
                        "SELECT t.id.publicationId, t.id.tag FROM PublicationTagEntity t "
                                + "WHERE t.id.publicationId IN :ids ORDER BY t.id.publicationId, t.id.tag",
                        Object[].class)
                .setParameter("ids", ids)
                .getResultList()
                .forEach(row -> result.computeIfAbsent((String) row[0], ignored -> new LinkedHashSet<>())
                        .add((String) row[1]));
        return result;
    }

    private static Map<String, List<AvailabilityView>> loadAvailability(
            jakarta.persistence.EntityManager em, List<String> ids) {
        if (ids.isEmpty()) return Map.of();
        Map<String, List<AvailabilityView>> result = new LinkedHashMap<>();
        em.createQuery(
                        "SELECT s.publication.id, s.id, s.startsAt, s.endsAt, s.capacity, s.reserved "
                                + "FROM PublicationAvailabilityEntity s "
                                + "WHERE s.publication.id IN :ids "
                                + "ORDER BY s.publication.id, s.startsAt, s.id",
                        Object[].class)
                .setParameter("ids", ids)
                .getResultList()
                .forEach(row -> result.computeIfAbsent((String) row[0], ignored -> new ArrayList<>())
                        .add(new AvailabilityView(
                                (String) row[1],
                                (LocalDateTime) row[2],
                                (LocalDateTime) row[3],
                                (Integer) row[4],
                                (Integer) row[5])));
        return result;
    }

    private Predicate[] buildPredicates(
            CriteriaBuilder cb,
            CriteriaQuery<?> criteria,
            Root<PublicationEntity> publication,
            Root<ProfileEntity> author,
            SearchQuery search,
            Optional<String> branchPath) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(publication.get("status"), PublicationStatus.PUBLISHED.name()));
        predicates.add(cb.equal(author.get("userId"), publication.get("authorUserId")));
        predicates.add(cb.isTrue(author.get("active")));

        if (!search.text().isBlank()) {
            String pattern = containsPattern(search.text());
            var tagMatches = criteria.subquery(Integer.class);
            Root<PublicationTagEntity> tag = tagMatches.from(PublicationTagEntity.class);
            tagMatches.select(cb.literal(1)).where(
                    cb.equal(tag.get("publication").get("id"), publication.get("id")),
                    cb.like(cb.lower(tag.get("id").get("tag")), pattern, '\\'));
            predicates.add(cb.or(
                    cb.like(cb.lower(publication.get("title")), pattern, '\\'),
                    cb.like(cb.lower(publication.get("description")), pattern, '\\'),
                    cb.like(cb.lower(publication.get("category")), pattern, '\\'),
                    cb.exists(tagMatches)));
        }
        if (search.category() != null && !search.category().isBlank()) {
            predicates.add(branchPath
                    .map(path -> inCategoryBranch(cb, criteria, publication, path))
                    .orElseGet(cb::disjunction));
        }
        if (search.modality() != null) {
            predicates.add(cb.equal(publication.get("modality"), search.modality().name()));
        }
        if (search.minimumPrice() != null) {
            predicates.add(cb.greaterThanOrEqualTo(
                    publication.<Integer>get("basePrice"), search.minimumPrice()));
        }
        if (search.maximumPrice() != null) {
            predicates.add(cb.lessThanOrEqualTo(
                    publication.<Integer>get("basePrice"), search.maximumPrice()));
        }
        if (search.skill() != null && !search.skill().isBlank()) {
            var skillMatches = criteria.subquery(Integer.class);
            Root<PublicationTagEntity> tag = skillMatches.from(PublicationTagEntity.class);
            skillMatches.select(cb.literal(1)).where(
                    cb.equal(tag.get("publication").get("id"), publication.get("id")),
                    cb.equal(cb.lower(tag.get("id").get("tag")),
                            search.skill().toLowerCase(Locale.ROOT)));
            predicates.add(cb.exists(skillMatches));
        }
        if (search.minimumReputation() != null) {
            predicates.add(cb.greaterThanOrEqualTo(
                    author.<BigDecimal>get("reputation"),
                    BigDecimal.valueOf(search.minimumReputation())));
        }
        return predicates.toArray(Predicate[]::new);
    }

    private List<jakarta.persistence.criteria.Order> orderBy(
            CriteriaBuilder cb,
            Root<PublicationEntity> publication,
            Root<ProfileEntity> author,
            SearchOrder order) {
        List<jakarta.persistence.criteria.Order> orders = new ArrayList<>(switch (order) {
            case LOWEST_PRICE -> List.of(
                    cb.asc(publication.get("basePrice")), cb.desc(publication.get("createdAt")));
            case MOST_RECENT -> List.of(cb.desc(publication.get("createdAt")));
            case BEST_RATING -> List.of(
                    cb.desc(author.get("reputation")), cb.desc(author.get("ratingCount")),
                    cb.desc(publication.get("createdAt")));
            case RELEVANCE -> List.of(
                    cb.desc(publication.get("requestCount")), cb.desc(publication.get("createdAt")));
        });
        orders.add(cb.asc(publication.get("id")));
        return orders;
    }

    /**
     * Ruta materializada de la categoría pedida, para filtrar su rama completa (#37). Es una sola
     * fila: no se carga el catálogo. Vacía si no se filtra por categoría o si no existe.
     */
    private static Optional<String> categoryPath(EntityManager em, SearchQuery search) {
        if (search.category() == null || search.category().isBlank()) return Optional.empty();
        return em.createQuery("SELECT c.path FROM CatalogCategoryEntity c WHERE LOWER(c.name) = LOWER(:name)",
                        String.class)
                .setParameter("name", search.category().trim())
                .getResultStream()
                .findFirst();
    }

    /**
     * Una publicación pertenece a la rama si su categoría es la elegida o una descendiente, o si
     * alguna de sus etiquetas es una habilidad de esa rama. Ambas condiciones se resuelven con
     * subconsultas sobre la ruta materializada, dentro de la misma consulta paginada.
     */
    private static Predicate inCategoryBranch(CriteriaBuilder cb, CriteriaQuery<?> criteria,
                                              Root<PublicationEntity> publication, String path) {
        String branch = likeEscape(path) + "%";
        var branchCategories = criteria.subquery(String.class);
        Root<CatalogCategoryEntity> category = branchCategories.from(CatalogCategoryEntity.class);
        branchCategories.select(category.get("name")).where(cb.like(category.get("path"), branch, '\\'));

        var branchSkillTags = criteria.subquery(Integer.class);
        Root<PublicationTagEntity> tag = branchSkillTags.from(PublicationTagEntity.class);
        Root<CatalogSkillEntity> skill = branchSkillTags.from(CatalogSkillEntity.class);
        Root<CatalogCategoryEntity> owner = branchSkillTags.from(CatalogCategoryEntity.class);
        branchSkillTags.select(cb.literal(1)).where(
                cb.equal(tag.get("publication").get("id"), publication.get("id")),
                cb.equal(cb.lower(tag.get("id").get("tag")), cb.lower(skill.get("name"))),
                cb.equal(skill.get("categoryId"), owner.get("id")),
                cb.like(owner.get("path"), branch, '\\'));

        return cb.or(publication.get("category").in(branchCategories), cb.exists(branchSkillTags));
    }

    private static String likeEscape(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static String containsPattern(String value) {
        String escaped = value.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
