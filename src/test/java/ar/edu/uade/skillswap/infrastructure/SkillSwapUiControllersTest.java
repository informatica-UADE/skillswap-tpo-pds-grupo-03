package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.model.Models.FiltrosBusqueda;
import ar.edu.uade.skillswap.model.Models.Modalidad;
import ar.edu.uade.skillswap.model.Models.EstadoContratacion;
import ar.edu.uade.skillswap.model.Models;
import ar.edu.uade.skillswap.controllers.BusquedaUiController;
import ar.edu.uade.skillswap.controllers.CalificacionUiController;
import ar.edu.uade.skillswap.controllers.ContratacionUiController;
import ar.edu.uade.skillswap.controllers.NotificacionUiController;
import ar.edu.uade.skillswap.controllers.PerfilUiController;
import ar.edu.uade.skillswap.controllers.PublicacionUiController;
import ar.edu.uade.skillswap.controllers.WalletUiController;
import ar.edu.uade.skillswap.controllers.error.PresentationException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SkillSwapUiControllersTest {
    @Test
    void cadaPantallaRecibeSuControllerYLaBilleteraYLaBandejaSiguenALaSesionDemo() {
        try (var wiring = new SkillSwapApplicationWiring()) {
            var ui = SkillSwapUiControllers.from(wiring);
            assertInstanceOf(BusquedaUiController.class, ui.busqueda());
            assertInstanceOf(PublicacionUiController.class, ui.publicaciones());
            assertInstanceOf(ContratacionUiController.class, ui.contrataciones());
            assertInstanceOf(CalificacionUiController.class, ui.calificaciones());
            assertInstanceOf(WalletUiController.class, ui.wallet());
            assertInstanceOf(NotificacionUiController.class, ui.notificaciones());
            assertInstanceOf(PerfilUiController.class, ui.perfiles());
            assertSame(wiring.currentUserProvider(), ui.sesionDemo());
            assertEquals(List.of(new Models.UsuarioDemo("mentor-ana", "Ana García", "Profesional"),
                    new Models.UsuarioDemo("mentor-lu", "Lucía Pérez", "Profesional"),
                    new Models.UsuarioDemo("user-mateo", "Mateo Silva", "Profesional")),
                    ui.sesionDemo().usuariosDisponibles());

            assertEquals(2480, ui.wallet().consultarSaldo("user-mateo"));
            assertEquals(1, ui.notificaciones().listarNotificaciones("user-mateo").size());

            ui.sesionDemo().cambiarUsuario("mentor-ana");

            var other = assertThrows(PresentationException.class, () -> ui.wallet().consultarSaldo("user-mateo"));
            assertEquals(Models.CodigoError.PERMISOS, other.error().codigo());
            assertEquals(0, ui.wallet().consultarSaldo("mentor-ana"));
            assertTrue(ui.notificaciones().listarNotificaciones("mentor-ana").isEmpty());
        }
    }

    @Test
    void muestraElTituloSinCacheYLaContraparteSegunLaIdentidadActiva() {
        try (var wiring = new SkillSwapApplicationWiring()) {
            var ui = SkillSwapUiControllers.from(wiring);
            var publication = ui.publicaciones().listarPropias("user-mateo").get(0);
            ui.sesionDemo().cambiarUsuario("mentor-ana");
            ui.contrataciones().solicitar("mentor-ana", publication.id(), "Mensaje", "Objetivo",
                    publication.franjasDisponibles().get(0).id());
            var freshUi = SkillSwapUiControllers.from(wiring);
            var buyerView = freshUi.contrataciones().listarContrataciones("mentor-ana").get(0);
            assertEquals(publication.titulo(), buyerView.titulo());
            assertEquals("Mateo Silva", buyerView.contraparte());
            assertEquals("user-mateo", buyerView.contraparteId());
            freshUi.sesionDemo().cambiarUsuario("user-mateo");
            var providerView = freshUi.contrataciones().listarContrataciones("user-mateo").get(0);
            assertEquals(publication.titulo(), providerView.titulo());
            assertEquals("Ana García", providerView.contraparte());
            assertEquals("mentor-ana", providerView.contraparteId());
        }
    }

    @Test
    void elPerfilPublicoDelAutorReflejaSuHistorialYReseniasDesdeLaUi() {
        try (var wiring = new SkillSwapApplicationWiring()) {
            var ui = SkillSwapUiControllers.from(wiring);
            var publication = ui.busqueda().buscar(
                    new FiltrosBusqueda("entrevistas", "Todas", null, 0, "Relevancia")).items().get(0);
            assertEquals("mentor-ana", publication.proveedorId());

            var before = ui.perfiles().verPerfilPublico(publication.proveedorId(), 0);
            assertEquals("Ana García", before.perfil().nombre());
            assertTrue(before.publicaciones().items().stream().anyMatch(item -> item.id().equals(publication.id())));
            assertEquals(0, before.completadasComoProveedor());
            assertTrue(before.ultimasResenas().isEmpty());

            var booking = ui.contrataciones().solicitar("user-mateo", publication.id(), "", "Practicar",
                    publication.franjasDisponibles().get(0).id());
            ui.sesionDemo().cambiarUsuario("mentor-ana");
            ui.contrataciones().aceptar("mentor-ana", booking.id());
            ui.sesionDemo().cambiarUsuario("user-mateo");
            ui.contrataciones().pagar("user-mateo", booking.id());
            ui.sesionDemo().cambiarUsuario("mentor-ana");
            ui.contrataciones().iniciar("mentor-ana", booking.id());
            ui.sesionDemo().cambiarUsuario("user-mateo");
            ui.contrataciones().completar("user-mateo", booking.id());
            ui.calificaciones().calificar("user-mateo", booking.id(), 5, "Muy útil");

            var after = ui.perfiles().verPerfilPublico("mentor-ana", 0);
            assertEquals(1, after.completadasComoProveedor());
            assertEquals(1, after.perfil().proyectosCompletados());
            assertEquals(5.0, after.perfil().reputacion());
            assertEquals("Muy útil", after.ultimasResenas().get(0).comentario());
            assertEquals(1, ui.perfiles().verPerfil("user-mateo").proyectosCompletados());
        }
    }

    @Test
    void mantieneLosTiposExplicitosYLosPermisosAlCambiarLaSesionDemo() {
        var controllers = SkillSwapUiControllers.from(new SkillSwapApplicationWiring());
        controllers.sesionDemo().cambiarUsuario("mentor-lu");
        assertEquals(List.of(Models.TipoPublicacion.MENTORIA, Models.TipoPublicacion.SERVICIO),
                controllers.publicaciones().tiposPublicacion("mentor-lu"));
        assertEquals(Models.TipoPublicacion.SERVICIO,
                controllers.publicaciones().listarPropias("mentor-lu").get(0).tipo());
        var draft = controllers.publicaciones().crearBorrador("mentor-lu", "Revisión de portfolio",
                "Servicio de revisión", "Diseño", Modalidad.REMOTA, 60, 100, Models.TipoPublicacion.SERVICIO);
        assertEquals(Models.TipoPublicacion.SERVICIO, draft.tipo());
        var forbidden = assertThrows(PresentationException.class, () -> controllers.publicaciones()
                .crearBorrador("mentor-lu", "Desafío", "Descripción", "Diseño", Modalidad.REMOTA,
                        60, 100, Models.TipoPublicacion.DESAFIO));
        assertEquals(Models.CodigoError.PERMISOS, forbidden.error().codigo());
        controllers.sesionDemo().cambiarUsuario("mentor-ana");
        assertTrue(controllers.publicaciones().listarPropias("mentor-ana").stream()
                .noneMatch(item -> item.id().equals(draft.id())));
        var mismatch = assertThrows(PresentationException.class,
                () -> controllers.publicaciones().tiposPublicacion("mentor-lu"));
        assertEquals(Models.CodigoError.PERMISOS, mismatch.error().codigo());
        controllers.sesionDemo().cambiarUsuario("mentor-lu");
        assertEquals(Models.TipoPublicacion.SERVICIO, controllers.publicaciones().listarPropias("mentor-lu")
                .stream().filter(item -> item.id().equals(draft.id())).findFirst().orElseThrow().tipo());
    }

    @Test
    void conectaLasNuevasOperacionesSinCambiarLaFachadaDeLaUi() {
        var controllers = SkillSwapUiControllers.from(new SkillSwapApplicationWiring());
        var draft = controllers.publicaciones().crearBorrador("user-mateo", "Java",
                "Mentoría", "Tecnología", Modalidad.REMOTA, 60, 100, Models.TipoPublicacion.MENTORIA);
        var updated = controllers.publicaciones().editarBorrador("user-mateo", draft.id(),
                new Models.EdicionBorrador("Java avanzado", "Descripción editada", "Tecnología",
                        Modalidad.HIBRIDA, 60, 6, java.util.List.of("Java")));
        assertEquals("Java avanzado", updated.titulo());
        assertEquals(updated, controllers.publicaciones().listarPropias("user-mateo").stream()
                .filter(item -> item.id().equals(draft.id())).findFirst().orElseThrow());
        var denied = assertThrows(PresentationException.class,
                () -> controllers.publicaciones().listarPropias("otra-persona"));
        assertEquals(Models.CodigoError.PERMISOS, denied.error().codigo());
    }

    @Test
    void conectaLaCreacionDePerfilYTraduceElConflictoDelUsuarioDemo() {
        var controllers = SkillSwapUiControllers.from(new SkillSwapApplicationWiring());
        var input = new Models.NuevoPerfil("Otro nombre", "Bio", "UTC", java.util.List.of(),
                java.util.List.of(), java.util.Set.of(), java.util.List.of(), Models.TipoActor.PROFESIONAL);
        var conflict = assertThrows(PresentationException.class,
                () -> controllers.perfiles().crearPerfil("user-mateo", input));
        assertEquals(Models.CodigoError.CONFLICTO, conflict.error().codigo());
        assertEquals("Mateo Silva", controllers.perfiles().verPerfil("user-mateo").nombre());
    }

    @Test
    void presentaLosErroresDeEntradaConUnContratoUniforme() {
        var controllers = SkillSwapUiControllers.from(new SkillSwapApplicationWiring());
        var search = assertThrows(PresentationException.class, () -> controllers.busqueda().buscar(null));
        assertEquals(Models.CodigoError.VALIDACION, search.error().codigo());
        var publication = assertThrows(PresentationException.class, () -> controllers.publicaciones()
                .crearBorrador("user-mateo", "Mentoría", "Descripción", "Tecnología", null, 60, 100,
                        Models.TipoPublicacion.MENTORIA));
        assertEquals(Models.CodigoError.VALIDACION, publication.error().codigo());
        var wallet = assertThrows(PresentationException.class,
                () -> controllers.wallet().acreditarDemo("user-mateo", -10));
        assertEquals(Models.CodigoError.VALIDACION, wallet.error().codigo());
    }

    @Test
    void presentaNoEncontradoEnOperacionesConYSinRetorno() {
        var controllers = SkillSwapUiControllers.from(new SkillSwapApplicationWiring());
        var booking = assertThrows(PresentationException.class,
                () -> controllers.contrataciones().avanzar("user-mateo", "inexistente"));
        assertEquals(Models.CodigoError.NO_ENCONTRADO, booking.error().codigo());
        var notification = assertThrows(PresentationException.class,
                () -> controllers.notificaciones().marcarLeida("user-mateo", "inexistente"));
        assertEquals(Models.CodigoError.NO_ENCONTRADO, notification.error().codigo());
    }

    @Test
    void presentaErroresDePermisosSinCambiarLaContratacion() {
        var controllers = SkillSwapUiControllers.from(new SkillSwapApplicationWiring());
        var publication = controllers.busqueda().buscar(
                new FiltrosBusqueda("", "Todas", null, 0, "Relevancia")).items().stream()
                .filter(item -> !"Mateo Silva".equals(item.proveedor())).findFirst().orElseThrow();
        var booking = controllers.contrataciones().solicitar("user-mateo", publication.id(),
                "Hola", "Aprender", publication.franjasDisponibles().get(0).id());

        var exception = assertThrows(PresentationException.class,
                () -> controllers.contrataciones().avanzar("user-mateo", booking.id()));

        assertEquals(Models.CodigoError.PERMISOS, exception.error().codigo());
        assertEquals(EstadoContratacion.PENDIENTE, controllers.contrataciones()
                .listarContrataciones("user-mateo").get(0).estado());
    }

    @Test
    void presentaLasTransicionesInvalidasComoErroresDeEstado() {
        var controllers = SkillSwapUiControllers.from(new SkillSwapApplicationWiring());
        var draft = controllers.publicaciones().crearBorrador("user-mateo", "Mentoría",
                "Descripción", "Tecnología", Modalidad.REMOTA, 60, 100, Models.TipoPublicacion.MENTORIA);

        var exception = assertThrows(PresentationException.class,
                () -> controllers.publicaciones().cambiarEstado("user-mateo", draft.id(),
                        Models.EstadoPublicacion.PAUSADA));

        assertEquals(Models.CodigoError.ESTADO, exception.error().codigo());
    }

    @Test
    void usaLaWiringDeAplicacionParaBusquedaYPerfil() {
        var controllers = SkillSwapUiControllers.from(new SkillSwapApplicationWiring());

        var resultados = controllers.busqueda().buscar(
                new FiltrosBusqueda("Java", "Todas", Modalidad.REMOTA, 0, "Relevancia")).items();

        assertFalse(resultados.isEmpty());
        assertTrue(resultados.stream().anyMatch(p -> p.titulo().toLowerCase().contains("java")));

        var perfil = controllers.perfiles().verPerfil("user-mateo");
        assertFalse(perfil.habilidades().isEmpty());
    }

    @Test
    void laBusquedaMuestraElNombreYLaReputacionRealesDelAutor() {
        var controllers = SkillSwapUiControllers.from(new SkillSwapApplicationWiring());

        var resultados = controllers.busqueda().buscar(
                new FiltrosBusqueda("entrevistas", "Todas", null, 0, "Relevancia")).items();

        assertEquals(1, resultados.size());
        assertEquals("Ana García", resultados.get(0).proveedor());
        assertEquals(0.0, resultados.get(0).reputacion());
    }

    @Test
    void laBusquedaDevuelvePaginasConElTotalYNavegacion() {
        var controllers = SkillSwapUiControllers.from(new SkillSwapApplicationWiring());

        var first = controllers.busqueda().buscar(new FiltrosBusqueda("", "Todas", null, null, null, null,
                null, "Menor precio", 0, 2));
        var second = controllers.busqueda().buscar(new FiltrosBusqueda("", "Todas", null, null, null, null,
                null, "Menor precio", 1, 2));

        assertEquals(3, first.total());
        assertEquals(2, first.totalPaginas());
        assertEquals(List.of(150, 180), first.items().stream().map(Models.Publicacion::precio).toList());
        assertFalse(first.hayAnterior());
        assertTrue(first.haySiguiente());
        assertEquals(List.of(220), second.items().stream().map(Models.Publicacion::precio).toList());
        assertTrue(second.hayAnterior());
        assertFalse(second.haySiguiente());
    }

    @Test
    void laBusquedaAplicaLosFiltrosNuevos() {
        var controllers = SkillSwapUiControllers.from(new SkillSwapApplicationWiring());

        var bySkill = controllers.busqueda().buscar(new FiltrosBusqueda("", "Todas", null, "backend", null, null,
                null, null, 0, 20));
        var byModality = controllers.busqueda().buscar(new FiltrosBusqueda("", "Todas", Modalidad.PRESENCIAL,
                null, null, null, null, null, 0, 20));
        var byPrice = controllers.busqueda().buscar(new FiltrosBusqueda("", "Todas", null, null, 160, 200,
                null, null, 0, 20));
        var byReputation = controllers.busqueda().buscar(new FiltrosBusqueda("", "Todas", null, null, null, null,
                1.0, null, 0, 20));

        assertEquals(List.of("Ana García"), bySkill.items().stream().map(Models.Publicacion::proveedor).toList());
        assertEquals(List.of("Lucía Pérez"), byModality.items().stream().map(Models.Publicacion::proveedor).toList());
        assertEquals(List.of(180), byPrice.items().stream().map(Models.Publicacion::precio).toList());
        assertEquals(0, byReputation.total());
    }

    @Test
    void losFiltrosInvalidosSeInformanComoValidacion() {
        var controllers = SkillSwapUiControllers.from(new SkillSwapApplicationWiring());

        var error = assertThrows(PresentationException.class, () -> controllers.busqueda().buscar(
                new FiltrosBusqueda("", "Todas", null, null, 300, 100, null, null, 0, 20)));

        assertEquals(Models.CodigoError.VALIDACION, error.error().codigo());
    }

    @Test
    void noRecomiendaParaOtroUsuario() {
        var controllers = SkillSwapUiControllers.from(new SkillSwapApplicationWiring());

        var error = assertThrows(PresentationException.class,
                () -> controllers.busqueda().recomendar("mentor-ana"));

        assertEquals(Models.CodigoError.PERMISOS, error.error().codigo());
    }

    @Test
    void lasRecomendacionesMuestranElNombreYLaReputacionRealesDelAutor() {
        var controllers = SkillSwapUiControllers.from(new SkillSwapApplicationWiring());

        var recomendaciones = controllers.busqueda().recomendar("user-mateo");

        assertFalse(recomendaciones.isEmpty());
        assertTrue(recomendaciones.stream().allMatch(p ->
                Set.of("Mateo Silva", "Ana García", "Lucía Pérez").contains(p.proveedor())));
        assertTrue(recomendaciones.stream().allMatch(p -> p.reputacion() == 0.0));
    }

        @Test
        void conectaContratacionesWalletYNotificacionesConAplicacion() {
        var controllers = SkillSwapUiControllers.from(new SkillSwapApplicationWiring());
        var publication = controllers.busqueda().buscar(
                new FiltrosBusqueda("", "Todas", null, 0, "Relevancia")).items().stream()
            .filter(item -> !"Mateo Silva".equals(item.proveedor()))
            .findFirst()
            .orElseThrow();

        var requested = controllers.contrataciones().solicitar(
            "user-mateo", publication.id(), "Hola, ¿tenés material previo?", "Practicar el tema",
            publication.franjasDisponibles().get(0).id());
        assertEquals(EstadoContratacion.PENDIENTE, requested.estado());
        assertFalse(controllers.contrataciones().puedeAvanzar("user-mateo", requested.id()));
        var canceled = controllers.contrataciones().cancelar("user-mateo", requested.id());
        assertEquals(EstadoContratacion.CANCELADA, canceled.estado());

        int initialBalance = controllers.wallet().consultarSaldo("user-mateo");
        assertEquals(initialBalance + 300, controllers.wallet().acreditarDemo("user-mateo", 300));
        assertTrue(controllers.wallet().listarMovimientos("user-mateo").stream()
            .anyMatch(movement -> movement.concepto().equals("Créditos demo")));

        assertFalse(controllers.notificaciones().listarNotificaciones("user-mateo").isEmpty());
        controllers.notificaciones().marcarTodasLeidas("user-mateo");
        assertTrue(controllers.notificaciones().listarNotificaciones("user-mateo").stream()
            .allMatch(Models.Notificacion::leida));
        }

    @Test
    void ofreceLosTiposDelUsuarioYCreaElBorradorDelTipoElegido() {
        var controllers = SkillSwapUiControllers.from(new SkillSwapApplicationWiring());

        assertEquals(List.of(Models.TipoPublicacion.MENTORIA, Models.TipoPublicacion.SERVICIO),
                controllers.publicaciones().tiposPublicacion("user-mateo"));
        var service = controllers.publicaciones().crearBorrador("user-mateo", "Revisión de CV",
                "Correcciones y sugerencias.", "Tecnología", Modalidad.REMOTA, 60, 90,
                Models.TipoPublicacion.SERVICIO);
        var withoutType = assertThrows(PresentationException.class, () -> controllers.publicaciones()
                .crearBorrador("user-mateo", "Mentoría Java", "Sesión práctica.", "Tecnología",
                        Modalidad.REMOTA, 60, 90, null));

        assertEquals(Models.TipoPublicacion.SERVICIO, service.tipo());
        assertEquals(Models.CodigoError.VALIDACION, withoutType.error().codigo());
        assertTrue(controllers.publicaciones().listarPropias("user-mateo").stream()
                .anyMatch(item -> item.id().equals(service.id())
                        && item.tipo() == Models.TipoPublicacion.SERVICIO));
    }

    @Test
    void rechazaUnTipoQueElUsuarioNoPuedeCrearComoPermisos() {
        var controllers = SkillSwapUiControllers.from(new SkillSwapApplicationWiring());
        int before = controllers.publicaciones().listarPropias("user-mateo").size();

        var error = assertThrows(PresentationException.class, () -> controllers.publicaciones().crearBorrador(
                "user-mateo", "Hackatón", "Desafío abierto.", "Tecnología", Modalidad.REMOTA, 60, 90,
                Models.TipoPublicacion.DESAFIO));

        assertEquals(Models.CodigoError.PERMISOS, error.error().codigo());
        assertEquals(before, controllers.publicaciones().listarPropias("user-mateo").size());
    }

        @Test
        void creaBorradoresPublicablesConDisponibilidadValida() {
        var controllers = SkillSwapUiControllers.from(new SkillSwapApplicationWiring());
        var draft = controllers.publicaciones().crearBorrador("user-mateo", "Mentoría de integración",
            "Práctica de arquitectura y testing.", "Tecnología", Modalidad.REMOTA, 60, 120,
            Models.TipoPublicacion.MENTORIA);

        var published = controllers.publicaciones().cambiarEstado("user-mateo", draft.id(),
            Models.EstadoPublicacion.PUBLICADA);

        assertEquals(Models.EstadoPublicacion.PUBLICADA, published.estado());
        assertTrue(controllers.busqueda().buscar(
                new FiltrosBusqueda("Mentoría de integración", "Todas", null, 0, "Relevancia")).items()
            .stream().anyMatch(item -> item.id().equals(draft.id())));
        }
}
