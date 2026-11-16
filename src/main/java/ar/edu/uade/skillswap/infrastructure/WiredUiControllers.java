package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.application.model.BookingData.BookingView;
import ar.edu.uade.skillswap.controllers.ContratacionUiController;
import ar.edu.uade.skillswap.controllers.CalificacionUiController;
import ar.edu.uade.skillswap.controllers.BusquedaUiController;
import ar.edu.uade.skillswap.controllers.PerfilUiController;
import ar.edu.uade.skillswap.controllers.PublicacionUiController;
import ar.edu.uade.skillswap.controllers.UiControllers;
import ar.edu.uade.skillswap.controllers.mapper.UiModelMapper;
import ar.edu.uade.skillswap.model.Models;

import java.util.List;

public final class WiredUiControllers implements UiControllers,
        UiControllers.BusquedaController,
        UiControllers.PublicacionController,
        UiControllers.WalletController,
        UiControllers.NotificacionController,
        UiControllers.PerfilController,
        UiControllers.SesionDemoController {

    private final SkillSwapApplicationWiring wiring;
    private final PerfilUiController profileController;
    private final CalificacionUiController ratingController;
    private final BusquedaUiController searchController;
    private final ContratacionUiController bookingController;
    private final PublicacionUiController publicationController;

    private final UiModelMapper mapper = new UiModelMapper();
    private final UiOperationExecutor operations = new UiOperationExecutor();

    public WiredUiControllers() {
        this(new SkillSwapApplicationWiring());
    }

    public WiredUiControllers(SkillSwapApplicationWiring wiring) {
        this.wiring = java.util.Objects.requireNonNull(wiring);
        searchController = new BusquedaUiController(wiring.discoveryService(), wiring.currentUserProvider(), operations);
        profileController = new PerfilUiController(wiring.profileService(), wiring.currentUserProvider(), operations);
        ratingController = new CalificacionUiController(wiring.ratingService(), wiring.currentUserProvider(), operations);
        bookingController = new ContratacionUiController(wiring.bookingService(),
                wiring.currentUserProvider(), operations, this::toContratacion);
        publicationController = new PublicacionUiController(
                wiring.publicationService(), wiring.publicationService(),
                wiring.currentUserProvider(), operations);
    }

    @Override
    public BusquedaController busqueda() {
        return this;
    }

    @Override
    public PublicacionController publicaciones() {
        return this;
    }

    @Override
    public ContratacionController contrataciones() {
        return bookingController;
    }

    @Override public CalificacionController calificaciones() { return ratingController; }
    @Override public SesionDemoController sesionDemo() { return this; }
    @Override public String usuarioActual() { return wiring.currentUserProvider().currentUserId(); }
    @Override public void cambiarUsuario(String userId) {
        operations.execute(() -> {
            ((DemoCurrentUserProvider) wiring.currentUserProvider()).switchTo(userId);
        });
    }

    @Override
    public WalletController wallet() {
        return this;
    }

    @Override
    public NotificacionController notificaciones() {
        return this;
    }

    @Override
    public PerfilController perfiles() {
        return this;
    }

    @Override
    public Models.PaginaPublicaciones buscar(Models.FiltrosBusqueda filtros) {
        return searchController.buscar(filtros);
    }

    @Override
    public List<Models.Publicacion> recomendar(String userId) {
        return searchController.recomendar(userId);
    }

    @Override
    public List<String> categorias() {
        return searchController.categorias();
    }

    @Override
    public List<Models.Publicacion> listarPropias(String userId) {
        return publicationController.listarPropias(userId);
    }

    @Override
    public Models.Publicacion crearBorrador(
            String userId,
            String titulo,
            String descripcion,
            String categoria,
            Models.Modalidad modalidad,
            int duracion,
            int precio, Models.TipoPublicacion tipo) {
        return publicationController.crearBorrador(
                userId, titulo, descripcion, categoria, modalidad, duracion, precio, tipo);
    }

    @Override
    public List<Models.TipoPublicacion> tiposPublicacion(String userId) {
        return publicationController.tiposPublicacion(userId);
    }

    @Override
    public Models.Publicacion crearBorrador(
            String userId,
            Models.DatosBorrador datos) {
        return publicationController.crearBorrador(userId, datos);
    }

    @Override
    public Models.Publicacion editarBorrador(
            String userId,
            String publicacionId,
            Models.EdicionBorrador datos) {
        return publicationController.editarBorrador(userId, publicacionId, datos);
    }

    @Override
    public Models.Publicacion cambiarEstado(
            String userId,
            String publicacionId,
            Models.EstadoPublicacion estado) {
        return publicationController.cambiarEstado(userId, publicacionId, estado);
    }

    @Override
    public Models.Perfil crearPerfil(
            String userId,
            Models.NuevoPerfil datos) {
        return profileController.crearPerfil(userId, datos);
    }

    @Override
    public Models.Perfil verPerfil(String userId) {
        return profileController.verPerfil(userId);
    }

    @Override
    public int consultarSaldo(String userId) {
        return operations.execute(() ->
                wiring.walletService().viewMyWallet().balanceInSkillCoins());
    }

    @Override
    public List<Models.Movimiento> listarMovimientos(String userId) {
        return operations.execute(() ->
                wiring.walletService().viewMyWallet().movements().stream()
                        .map(mapper::toMovimiento)
                        .toList());
    }

    @Override
    public int acreditarDemo(String userId, int importe) {
        return operations.execute(() ->
                wiring.walletService().creditDemo(importe).balanceInSkillCoins());
    }

    @Override
    public List<Models.Notificacion> listarNotificaciones(String userId) {
        return operations.execute(() ->
                wiring.notificationService().listMine().stream()
                        .map(mapper::toNotificacion)
                        .toList());
    }

    @Override
    public void marcarLeida(String userId, String notificacionId) {
        operations.execute(() ->
                wiring.notificationService().markRead(notificacionId));
    }

    @Override
    public void marcarTodasLeidas(String userId) {
        operations.execute(() ->
                wiring.notificationService().markAllRead());
    }

    private Models.Contratacion toContratacion(BookingView booking) {
        String otherUserId = booking.buyerUserId().equals(usuarioActual())
                ? booking.providerUserId() : booking.buyerUserId();
        return mapper.toContratacion(booking, booking.publicationTitle(), providerName(otherUserId));
    }

    private String providerName(String authorUserId) {
        return wiring.discoveryService().author(authorUserId).displayName();
    }
}
