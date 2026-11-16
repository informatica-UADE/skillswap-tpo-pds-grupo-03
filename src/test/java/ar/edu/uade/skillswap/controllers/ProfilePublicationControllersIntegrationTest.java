package ar.edu.uade.skillswap.controllers;

import ar.edu.uade.skillswap.application.model.DiscoveryData.SearchQuery;
import ar.edu.uade.skillswap.application.service.DiscoveryApplicationService;
import ar.edu.uade.skillswap.application.service.ProfileApplicationService;
import ar.edu.uade.skillswap.application.service.PublicProfileFacade;
import ar.edu.uade.skillswap.application.service.PublicationApplicationService;
import ar.edu.uade.skillswap.controllers.error.PresentationException;
import ar.edu.uade.skillswap.infrastructure.InMemoryBookingRepository;
import ar.edu.uade.skillswap.infrastructure.InMemoryProfileRepository;
import ar.edu.uade.skillswap.infrastructure.InMemoryPublicationRepository;
import ar.edu.uade.skillswap.infrastructure.InMemoryRatingRepository;
import ar.edu.uade.skillswap.infrastructure.SynchronousTransactionManager;
import ar.edu.uade.skillswap.infrastructure.UiOperationExecutor;
import ar.edu.uade.skillswap.model.Models;
import ar.edu.uade.skillswap.domain.model.RecentPopularityStrategy;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;

/** Real use cases and domain, with memory repositories; no JPA or database connection. */
class ProfilePublicationControllersIntegrationTest {
    private String actor = "u1";
    private final InMemoryProfileRepository profileRepository = new InMemoryProfileRepository();
    private final InMemoryPublicationRepository publicationRepository = new InMemoryPublicationRepository(profileRepository);
    private final SynchronousTransactionManager transactions = new SynchronousTransactionManager();
    private final InMemoryBookingRepository bookingRepository = new InMemoryBookingRepository();
    private final PerfilUiController profiles = new PerfilUiController(new ProfileApplicationService(
            profileRepository, bookingRepository, () -> actor, transactions),
            new PublicProfileFacade(profileRepository, publicationRepository, bookingRepository,
                    new InMemoryRatingRepository(), transactions), () -> actor, new UiOperationExecutor());
    private final PublicationApplicationService publicationService = new PublicationApplicationService(
            publicationRepository, profileRepository, () -> actor, transactions);
    private final PublicacionUiController publications = new PublicacionUiController(
            publicationService, publicationService, () -> actor, new UiOperationExecutor());

    @Test
    void registraUnaOrganizacionYCreaEditaYPublicaSuDesafioDesdeLosControllers() {
        profiles.crearPerfil("u1", new Models.NuevoPerfil("Organización", "Bio", "UTC",
                List.of(), List.of(), Set.of(), List.of(), Models.TipoActor.ORGANIZACION));
        assertEquals(ar.edu.uade.skillswap.domain.model.ActorType.ORGANIZATION,
                profileRepository.findByUserId("u1").orElseThrow().actorType());
        assertEquals(List.of(Models.TipoPublicacion.DESAFIO), publications.tiposPublicacion("u1"));
        var input = draft();
        var challenge = publications.crearBorrador("u1", new Models.DatosBorrador(input.titulo(),
                input.descripcion(), input.categoria(), input.modalidad(), input.duracionMinutos(),
                input.cupos(), input.precio(), input.etiquetas(), input.disponibilidad(), Models.TipoPublicacion.DESAFIO));
        var edited = publications.editarBorrador("u1", challenge.id(), edition("Desafío actualizado"));
        assertEquals(Models.TipoPublicacion.DESAFIO, edited.tipo());
        var published = publications.cambiarEstado("u1", challenge.id(), Models.EstadoPublicacion.PUBLICADA);
        assertEquals(Models.TipoPublicacion.DESAFIO, published.tipo());
        assertEquals(Models.EstadoPublicacion.PUBLICADA, published.estado());
        assertCode(Models.CodigoError.PERMISOS, () -> publications.crearBorrador("u1", draft()));
        assertEquals(1, publications.listarPropias("u1").size());
        var catalog = new DiscoveryApplicationService(publicationRepository, publicationRepository,
                profileRepository, () -> actor, new RecentPopularityStrategy())
                .search(new SearchQuery("", null, null, null, null, null, null, null, 0, 20));
        assertTrue(catalog.items().stream().anyMatch(item -> item.id().equals(challenge.id())));
    }

    @Test
    void rechazaTiposAusentesEnLosDtosSinGuardarDatos() {
        assertCode(Models.CodigoError.VALIDACION, () -> profiles.crearPerfil("u1",
                new Models.NuevoPerfil("Julieta", "Bio", "UTC", List.of(), List.of(), Set.of(), List.of(), null)));
        assertTrue(profileRepository.findByUserId("u1").isEmpty());
        profiles.crearPerfil("u1", profile("Julieta"));
        var input = draft();
        assertCode(Models.CodigoError.VALIDACION, () -> publications.crearBorrador("u1",
                new Models.DatosBorrador(input.titulo(), input.descripcion(), input.categoria(),
                        input.modalidad(), input.duracionMinutos(), input.cupos(), input.precio(),
                        input.etiquetas(), input.disponibilidad(), null)));
        assertTrue(publications.listarPropias("u1").isEmpty());
    }

