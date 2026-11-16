package ar.edu.uade.skillswap.infrastructure.persistence.repository;

import ar.edu.uade.skillswap.application.port.out.CatalogRepository;
import ar.edu.uade.skillswap.domain.catalog.CatalogCategory;
import ar.edu.uade.skillswap.infrastructure.persistence.JpaRepositorySupport;
import ar.edu.uade.skillswap.infrastructure.persistence.JpaTransactionManager;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.CatalogCategoryEntity;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.CatalogSkillEntity;
import ar.edu.uade.skillswap.infrastructure.persistence.mapper.CatalogMapper;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Catálogo fijo cargado por la migración V6; solo lectura. */
public final class JpaCatalogRepository extends JpaRepositorySupport implements CatalogRepository {

    public JpaCatalogRepository(JpaTransactionManager transactions) {
        super(transactions);
    }

    /** Dos consultas (categorías y habilidades) para todo el árbol, sin N+1. */
    @Override
    public List<CatalogCategory> findRootCategories() {
        return query(em -> CatalogMapper.toDomain(
                em.createQuery("SELECT c FROM CatalogCategoryEntity c ORDER BY c.sortOrder, c.name",
                        CatalogCategoryEntity.class).getResultList(),
                em.createQuery("SELECT s FROM CatalogSkillEntity s ORDER BY s.sortOrder, s.name",
                        CatalogSkillEntity.class).getResultList()));
    }

    /** En MySQL la colación de {@code name} ya ignora mayúsculas y acentos. */
    @Override
    public Optional<String> findCategoryName(String name) {
        Objects.requireNonNull(name, "Falta el nombre de la categoría");
        return query(em -> em.createQuery(
                        "SELECT c.name FROM CatalogCategoryEntity c WHERE LOWER(c.name) = LOWER(:name)", String.class)
                .setParameter("name", name.trim())
                .getResultStream()
                .findFirst());
    }
}
