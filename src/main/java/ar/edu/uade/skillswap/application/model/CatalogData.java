package ar.edu.uade.skillswap.application.model;

import java.util.List;

/** Modelo de lectura del catálogo jerárquico; no expone las entidades del Composite. */
public final class CatalogData {
    private CatalogData() {}

    public enum CatalogItemKind { CATEGORY, SKILL }

    /** Un nodo del árbol: una categoría con sus hijos o una habilidad (sin hijos). */
    public record CatalogNodeView(String id, String name, CatalogItemKind kind, List<CatalogNodeView> children) {
        public CatalogNodeView {
            children = List.copyOf(children == null ? List.of() : children);
        }
    }
}