    @Test
    void creaConsultaYRechazaUnPerfilDuplicado() {
        var created = profiles.crearPerfil("u1", profile("Julieta"));
        assertEquals("Julieta", created.nombre());
        assertEquals(created, profiles.verPerfil("u1"));
        assertCode(Models.CodigoError.CONFLICTO, () -> profiles.crearPerfil("u1", profile("Otro")));
        assertEquals("Julieta", profiles.verPerfil("u1").nombre());
    }

    @Test
    void editaPublicaYPausaDelegandoLasTransicionesAlDominio() {
        profiles.crearPerfil("u1", profile("Julieta"));
        var draft = publications.crearBorrador("u1", draft());
        var edited = publications.editarBorrador("u1", draft.id(), edition("Java avanzado"));
        assertEquals("Java avanzado", edited.titulo());
        assertEquals(draft.precio(), edited.precio());
        assertEquals(draft.franjasDisponibles(), edited.franjasDisponibles());
        var published = publications.cambiarEstado("u1", draft.id(), Models.EstadoPublicacion.PUBLICADA);
        assertEquals(Models.EstadoPublicacion.PUBLICADA, published.estado());

        assertCode(Models.CodigoError.ESTADO,
                () -> publications.editarBorrador("u1", draft.id(), edition("No permitido")));

        var paused = publications.cambiarEstado("u1", draft.id(), Models.EstadoPublicacion.PAUSADA);
        assertEquals(Models.EstadoPublicacion.PAUSADA, paused.estado());
        assertEquals(List.of(paused), publications.listarPropias("u1"));
    }

    @Test
    void elUsuarioAutenticadoTampocoPuedeEditarNiPublicarRecursosAjenos() {
        profiles.crearPerfil("u1", profile("Julieta"));
        var draft = publications.crearBorrador("u1", draft());
        actor = "u2";
        assertTrue(publications.listarPropias("u2").isEmpty());
        assertCode(Models.CodigoError.PERMISOS,
                () -> publications.editarBorrador("u2", draft.id(), edition("Ajeno")));
        assertCode(Models.CodigoError.PERMISOS,
                () -> publications.cambiarEstado("u2", draft.id(), Models.EstadoPublicacion.PUBLICADA));
        assertCode(Models.CodigoError.PERMISOS, () -> profiles.verPerfil("u1"));
        actor = "u1";
        assertEquals(List.of(draft), publications.listarPropias("u1"));
    }

    @Test
    void losDatosInvalidosNoSeGuardanYElRecursoInexistenteEsNoEncontrado() {
        assertCode(Models.CodigoError.VALIDACION, () -> profiles.crearPerfil("u1", profile(" ")));
        assertTrue(profileRepository.findByUserId("u1").isEmpty());
        assertCode(Models.CodigoError.NO_ENCONTRADO, () -> profiles.verPerfil("u1"));
        assertCode(Models.CodigoError.NO_ENCONTRADO,
                () -> publications.editarBorrador("u1", "inexistente", edition("Java")));
        assertCode(Models.CodigoError.NO_ENCONTRADO,
                () -> publications.cambiarEstado("u1", "inexistente", Models.EstadoPublicacion.PUBLICADA));
        var start = LocalDateTime.now().plusDays(2);
        var invalid = new Models.DatosBorrador("Java", "Mentoría", "Tecnología", Models.Modalidad.REMOTA,
                60, 2, 100, List.of(), List.of(new Models.Disponibilidad(null, start, start.minusHours(1), 2)), Models.TipoPublicacion.MENTORIA);
        assertCode(Models.CodigoError.VALIDACION, () -> publications.crearBorrador("u1", invalid));
        assertTrue(publications.listarPropias("u1").isEmpty());
    }

    private Models.NuevoPerfil profile(String name) {
        return new Models.NuevoPerfil(name, "Bio", "UTC", List.of("Java"), List.of("SQL"),
                Set.of(Models.CanalNotificacion.IN_APP), List.of(), Models.TipoActor.PROFESIONAL);
    }

    private Models.DatosBorrador draft() {
        var start = LocalDateTime.now().plusDays(2);
        return new Models.DatosBorrador("Java", "Mentoría", "Tecnología", Models.Modalidad.REMOTA,
                60, 2, 100, List.of("OO"), List.of(new Models.Disponibilidad(null, start, start.plusHours(2), 2)), Models.TipoPublicacion.MENTORIA);
    }

    private Models.EdicionBorrador edition(String title) {
        return new Models.EdicionBorrador(title, "Descripción editada", "Tecnología",
                Models.Modalidad.HIBRIDA, 90, 2, List.of("Java"));
    }

    private void assertCode(Models.CodigoError code, Supplier<?> action) {
        assertEquals(code, assertThrows(PresentationException.class, action::get).error().codigo());
    }
}
