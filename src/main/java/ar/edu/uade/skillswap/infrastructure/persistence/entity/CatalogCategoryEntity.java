package ar.edu.uade.skillswap.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Fila de una categoría del catálogo (#37). Guarda la jerarquía con {@code parentId} y, además, la
 * ruta materializada ({@code /tecnologia/desarrollo-backend/}) para que la búsqueda filtre una rama
 * completa con un {@code LIKE} sobre la ruta, sin recorrer el árbol en memoria.
 */
@Entity
@Table(name = "catalog_categories")
public class CatalogCategoryEntity {

    @Id
    @Column(name = "id", length = 80, nullable = false)
    private String id;

    @Column(name = "parent_id", length = 80)
    private String parentId;

    @Column(name = "name", length = 150, nullable = false, unique = true)
    private String name;

    @Column(name = "path", length = 500, nullable = false, unique = true)
    private String path;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected CatalogCategoryEntity() {}

    public CatalogCategoryEntity(String id, String parentId, String name, String path, int sortOrder) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
        this.path = path;
        this.sortOrder = sortOrder;
    }

    public String getId() { return id; }

    public String getParentId() { return parentId; }

    public String getName() { return name; }

    public String getPath() { return path; }

    public int getSortOrder() { return sortOrder; }
}
