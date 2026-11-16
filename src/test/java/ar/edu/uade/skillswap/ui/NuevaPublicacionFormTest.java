package ar.edu.uade.skillswap.ui;

import ar.edu.uade.skillswap.infrastructure.SkillSwapUiControllers;
import ar.edu.uade.skillswap.infrastructure.persistence.H2Database;
import ar.edu.uade.skillswap.model.Models.Categoria;
import ar.edu.uade.skillswap.model.Models.DatosBorrador;
import ar.edu.uade.skillswap.model.Models.Disponibilidad;
import ar.edu.uade.skillswap.model.Models.EstadoPublicacion;
import ar.edu.uade.skillswap.model.Models.FiltrosBusqueda;
import ar.edu.uade.skillswap.model.Models.Modalidad;
import ar.edu.uade.skillswap.model.Models.Publicacion;
import ar.edu.uade.skillswap.model.Models.TipoPublicacion;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NuevaPublicacionFormTest {
    private static final LocalDateTime LUNES = LocalDateTime.now().plusDays(3).truncatedTo(ChronoUnit.HOURS);

    @Test
    void armaElBorradorConTodosLosDatosCargados() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var form = new NuevaPublicacionForm(List.of(TipoPublicacion.MENTORIA, TipoPublicacion.SERVICIO),
                    List.of(new Categoria("Tecnología", 0), new Categoria("Arquitectura de software", 2)));
            form.tipo.setSelectedItem(TipoPublicacion.SERVICIO);
            form.titulo.setText("Revisión de arquitectura");
            form.descripcion.setText("Repaso de dependencias y casos de uso.");
            form.categoria.setSelectedIndex(1);
            form.modalidad.setSelectedItem(Modalidad.HIBRIDA);
            form.duracion.setValue(90);
            form.cupos.setValue(3);
            form.precio.setValue(240);
            form.etiquetas.setText(" Java, Clean Architecture ,, ");
            agregarFranja(form, LUNES);
            agregarFranja(form, LUNES.plusDays(1));

            assertEquals(new DatosBorrador("Revisión de arquitectura", "Repaso de dependencias y casos de uso.",
                    "Arquitectura de software", Modalidad.HIBRIDA, 90, 3, 240, List.of("Java", "Clean Architecture"),
                    List.of(new Disponibilidad(null, LUNES, LUNES.plusMinutes(90), 3),
                            new Disponibilidad(null, LUNES.plusDays(1), LUNES.plusDays(1).plusMinutes(90), 3)),
                    TipoPublicacion.SERVICIO), form.datos());
        });
    }

    @Test
    void elBorradorCreadoConElFormularioSePublicaYSeEncuentraPorSusEtiquetas() throws Exception {
        try (var wiring = H2Database.wiring()) {
            var ui = SkillSwapUiControllers.from(wiring);
            Publicacion[] creada = new Publicacion[1];
            SwingUtilities.invokeAndWait(() -> {
                var form = new NuevaPublicacionForm(ui.publicaciones().tiposPublicacion("user-mateo"),
                        ui.busqueda().categorias());
                form.titulo.setText("Mentoría de SQL");
                form.descripcion.setText("Consultas, índices y planes de ejecución.");
                form.categoria.setSelectedItem(new Categoria("Desarrollo de software", 1));
                form.modalidad.setSelectedItem(Modalidad.PRESENCIAL);
                form.precio.setValue(310);
                form.etiquetas.setText("Optimización de consultas");
                agregarFranja(form, LUNES);
                creada[0] = ui.publicaciones().crearBorrador("user-mateo", form.datos());
            });

            Publicacion borrador = creada[0];
            assertEquals(Modalidad.PRESENCIAL, borrador.modalidad());
            assertEquals(310, borrador.precio());
            assertEquals(List.of("Optimización de consultas"), borrador.etiquetas());
            assertEquals(1, borrador.franjasDisponibles().size());

            ui.publicaciones().cambiarEstado("user-mateo", borrador.id(), EstadoPublicacion.PUBLICADA);
            var porHabilidad = ui.busqueda().buscar(new FiltrosBusqueda("", "Todas", null,
                    "Optimización de consultas", null, null, null, "Relevancia", 0, 20)).items();
            assertTrue(porHabilidad.stream().anyMatch(item -> item.id().equals(borrador.id())));
        }
    }

    private static void agregarFranja(NuevaPublicacionForm form, LocalDateTime inicio) {
        form.inicioFranja.setValue(Date.from(inicio.atZone(ZoneId.systemDefault()).toInstant()));
        form.agregarFranja();
    }
}
