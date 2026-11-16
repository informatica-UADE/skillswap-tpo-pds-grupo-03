package ar.edu.uade.skillswap.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Fila de una habilidad del catálogo (#37); siempre pertenece a una categoría. */
@Entity
@Table(name = "catalog_skills", uniqueConstraints = @UniqueConstraint(
        name = "uq_catalog_skill_category_name", columnNames = {"category_id", "name"}))
public class CatalogSkillEntity {

    @Id
    @Column(name = "id", length = 80, nullable = false)
    private String id;

    @Column(name = "category_id", length = 80, nullable = false)
    private String categoryId;

    @Column(name = "name", length = 150, nullable = false)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected CatalogSkillEntity() {}

    public CatalogSkillEntity(String id, String categoryId, String name, int sortOrder) {
        this.id = id;
        this.categoryId = categoryId;
        this.name = name;
        this.sortOrder = sortOrder;
    }

    public String getId() { return id; }

    public String getCategoryId() { return categoryId; }

    public String getName() { return name; }

    public int getSortOrder() { return sortOrder; }
}
