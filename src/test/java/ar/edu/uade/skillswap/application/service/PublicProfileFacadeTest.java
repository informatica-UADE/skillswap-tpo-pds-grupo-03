package ar.edu.uade.skillswap.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.uade.skillswap.application.error.ApplicationErrorCode;
import ar.edu.uade.skillswap.application.error.ApplicationException;
import ar.edu.uade.skillswap.application.model.DiscoveryData.SearchQuery;
import ar.edu.uade.skillswap.application.model.PageResult;
import ar.edu.uade.skillswap.application.model.PublicProfileData.PublicProfileQuery;
import ar.edu.uade.skillswap.application.model.PublicProfileData.PublicProfileView;
import ar.edu.uade.skillswap.application.model.PublicProfileData.ReceivedRatingView;
import ar.edu.uade.skillswap.application.model.PublicationData.PublicationView;
import ar.edu.uade.skillswap.application.port.out.BookingRepository;
import ar.edu.uade.skillswap.application.port.out.ProfileRepository;
import ar.edu.uade.skillswap.application.port.out.PublicationRepository;
import ar.edu.uade.skillswap.application.port.out.RatingRepository;
import ar.edu.uade.skillswap.application.port.out.TransactionManager;
import ar.edu.uade.skillswap.domain.model.ActorType;
import ar.edu.uade.skillswap.domain.model.AvailabilitySlot;
import ar.edu.uade.skillswap.domain.model.Booking;
import ar.edu.uade.skillswap.domain.model.Modality;
import ar.edu.uade.skillswap.domain.model.NotificationChannelType;
import ar.edu.uade.skillswap.domain.model.Profile;
import ar.edu.uade.skillswap.domain.model.Publication;
import ar.edu.uade.skillswap.domain.model.PublicationType;
import ar.edu.uade.skillswap.domain.model.Rating;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import java.lang.reflect.RecordComponent;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Facade del perfil público con dobles de los repositorios: compone lecturas sin reglas propias (#40). */
class PublicProfileFacadeTest {
    private static final LocalDateTime T0 = LocalDateTime.of(2026, 10, 1, 10, 0);

    private final Profiles profiles = new Profiles();
    private final Publications publications = new Publications();
    private final Bookings bookings = new Bookings();
    private final Ratings ratings = new Ratings();
    private final Transactions transactions = new Transactions();
    private PublicProfileFacade facade;

    @BeforeEach
    void createFacade() {
        facade = new PublicProfileFacade(profiles, publications, bookings, ratings, transactions);
    }

    @Test
    void armaLaVistaConReputacionPublicacionesActivasEHistorialResumido() {
        Profile ana = profile("ana", true, 4.5, 4);
        profiles.add(ana);
        Publication mentoria = published("Java", "ana");
        publications.page = new PageResult<>(List.of(mentoria), 0, 5, 7);
        bookings.completedAsProvider.put("ana", 3L);
        bookings.completedAsBuyer.put("ana", 1L);
        ratings.recent = List.of(rating("r2", "beto", T0.plusDays(2)), rating("r1", "caro", T0.plusDays(1)));

        PublicProfileView view = facade.viewPublicProfile(new PublicProfileQuery("ana", 0, 5));

        assertEquals("ana", view.userId());
        assertEquals(ActorType.PROFESSIONAL, view.actorType());
        assertEquals("Ana", view.displayName());
        assertEquals("Mentora de Java", view.biography());
        assertEquals(List.of("Java"), view.skills());
        assertEquals(4.5, view.reputation());
        assertEquals(4, view.ratingCount());
        assertEquals(List.of(mentoria.id()), view.activePublications().items().stream().map(PublicationView::id).toList());
        assertEquals(7, view.activePublications().totalElements());
        assertEquals("Ana", view.activePublications().items().get(0).author().displayName());
        assertEquals(3, view.completedBookings().asProvider());
        assertEquals(1, view.completedBookings().asClient());
        assertEquals(4, view.completedBookings().total());
        assertEquals(List.of(5, 5), view.recentRatings().stream().map(ReceivedRatingView::score).toList());
        assertEquals("Comentario r2", view.recentRatings().get(0).comment());
    }

    @Test
    void pideUnaPaginaYConteosSinCargarListadosCompletos() {
        profiles.add(profile("ana", true, 0, 0));
        publications.page = new PageResult<>(List.of(), 2, 10, 0);

        facade.viewPublicProfile(new PublicProfileQuery(" ana ", 2, 10));

        // Los dobles fallan si la Facade usa findByAuthorUserId, findByParticipantUserId o findBySubjectUserId.
        assertEquals(List.of("ana", 2, 10), publications.lastRequest);
        assertEquals(List.of("ana"), bookings.countedProviders);
        assertEquals(List.of("ana"), bookings.countedBuyers);
        assertEquals(List.of("ana", PublicProfileFacade.RECENT_RATINGS), ratings.lastRequest);
    }

    @Test
    void noExponeDatosPrivadosNiQuienEscribioCadaCalificacion() {
        assertEquals(Set.of("userId", "actorType", "displayName", "biography", "skills", "reputation", "ratingCount",
                        "activePublications", "completedBookings", "recentRatings"),
                componentNames(PublicProfileView.class));
        assertEquals(Set.of("score", "comment", "createdAt"), componentNames(ReceivedRatingView.class));
    }

    @Test
    void unPerfilDadoDeBajaNoMuestraNingunaInformacion() {
        profiles.add(profile("ana", false, 4.5, 4));

        ApplicationException error = assertThrows(ApplicationException.class,
                () -> facade.viewPublicProfile(new PublicProfileQuery("ana", 0, 5)));

        assertEquals(ApplicationErrorCode.NOT_FOUND, error.code());
        assertTrue(publications.lastRequest.isEmpty(), "No debe leer publicaciones");
        assertTrue(bookings.countedProviders.isEmpty() && bookings.countedBuyers.isEmpty(), "No debe leer contrataciones");
        assertTrue(ratings.lastRequest.isEmpty(), "No debe leer calificaciones");
    }

    @Test
    void unPerfilInexistenteSeInformaComoNoEncontrado() {
        ApplicationException error = assertThrows(ApplicationException.class,
                () -> facade.viewPublicProfile(new PublicProfileQuery("nadie", 0, 5)));

        assertEquals(ApplicationErrorCode.NOT_FOUND, error.code());
    }

    @Test
    void validaLosDatosDeEntrada() {
        ApplicationException error = assertThrows(ApplicationException.class, () -> facade.viewPublicProfile(null));
        assertEquals(ApplicationErrorCode.VALIDATION, error.code());
        assertThrows(IllegalArgumentException.class, () -> new PublicProfileQuery(" ", 0, 5));
        assertThrows(IllegalArgumentException.class, () -> new PublicProfileQuery("ana", -1, 5));
        assertThrows(IllegalArgumentException.class, () -> new PublicProfileQuery("ana", 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new PublicProfileQuery("ana", 0, PublicProfileQuery.MAX_PAGE_SIZE + 1));
        assertThrows(IllegalArgumentException.class, () -> new PublicProfileQuery("ana", Integer.MAX_VALUE, 2));
    }

    @Test
    void lasCuatroLecturasCompartenUnaTransaccion() {
        profiles.add(profile("ana", true, 0, 0));

        facade.viewPublicProfile(new PublicProfileQuery("ana", 0, 5));

        assertEquals(1, transactions.calls);
        assertFalse(profiles.readOutsideTransaction || publications.readOutsideTransaction
                || bookings.readOutsideTransaction || ratings.readOutsideTransaction);
    }

    // ---------- Fixtures ----------

    private static Set<String> componentNames(Class<? extends Record> type) {
        return Set.copyOf(Arrays.stream(type.getRecordComponents()).map(RecordComponent::getName).toList());
    }

    private static Profile profile(String userId, boolean active, double reputation, int ratingCount) {
        return new Profile(null, userId, ActorType.PROFESSIONAL, "Ana", "Mentora de Java", "America/Buenos_Aires",
                List.of("Java"), List.of("Fotografía"), Set.of(NotificationChannelType.EMAIL),
                List.of(AvailabilitySlot.of(T0.plusDays(10), T0.plusDays(10).plusHours(1), 1)),
                reputation, ratingCount, active);
    }

    private static Publication published(String id, String author) {
        LocalDateTime startsAt = T0.plusDays(30);
        Publication publication = Publication.builder(author, ActorType.PROFESSIONAL, PublicationType.MENTORSHIP)
                .title("Mentoría " + id)
                .description("Sesión " + id)
                .category("Tecnología")
                .modality(Modality.REMOTE)
                .durationMinutes(60)
                .capacity(2)
                .basePrice(new SkillCoins(100))
                .addAvailability(AvailabilitySlot.of(startsAt, startsAt.plusHours(1), 2))
                .build();
        publication.publish(author);
        return publication;
    }

    private static Rating rating(String id, String author, LocalDateTime at) {
        return Rating.restore(id, "booking-" + id, author, "ana", 5, "Comentario " + id, at);
    }

    // ---------- Dobles ----------

    private final class Transactions implements TransactionManager {
        private int calls;
        private boolean active;

        @Override
        public <T> T required(Supplier<T> work) {
            calls++;
            active = true;
            try {
                return work.get();
            } finally {
                active = false;
            }
        }
    }

    private abstract class TransactionalDouble {
        boolean readOutsideTransaction;

        void read() {
            if (!transactions.active) readOutsideTransaction = true;
        }
    }

    private final class Profiles extends TransactionalDouble implements ProfileRepository {
        private final Map<String, Profile> byUserId = new HashMap<>();

        void add(Profile profile) {
            byUserId.put(profile.userId(), profile);
        }

        @Override public Optional<Profile> findByUserId(String userId) {
            read();
            return Optional.ofNullable(byUserId.get(userId));
        }
        @Override public Optional<Profile> findByUserIdForUpdate(String userId) {
            throw new UnsupportedOperationException("Una consulta no bloquea el perfil");
        }
        @Override public List<Profile> findByUserIds(Collection<String> userIds) {
            throw new UnsupportedOperationException("La Facade lee un solo perfil");
        }
        @Override public Profile save(Profile profile) {
            throw new UnsupportedOperationException("Una consulta no modifica perfiles");
        }
    }

    private final class Publications extends TransactionalDouble implements PublicationRepository {
        private PageResult<Publication> page = new PageResult<>(List.of(), 0, 5, 0);
        private List<Object> lastRequest = List.of();

        @Override public PageResult<Publication> findPublishedByAuthor(String authorUserId, int pageNumber, int size) {
            read();
            lastRequest = List.of(authorUserId, pageNumber, size);
            return page;
        }
        @Override public Optional<Publication> findById(String publicationId) { throw unexpected(); }
        @Override public Optional<Publication> findByIdForUpdate(String publicationId) { throw unexpected(); }
        @Override public List<Publication> findByAuthorUserId(String authorUserId) { throw unexpected(); }
        @Override public List<Publication> findPublished() { throw unexpected(); }
        @Override public PageResult<Publication> searchPublished(SearchQuery query) { throw unexpected(); }
        @Override public Publication save(Publication publication) { throw unexpected(); }
    }

    private final class Bookings extends TransactionalDouble implements BookingRepository {
        private final Map<String, Long> completedAsProvider = new HashMap<>();
        private final Map<String, Long> completedAsBuyer = new HashMap<>();
        private final List<String> countedProviders = new ArrayList<>();
        private final List<String> countedBuyers = new ArrayList<>();

        @Override public long countCompletedByProvider(String providerUserId) {
            read();
            countedProviders.add(providerUserId);
            return completedAsProvider.getOrDefault(providerUserId, 0L);
        }
        @Override public long countCompletedByBuyer(String buyerUserId) {
            read();
            countedBuyers.add(buyerUserId);
            return completedAsBuyer.getOrDefault(buyerUserId, 0L);
        }
        @Override public Optional<Booking> findById(String bookingId) { throw unexpected(); }
        @Override public Optional<Booking> findByIdForUpdate(String bookingId) { throw unexpected(); }
        @Override public List<Booking> findByParticipantUserId(String userId) { throw unexpected(); }
        @Override public Booking save(Booking booking) { throw unexpected(); }
    }

    private final class Ratings extends TransactionalDouble implements RatingRepository {
        private List<Rating> recent = List.of();
        private List<Object> lastRequest = List.of();

        @Override public List<Rating> findRecentBySubjectUserId(String subjectUserId, int limit) {
            read();
            lastRequest = List.of(subjectUserId, limit);
            return recent;
        }
        @Override public boolean existsByBookingIdAndAuthorUserId(String bookingId, String authorUserId) {
            throw unexpected();
        }
        @Override public List<Rating> findBySubjectUserId(String subjectUserId) { throw unexpected(); }
        @Override public Rating save(Rating rating) { throw unexpected(); }
    }

    private static UnsupportedOperationException unexpected() {
        return new UnsupportedOperationException("La Facade solo usa las consultas paginadas y los conteos");
    }
}
