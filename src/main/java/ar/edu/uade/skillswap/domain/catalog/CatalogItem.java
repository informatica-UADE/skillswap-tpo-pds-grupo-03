package ar.edu.uade.skillswap.domain.catalog;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Componente del Composite (#37): elemento del catálogo jerárquico. Una {@link CatalogSkill} es
 * una hoja; una {@link CatalogCategory} es un compuesto que contiene subcategorías y habilidades.
 * El cliente recorre una rama o una hoja con las mismas operaciones ({@link #stream()},
 * {@link #skillNames()}, {@link #categoryNames()}), sin preguntar de qué tipo es.
 */
public abstract sealed class CatalogItem permits CatalogCategory, CatalogSkill {
    private final String id;
    private final String name;
    private CatalogCategory parent;

    protected CatalogItem(String id, String name) {
        this.id = required(id, "Falta el identificador del elemento del catálogo");
        this.name = required(name, "Falta el nombre del elemento del catálogo");
    }

    public String id() { return id; }

    public String name() { return name; }

    public Optional<CatalogCategory> parent() { return Optional.ofNullable(parent); }

    /** Profundidad en el árbol: cero para una categoría raíz. */
    public int depth() {
        return parent == null ? 0 : parent.depth() + 1;
    }

    /** Hijos directos; una hoja no tiene. */
    public abstract List<CatalogItem> children();

    /** Recorrido en preorden: el elemento y, si es compuesto, todos sus descendientes. */
    public abstract Stream<CatalogItem> stream();

    /** Habilidades del subárbol; para una hoja, solo ella. */
    public final Set<String> skillNames() {
        return namesOf(CatalogSkill.class);
    }

    /** Categorías del subárbol; para una hoja, ninguna. */
    public final Set<String> categoryNames() {
        return namesOf(CatalogCategory.class);
    }

    /** Busca una categoría del subárbol por nombre, sin distinguir mayúsculas ni acentos. */
    public final Optional<CatalogCategory> findCategory(String categoryName) {
        String key = key(categoryName);
        return stream().filter(CatalogCategory.class::isInstance).map(CatalogCategory.class::cast)
                .filter(category -> key(category.name()).equals(key)).findFirst();
    }

    /** Raíz del árbol al que pertenece el elemento. */
    final CatalogItem root() {
        return parent == null ? this : parent.root();
    }

    final boolean isAncestorOrSelf(CatalogItem other) {
        for (CatalogItem current = other; current != null; current = current.parent) {
            if (current == this) return true;
        }
        return false;
    }

    final void attachTo(CatalogCategory newParent) {
        this.parent = newParent;
    }

    /**
     * Clave de comparación de nombres: sin mayúsculas ni acentos, igual que la colación
     * {@code utf8mb4_0900_ai_ci} que usa la base para las categorías.
     */
    static String key(String value) {
        return Normalizer.normalize(value == null ? "" : value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    private Set<String> namesOf(Class<? extends CatalogItem> type) {
        return stream().filter(type::isInstance).map(CatalogItem::name)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String required(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        return value.trim();
    }
}
