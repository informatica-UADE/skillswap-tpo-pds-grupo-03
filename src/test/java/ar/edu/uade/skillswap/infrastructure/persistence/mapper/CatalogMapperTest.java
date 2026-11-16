package ar.edu.uade.skillswap.infrastructure.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.uade.skillswap.domain.catalog.CatalogCategory;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.CatalogCategoryEntity;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.CatalogSkillEntity;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Las filas pueden romper la jerarquía de maneras que el dominio no ve al construirla (un padre que
 * no existe, un ciclo, una ruta desactualizada). El mapper rechaza un catálogo guardado inválido en
 * lugar de mostrar un árbol incompleto.
 */
class CatalogMapperTest {

    @Test
    void armaElArbolDesdeLasRaicesConservandoElOrdenEntreHermanos() {
        List<CatalogCategory> roots = CatalogMapper.toDomain(List.of(
                category("tecnologia", null, "Tecnología", "/tecnologia/"),
                category("backend", "tecnologia", "Backend", "/tecnologia/backend/"),
                category("datos", "tecnologia", "Datos", "/tecnologia/datos/"),
                category("diseno", null, "Diseño", "/diseno/")),
                List.of(skill("java", "backend", "Java"), skill("sql", "backend", "SQL")));

        assertEquals(List.of("Tecnología", "Diseño"), roots.stream().map(CatalogCategory::name).toList());
        assertEquals(List.of("Backend", "Datos"),
                roots.get(0).children().stream().map(item -> item.name()).toList());
        assertEquals(List.of("Tecnología", "Backend", "Java", "SQL", "Datos"),
                roots.get(0).stream().map(item -> item.name()).toList());
        assertEquals(1, roots.get(0).findCategory("Backend").orElseThrow().depth());
    }

    @Test
    void unCatalogoVacioNoTieneRaices() {
        assertTrue(CatalogMapper.toDomain(List.of(), List.of()).isEmpty());
    }

    @Test
    void rechazaUnPadreOUnaCategoriaDeHabilidadInexistentes() {
        assertInvalid(List.of(category("backend", "tecnologia", "Backend", "/tecnologia/backend/")), List.of(),
                "padre inexistente");
        assertInvalid(List.of(category("tecnologia", null, "Tecnología", "/tecnologia/")),
                List.of(skill("java", "backend", "Java")), "categoría inexistente");
    }

    @Test
    void rechazaCategoriasQueFormanUnCicloYNoSeAlcanzanDesdeUnaRaiz() {
        assertInvalid(List.of(
                category("tecnologia", null, "Tecnología", "/tecnologia/"),
                category("a", "b", "A", "/a/"),
                category("b", "a", "B", "/b/")), List.of(), "ciclo");
    }

    @Test
    void rechazaUnaRutaQueNoCoincideConLaJerarquia() {
        assertInvalid(List.of(
                category("tecnologia", null, "Tecnología", "/tecnologia/"),
                category("backend", "tecnologia", "Backend", "/backend/")), List.of(), "ruta");
    }

    @Test
    void lasReglasDelDominioTambienValidanLoGuardado() {
        assertInvalid(List.of(category("tecnologia", null, "Tecnología", "/tecnologia/")),
                List.of(skill("java", "tecnologia", "Java"), skill("java-2", "tecnologia", "JAVA")), "ya contiene");
    }

    private static void assertInvalid(List<CatalogCategoryEntity> categories, List<CatalogSkillEntity> skills,
                                      String detail) {
        var error = assertThrows(IllegalStateException.class, () -> CatalogMapper.toDomain(categories, skills));
        assertTrue(error.getMessage().startsWith("El catálogo guardado es inválido"), error.getMessage());
        assertTrue(error.getMessage().contains(detail), error.getMessage());
    }

    private static CatalogCategoryEntity category(String id, String parentId, String name, String path) {
        return new CatalogCategoryEntity(id, parentId, name, path, 0);
    }

    private static CatalogSkillEntity skill(String id, String categoryId, String name) {
        return new CatalogSkillEntity(id, categoryId, name, 0);
    }
}
