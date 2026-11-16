package ar.edu.uade.skillswap.infrastructure.persistence.repository;

import ar.edu.uade.skillswap.domain.catalog.CatalogCategory;
import ar.edu.uade.skillswap.domain.catalog.CatalogItem;
import ar.edu.uade.skillswap.domain.catalog.CatalogSkill;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.CatalogCategoryEntity;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.CatalogSkillEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;

/**
 * Guarda un árbol del catálogo como lo haría la migración V6: una fila por categoría con su padre y
 * su ruta materializada, y una fila por habilidad. En producción el catálogo lo carga Flyway; esta
 * carga existe solo para las pruebas sobre H2.
 */
final class JpaCatalogFixtures {
    private JpaCatalogFixtures() {}

    static void persist(EntityManagerFactory factory, List<CatalogCategory> roots) {
        var em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            for (int index = 0; index < roots.size(); index++) {
                persist(em, roots.get(index), null, "/", index);
            }
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    private static void persist(EntityManager em, CatalogItem item, String parentId,
                                String parentPath, int order) {
        if (item instanceof CatalogSkill skill) {
            em.persist(new CatalogSkillEntity(skill.id(), parentId, skill.name(), order));
            return;
        }
        String path = parentPath + item.id() + "/";
        em.persist(new CatalogCategoryEntity(item.id(), parentId, item.name(), path, order));
        List<CatalogItem> children = item.children();
        int categories = 0;
        int skills = 0;
        for (CatalogItem child : children) {
            persist(em, child, item.id(), path, child instanceof CatalogSkill ? skills++ : categories++);
        }
    }
}
