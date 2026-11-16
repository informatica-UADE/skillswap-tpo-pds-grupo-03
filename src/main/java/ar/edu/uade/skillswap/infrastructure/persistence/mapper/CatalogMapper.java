package ar.edu.uade.skillswap.infrastructure.persistence.mapper;

import ar.edu.uade.skillswap.domain.catalog.CatalogCategory;
import ar.edu.uade.skillswap.domain.catalog.CatalogSkill;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.CatalogCategoryEntity;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.CatalogSkillEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reconstruye el Composite a partir de las filas. Arma el árbol desde las raíces con
 * {@link CatalogCategory#add}, así las mismas reglas del dominio validan lo que está guardado, y
 * además rechaza lo que las filas pueden romper y el dominio no ve: padres inexistentes, categorías
 * que no se alcanzan desde una raíz (un ciclo) y rutas que no coinciden con la jerarquía.
 */
public final class CatalogMapper {
    private CatalogMapper() {}

    /** Las filas llegan en orden de presentación; ese orden se conserva entre hermanos. */
    public static List<CatalogCategory> toDomain(List<CatalogCategoryEntity> categoryRows,
                                                 List<CatalogSkillEntity> skillRows) {
        Map<String, CatalogCategoryEntity> rowsById = new LinkedHashMap<>();
        categoryRows.forEach(row -> rowsById.put(row.getId(), row));
        Map<String, List<CatalogCategoryEntity>> childRows = new HashMap<>();
        List<CatalogCategoryEntity> rootRows = new ArrayList<>();
        for (CatalogCategoryEntity row : categoryRows) {
            if (row.getParentId() == null) {
                rootRows.add(row);
            } else if (!rowsById.containsKey(row.getParentId())) {
                throw invalid("la categoría \"" + row.getName() + "\" apunta a un padre inexistente");
            } else {
                childRows.computeIfAbsent(row.getParentId(), id -> new ArrayList<>()).add(row);
            }
        }
        Map<String, List<CatalogSkillEntity>> skillsByCategory = new HashMap<>();
        for (CatalogSkillEntity row : skillRows) {
            if (!rowsById.containsKey(row.getCategoryId())) {
                throw invalid("la habilidad \"" + row.getName() + "\" apunta a una categoría inexistente");
            }
            skillsByCategory.computeIfAbsent(row.getCategoryId(), id -> new ArrayList<>()).add(row);
        }

        Map<String, CatalogCategory> built = new HashMap<>();
        List<CatalogCategory> roots = new ArrayList<>();
        for (CatalogCategoryEntity row : rootRows) {
            roots.add(build(row, "/" + row.getId() + "/", childRows, skillsByCategory, built));
        }
        List<String> unreachable = categoryRows.stream().filter(row -> !built.containsKey(row.getId()))
                .map(CatalogCategoryEntity::getName).toList();
        if (!unreachable.isEmpty()) {
            throw invalid("las categorías " + unreachable + " no se alcanzan desde una raíz (forman un ciclo)");
        }
        return roots;
    }

    private static CatalogCategory build(CatalogCategoryEntity row, String expectedPath,
                                         Map<String, List<CatalogCategoryEntity>> childRows,
                                         Map<String, List<CatalogSkillEntity>> skillsByCategory,
                                         Map<String, CatalogCategory> built) {
        if (!expectedPath.equals(row.getPath())) {
            throw invalid("la ruta de \"" + row.getName() + "\" es " + row.getPath() + " y debería ser " + expectedPath);
        }
        CatalogCategory category = new CatalogCategory(row.getId(), row.getName());
        built.put(row.getId(), category);
        try {
            for (CatalogCategoryEntity child : childRows.getOrDefault(row.getId(), List.of())) {
                category.add(build(child, expectedPath + child.getId() + "/", childRows, skillsByCategory, built));
            }
            for (CatalogSkillEntity skill : skillsByCategory.getOrDefault(row.getId(), List.of())) {
                category.add(new CatalogSkill(skill.getId(), skill.getName()));
            }
        } catch (IllegalArgumentException exception) {
            throw invalid(exception.getMessage());
        }
        return category;
    }

    private static IllegalStateException invalid(String detail) {
        return new IllegalStateException("El catálogo guardado es inválido: " + detail);
    }
}
