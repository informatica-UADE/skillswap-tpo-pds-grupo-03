package ar.edu.uade.skillswap.domain.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Compuesto del Composite: una categoría que contiene subcategorías y habilidades. Es la dueña de
 * las reglas que mantienen el catálogo como un árbol válido, y las controla cada vez que se agrega
 * un elemento, sobre todo su subárbol:
 * <ul>
 *   <li>un elemento tiene un solo padre;</li>
 *   <li>no se puede agregar una categoría dentro de sí misma ni de sus descendientes (ciclo);</li>
 *   <li>los hijos de una categoría no repiten nombre;</li>
 *   <li>el nombre de una categoría es único en todo el árbol, porque las publicaciones la referencian por nombre.</li>
 * </ul>
 * Los nombres se comparan sin mayúsculas ni acentos.
 */
public final class CatalogCategory extends CatalogItem {
    private final List<CatalogItem> children = new ArrayList<>();

    public CatalogCategory(String id, String name) {
        super(id, name);
    }

    /** Agrega una subcategoría o una habilidad y devuelve esta categoría para encadenar. */
    public CatalogCategory add(CatalogItem child) {
        Objects.requireNonNull(child, "Falta el elemento a agregar");
        if (child.isAncestorOrSelf(this)) {
            throw new IllegalArgumentException("Agregar \"" + child.name() + "\" dentro de \"" + name()
                    + "\" formaría un ciclo en el catálogo");
        }
        child.parent().ifPresent(current -> {
            throw new IllegalArgumentException("\"" + child.name() + "\" ya pertenece a la categoría \""
                    + current.name() + "\"");
        });
        String childKey = key(child.name());
        if (children.stream().anyMatch(sibling -> key(sibling.name()).equals(childKey))) {
            throw new IllegalArgumentException("\"" + name() + "\" ya contiene \"" + child.name() + "\"");
        }
        Set<String> existingCategories = categoryKeys(root().stream());
        categoryKeys(child.stream()).stream().filter(existingCategories::contains).findFirst()
                .ifPresent(repeated -> {
                    throw new IllegalArgumentException("La categoría \"" + child.findCategory(repeated)
                            .map(CatalogItem::name).orElse(repeated) + "\" ya existe en el catálogo");
                });
        children.add(child);
        child.attachTo(this);
        return this;
    }

    @Override
    public List<CatalogItem> children() {
        return List.copyOf(children);
    }

    @Override
    public Stream<CatalogItem> stream() {
        return Stream.concat(Stream.of(this), children.stream().flatMap(CatalogItem::stream));
    }

    private static Set<String> categoryKeys(Stream<CatalogItem> items) {
        return items.filter(CatalogCategory.class::isInstance).map(item -> key(item.name()))
                .collect(Collectors.toSet());
    }
}
