package ar.edu.uade.skillswap.controllers;

import ar.edu.uade.skillswap.application.error.ApplicationErrorCode;
import ar.edu.uade.skillswap.application.error.ApplicationException;
import ar.edu.uade.skillswap.application.model.PageResult;
import ar.edu.uade.skillswap.application.model.ProfileData.*;
import ar.edu.uade.skillswap.application.model.PublicProfileData.CompletedBookingsSummary;
import ar.edu.uade.skillswap.application.model.PublicProfileData.PublicProfileQuery;
import ar.edu.uade.skillswap.application.model.PublicProfileData.PublicProfileView;
import ar.edu.uade.skillswap.application.model.PublicProfileData.ReceivedRatingView;
import ar.edu.uade.skillswap.application.port.in.ProfileUseCases;
import ar.edu.uade.skillswap.application.port.in.PublicProfileUseCases;
import ar.edu.uade.skillswap.controllers.error.PresentationException;
import ar.edu.uade.skillswap.domain.model.ActorType;
import ar.edu.uade.skillswap.domain.model.NotificationChannelType;
import ar.edu.uade.skillswap.infrastructure.UiOperationExecutor;
import ar.edu.uade.skillswap.model.Models;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class PerfilUiControllerTest {
    private final ProfileSpy service = new ProfileSpy();
    private final PublicProfileSpy publicProfiles = new PublicProfileSpy();
    private final PerfilUiController controller = new PerfilUiController(service, publicProfiles, () -> "u1",
            new UiOperationExecutor());

    @Test
    void convierteLaEntradaYDelegaElRegistroUnaSolaVez() {
        var start = LocalDateTime.of(2026, 11, 4, 15, 0);
        var input = new Models.NuevoPerfil("Julieta", "Bio", "America/Argentina/Buenos_Aires",
                List.of("Java"), List.of("Diseño"), Set.of(Models.CanalNotificacion.EMAIL,
                Models.CanalNotificacion.PUSH, Models.CanalNotificacion.IN_APP),
                List.of(new Models.Disponibilidad("slot", start, start.plusHours(1), 2)), Models.TipoActor.PROFESIONAL);

        var result = controller.crearPerfil(" u1 ", input);

        assertEquals(new RegisterCommand("Julieta", "Bio", "America/Argentina/Buenos_Aires",
                List.of("Java"), List.of("Diseño"), Set.of(NotificationChannelType.EMAIL,
                NotificationChannelType.PUSH, NotificationChannelType.IN_APP),
                List.of(new AvailabilityInput("slot", start, start.plusHours(1), 2)),
                ActorType.PROFESSIONAL), service.command);
        assertEquals(1, service.registrations);
        assertEquals("u1", result.id());
        assertEquals("Resultado del servicio", result.nombre());
        assertEquals(List.of("SQL"), result.habilidades());
        assertEquals(4.5, result.reputacion());
        assertEquals(3, result.resenas());
    }

    @Test
    void consultaElPerfilDesdeElPuertoYConvierteSuResultado() {
        var result = controller.verPerfil("u1");
        assertEquals(1, service.views);
        assertEquals("Resultado del servicio", result.nombre());
        assertEquals("Bio de salida", result.bio());
        assertEquals("Profesional", result.rol());
        assertEquals(5, result.proyectosCompletados());
    }

    // ---------- Perfil público (#40) ----------

    @Test
    void convierteLaConsultaDelPerfilPublicoYSuResultado() {
        var result = controller.verPerfilPublico(" ana ", 1);

        assertEquals(new PublicProfileQuery("ana", 1, Models.PerfilPublico.TAMANIO_PAGINA), publicProfiles.query);
        assertEquals(1, publicProfiles.calls);
        assertEquals(new Models.Perfil("ana", "Ana", "Organización", "Bio pública", List.of("Java"), 4.5, 2, 3),
                result.perfil());
        assertEquals(2, result.completadasComoProveedor());
        assertEquals(1, result.completadasComoCliente());
        assertEquals(List.of(new Models.Resena(5, "Excelente", LocalDateTime.of(2026, 11, 1, 10, 0))),
                result.ultimasResenas());
        assertEquals(1, result.publicaciones().pagina());
        assertEquals(7, result.publicaciones().total());
        assertEquals(2, result.publicaciones().totalPaginas());
    }

    @Test
    void elPerfilPublicoNoExigeSerElUsuarioActivo() {
        var anonymous = new PerfilUiController(service, publicProfiles, () -> null, new UiOperationExecutor());
        assertEquals("Ana", anonymous.verPerfilPublico("ana", 0).perfil().nombre());
        assertEquals("Ana", controller.verPerfilPublico("ana", 0).perfil().nombre());
        assertEquals(2, publicProfiles.calls);
    }

    @Test
    void validaLaConsultaPublicaAntesDeInvocarElPuertoYTraduceErrores() {
        assertCode(Models.CodigoError.VALIDACION, () -> controller.verPerfilPublico(" ", 0));
        assertCode(Models.CodigoError.VALIDACION, () -> controller.verPerfilPublico("ana", -1));
        assertEquals(0, publicProfiles.calls);
        publicProfiles.failure = new ApplicationException(ApplicationErrorCode.NOT_FOUND,
                "El perfil no existe o no está disponible");
        assertCode(Models.CodigoError.NO_ENCONTRADO, () -> controller.verPerfilPublico("ana", 0));
    }

    @Test
    void rechazaSuplantacionAntesDeInvocarElPuerto() {
        assertCode(Models.CodigoError.PERMISOS, () -> controller.verPerfil("otra-persona"));
        assertCode(Models.CodigoError.PERMISOS, () -> controller.crearPerfil("otra-persona", input()));
        assertEquals(0, service.views);
        assertEquals(0, service.registrations);
    }

    @Test
    void rechazaIdentidadAusenteYUsuarioNoInformado() {
        for (String active : new String[] {null, " "}) {
            var anonymous = new PerfilUiController(service, publicProfiles, () -> active, new UiOperationExecutor());
            assertCode(Models.CodigoError.NO_AUTENTICADO, () -> anonymous.verPerfil("u1"));
            assertCode(Models.CodigoError.NO_AUTENTICADO, () -> anonymous.crearPerfil("u1", input()));
        }
        assertCode(Models.CodigoError.VALIDACION, () -> controller.verPerfil(null));
        assertCode(Models.CodigoError.VALIDACION, () -> controller.crearPerfil(" ", input()));
        assertEquals(0, service.views + service.registrations);
    }

    @Test
    void traduceDatosFaltantesYErroresDelCasoDeUso() {
        assertCode(Models.CodigoError.VALIDACION, () -> controller.crearPerfil("u1", null));
        assertEquals(0, service.registrations);
        service.failure = new ApplicationException(ApplicationErrorCode.CONFLICT, "El perfil ya existe");
        assertCode(Models.CodigoError.CONFLICTO, () -> controller.crearPerfil("u1", input()));
        service.failure = new ApplicationException(ApplicationErrorCode.NOT_FOUND, "No existe el perfil");
        assertCode(Models.CodigoError.NO_ENCONTRADO, () -> controller.verPerfil("u1"));
    }

    private Models.NuevoPerfil input() {
        return new Models.NuevoPerfil("Julieta", "Bio", "UTC", List.of(), List.of(), Set.of(), List.of(), Models.TipoActor.PROFESIONAL);
    }

    private void assertCode(Models.CodigoError expected, Runnable action) {
        assertEquals(expected, assertThrows(PresentationException.class, action::run).error().codigo());
    }

    private static class ProfileSpy implements ProfileUseCases {
        private RegisterCommand command;
        private int registrations;
        private int views;
        private RuntimeException failure;

        @Override public ProfileView register(RegisterCommand command) {
            registrations++;
            this.command = command;
            if (failure != null) throw failure;
            return view();
        }
        @Override public ProfileView viewMyProfile() {
            views++;
            if (failure != null) throw failure;
            return view();
        }
        private ProfileView view() {
            return new ProfileView("perfil-1", "u1", ActorType.PROFESSIONAL,
                    "Resultado del servicio", "Bio de salida", "UTC",
                    List.of("SQL"), List.of(), Set.of(), List.of(), 4.5, 3, true, new CompletedBookingsSummary(4, 1));
        }
        @Override public ProfileView update(UpdateCommand command) { throw new AssertionError("No debe actualizar"); }
        @Override public void activateMyProfile() { throw new AssertionError("No debe activar"); }
        @Override public void deactivateMyProfile() { throw new AssertionError("No debe desactivar"); }
    }

    private static class PublicProfileSpy implements PublicProfileUseCases {
        private PublicProfileQuery query;
        private int calls;
        private RuntimeException failure;

        @Override public PublicProfileView viewPublicProfile(PublicProfileQuery query) {
            calls++;
            this.query = query;
            if (failure != null) throw failure;
            return new PublicProfileView(query.userId(), ActorType.ORGANIZATION, "Ana", "Bio pública",
                    List.of("Java"), 4.5, 2, new PageResult<>(List.of(), query.page(), query.size(), 7),
                    new CompletedBookingsSummary(2, 1),
                    List.of(new ReceivedRatingView(5, "Excelente", LocalDateTime.of(2026, 11, 1, 10, 0))));
        }
    }
}
