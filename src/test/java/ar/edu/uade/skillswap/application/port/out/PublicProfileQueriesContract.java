package ar.edu.uade.skillswap.application.port.out;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.uade.skillswap.application.model.PageResult;
import ar.edu.uade.skillswap.domain.model.ActorType;
import ar.edu.uade.skillswap.domain.model.AvailabilitySlot;
import ar.edu.uade.skillswap.domain.model.Booking;
import ar.edu.uade.skillswap.domain.model.Modality;
import ar.edu.uade.skillswap.domain.model.Profile;
import ar.edu.uade.skillswap.domain.model.Publication;
import ar.edu.uade.skillswap.domain.model.PublicationType;
import ar.edu.uade.skillswap.domain.model.Rating;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import ar.edu.uade.skillswap.domain.state.BookingState;
import ar.edu.uade.skillswap.domain.state.BookingStates;
import ar.edu.uade.skillswap.domain.state.PublicationState;
import ar.edu.uade.skillswap.domain.state.PublicationStates;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Contrato ejecutable de las consultas que usa el perfil público (#40). Cada implementación
 * (en memoria, JPA) lo extiende e implementa {@link #createRepositories}, que debe dejar persistidos
 * exactamente los datos recibidos. Así las tres consultas se comportan igual con cualquier almacenamiento.
 */
public abstract class PublicProfileQueriesContract {
    protected static final LocalDateTime T0 = LocalDateTime.of(2026, 10, 1, 10, 0);

    /** Repositorios bajo prueba que comparten el mismo almacenamiento. */
    public record Repositories(PublicationRepository publications, BookingRepository bookings,
                                  RatingRepository ratings) {}

    protected abstract Repositories createRepositories(List<Profile> profiles, List<Publication> publications,
                                                       List<Booking> bookings, List<Rating> ratings);

    private Repositories repositories;

    @BeforeEach
    void loadScenario() {
        List<Profile> profiles = List.of(profile("ana"), profile("beto"), profile("caro"), profile("dani"),
                profile("eva"), profile("fede"));
        List<Publication> publications = List.of(
                publication("ana-old", "ana", PublicationStates.published(), T0),
                publication("ana-mid", "ana", PublicationStates.published(), T0.plusDays(1)),
                publication("ana-new", "ana", PublicationStates.published(), T0.plusDays(2)),
                publication("ana-draft", "ana", PublicationStates.draft(), T0.plusDays(3)),
                publication("ana-paused", "ana", PublicationStates.paused(), T0.plusDays(4)),
                publication("ana-closed", "ana", PublicationStates.closed(), T0.plusDays(5)),
                publication("beto-pub", "beto", PublicationStates.published(), T0));
        List<Booking> bookings = List.of(
                booking("k1", "ana-old", "beto", "ana", BookingStates.completed()),
                booking("k2", "ana-mid", "caro", "ana", BookingStates.completed()),
                booking("k4", "ana-new", "dani", "ana", BookingStates.completed()),
                booking("k5", "ana-new", "eva", "ana", BookingStates.confirmed()),
                booking("k6", "ana-old", "fede", "ana", BookingStates.cancelled()),
                booking("k3", "beto-pub", "ana", "beto", BookingStates.completed()),
                booking("k7", "beto-pub", "ana", "beto", BookingStates.pending()));
        List<Rating> ratings = List.of(
                rating("r1", "k1", "beto", "ana", T0.plusDays(1)),
                rating("r3", "k3", "beto", "ana", T0.plusDays(2)),
                rating("r2", "k2", "caro", "ana", T0.plusDays(3)),
                rating("r4", "k4", "dani", "ana", T0.plusDays(4)),
                rating("r5", "k3", "ana", "beto", T0.plusDays(5)));
        repositories = createRepositories(profiles, publications, bookings, ratings);
    }

    // ---------- Publicaciones activas ----------

    @Test
    void listaSoloLasPublicadasDelAutorDeLaMasRecienteALaMasAntigua() {
        PageResult<Publication> page = repositories.publications().findPublishedByAuthor("ana", 0, 10);

        assertEquals(List.of("ana-new", "ana-mid", "ana-old"), ids(page));
        assertEquals(3, page.totalElements());
    }

    @Test
    void paginaEnLaConsultaYCuentaElTotal() {
        PageResult<Publication> first = repositories.publications().findPublishedByAuthor("ana", 0, 2);
        PageResult<Publication> second = repositories.publications().findPublishedByAuthor("ana", 1, 2);
        PageResult<Publication> beyond = repositories.publications().findPublishedByAuthor("ana", 5, 2);

        assertEquals(List.of("ana-new", "ana-mid"), ids(first));
        assertEquals(List.of("ana-old"), ids(second));
        assertTrue(beyond.items().isEmpty());
        assertEquals(3, beyond.totalElements());
        assertEquals(2, first.totalPages());
    }

    @Test
    void noMezclaAutoresNiDevuelveDatosDeUnAutorInexistente() {
        assertEquals(List.of("beto-pub"), ids(repositories.publications().findPublishedByAuthor("beto", 0, 10)));
        PageResult<Publication> none = repositories.publications().findPublishedByAuthor("nadie", 0, 10);
        assertTrue(none.items().isEmpty());
        assertEquals(0, none.totalElements());
    }

    // ---------- Historial ----------

    @Test
    void cuentaSoloContratacionesCompletadasSegunElRol() {
        assertEquals(3, repositories.bookings().countCompletedByProvider("ana"));
        assertEquals(1, repositories.bookings().countCompletedByBuyer("ana"));
        assertEquals(1, repositories.bookings().countCompletedByProvider("beto"));
        assertEquals(1, repositories.bookings().countCompletedByBuyer("beto"));
        assertEquals(0, repositories.bookings().countCompletedByProvider("nadie"));
    }

    // ---------- Calificaciones ----------

    @Test
    void devuelveLasUltimasCalificacionesRecibidasHastaElLimite() {
        assertEquals(List.of("r4", "r2", "r3"),
                ratingIds(repositories.ratings().findRecentBySubjectUserId("ana", 3)));
        assertEquals(List.of("r4"), ratingIds(repositories.ratings().findRecentBySubjectUserId("ana", 1)));
        assertEquals(List.of("r5"), ratingIds(repositories.ratings().findRecentBySubjectUserId("beto", 3)));
        assertTrue(repositories.ratings().findRecentBySubjectUserId("nadie", 3).isEmpty());
    }

    // ---------- Fixtures ----------

    private static List<String> ids(PageResult<Publication> page) {
        return page.items().stream().map(Publication::id).toList();
    }

    private static List<String> ratingIds(List<Rating> ratings) {
        return ratings.stream().map(Rating::id).toList();
    }

    private static Profile profile(String userId) {
        return new Profile(null, userId, ActorType.PROFESSIONAL, "Nombre " + userId, "", "UTC",
                Set.of(), Set.of(), Set.of(), 0, 0, true);
    }

    private static Publication publication(String id, String author, PublicationState state, LocalDateTime created) {
        LocalDateTime startsAt = T0.plusDays(30);
        return new Publication(id, author, PublicationType.MENTORSHIP, "Mentoría " + id, "Sesión " + id,
                "Productividad", Modality.REMOTE, 60, 5, new SkillCoins(100), Set.of(),
                List.of(new AvailabilitySlot(slotOf(id), startsAt, startsAt.plusHours(1), 5, 0)),
                state, created, 0, null, 0, List.of());
    }

    private static Booking booking(String id, String publicationId, String buyer, String provider,
                                   BookingState state) {
        boolean paid = switch (state.status()) {
            case CONFIRMED, IN_PROGRESS, COMPLETED -> true;
            default -> false;
        };
        return new Booking(id, publicationId, buyer, provider, "Objetivo " + id, "", slotOf(publicationId),
                new SkillCoins(100), state, T0, paid ? T0.plusHours(1) : null, SkillCoins.zero(), null,
                false, false, List.of());
    }

    private static Rating rating(String id, String bookingId, String author, String subject, LocalDateTime at) {
        return Rating.restore(id, bookingId, author, subject, 5, "Comentario " + id, at);
    }

    private static String slotOf(String publicationId) {
        return "slot-" + publicationId;
    }
}
