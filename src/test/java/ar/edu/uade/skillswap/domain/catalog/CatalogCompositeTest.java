package ar.edu.uade.skillswap.domain.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CatalogCompositeTest {
    private final CatalogSkill java = new CatalogSkill("java", "Java");
    private final CatalogSkill sql = new CatalogSkill("sql", "SQL");
    private final CatalogSkill react = new CatalogSkill("react", "React");
    private final CatalogCategory backend = new CatalogCategory("backend", "Desarrollo backend").add(java).add(sql);
    private final CatalogCategory frontend = new CatalogCategory("frontend", "Desarrollo frontend").add(react);
    private final CatalogCategory technology = new CatalogCategory("tecnologia", "Tecnología")
            .add(backend).add(frontend);

    // ---------- Misma operación para hoja y compuesto ----------

    @Test
    void elRecorridoFuncionaIgualParaUnaHojaYParaUnaRama() {
        assertEquals(List.of(java), java.stream().toList());
        assertEquals(List.of(technology, backend, java, sql, frontend, react), technology.stream().toList());
    }

    @Test
    void lasHabilidadesYCategoriasDelSubarbolSeCalculanRecursivamente() {
        assertEquals(Set.of("Java"), java.skillNames());
        assertEquals(Set.of(), java.categoryNames());
        assertEquals(Set.of("Java", "SQL"), backend.skillNames());
        assertEquals(Set.of("Java", "SQL", "React"), technology.skillNames());
        assertEquals(Set.of("Tecnología", "Desarrollo backend", "Desarrollo frontend"), technology.categoryNames());
    }

    @Test
    void conoceSuPadreYSuProfundidad() {
        assertEquals(0, technology.depth());
        assertEquals(1, backend.depth());
        assertEquals(2, java.depth());
        assertEquals(backend, java.parent().orElseThrow());
        assertTrue(technology.parent().isEmpty());
        assertEquals(List.of(), java.children());
        assertEquals(List.of(java, sql), backend.children());
    }

    @Test
    void buscaCategoriasEnElSubarbolSinDistinguirMayusculasNiAcentos() {
        assertEquals(backend, technology.findCategory("desarrollo BACKEND").orElseThrow());
        assertEquals(technology, technology.findCategory("tecnologia").orElseThrow());
        assertTrue(backend.findCategory("Tecnología").isEmpty());
        assertTrue(java.findCategory("Java").isEmpty());
    }

    @Test
    void losHijosNoSePuedenModificarDesdeAfuera() {
        assertThrows(UnsupportedOperationException.class, () -> technology.children().add(java));
    }

    // ---------- Reglas del árbol ----------

    @Test
    void rechazaCiclos() {
        assertThrows(IllegalArgumentException.class, () -> backend.add(backend));
        var root = new CatalogCategory("raiz", "Raíz");
        var child = new CatalogCategory("hija", "Hija");
        root.add(child);
        var cycle = assertThrows(IllegalArgumentException.class, () -> child.add(root));
        assertTrue(cycle.getMessage().contains("ciclo"));
    }

    @Test
    void unElementoTieneUnSoloPadre() {
        var other = new CatalogCategory("otra", "Otra categoría");
        var error = assertThrows(IllegalArgumentException.class, () -> other.add(java));
        assertTrue(error.getMessage().contains("Desarrollo backend"));
        assertEquals(backend, java.parent().orElseThrow());
        assertFalse(other.children().contains(java));
    }

    @Test
    void losHermanosNoRepitenNombre() {
        assertThrows(IllegalArgumentException.class, () -> backend.add(new CatalogSkill("java-2", "JAVA")));
        assertThrows(IllegalArgumentException.class,
                () -> technology.add(new CatalogCategory("backend-2", "Desarrollo Backend")));
    }

    @Test
    void elNombreDeUnaCategoriaEsUnicoEnTodoElArbolAunqueEsteEnOtraRama() {
        var design = new CatalogCategory("diseno", "Diseño");
        technology.add(design);
        var nested = new CatalogCategory("otra-backend", "desarrollo backend");
        var error = assertThrows(IllegalArgumentException.class, () -> design.add(nested));
        assertTrue(error.getMessage().contains("ya existe en el catálogo"));
        var subtreeWithRepeated = new CatalogCategory("nueva", "Nueva").add(new CatalogCategory("tec", "Tecnologia"));
        assertThrows(IllegalArgumentException.class, () -> design.add(subtreeWithRepeated));
    }

    @Test
    void unaHabilidadPuedeLlamarseIgualQueUnaCategoriaOQueOtraHabilidadDeOtraRama() {
        var data = new CatalogCategory("datos", "Datos").add(new CatalogSkill("sql-datos", "SQL"));
        technology.add(data);
        frontend.add(new CatalogSkill("frontend-skill", "Desarrollo frontend"));
        assertEquals(Set.of("Java", "SQL", "React", "Desarrollo frontend"), technology.skillNames());
    }

    @Test
    void exigeIdentificadorYNombre() {
        assertThrows(IllegalArgumentException.class, () -> new CatalogSkill(" ", "Java"));
        assertThrows(IllegalArgumentException.class, () -> new CatalogCategory("id", null));
        assertThrows(NullPointerException.class, () -> technology.add(null));
    }
}
