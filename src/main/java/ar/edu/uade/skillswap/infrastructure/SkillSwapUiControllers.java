package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.controllers.BusquedaUiController;
import ar.edu.uade.skillswap.controllers.CalificacionUiController;
import ar.edu.uade.skillswap.controllers.ContratacionUiController;
import ar.edu.uade.skillswap.controllers.ControllerExecutor;
import ar.edu.uade.skillswap.controllers.NotificacionUiController;
import ar.edu.uade.skillswap.controllers.PerfilUiController;
import ar.edu.uade.skillswap.controllers.PublicacionUiController;
import ar.edu.uade.skillswap.controllers.UiControllers;
import ar.edu.uade.skillswap.controllers.WalletUiController;

/**
 * Punto de composición de la interfaz (#32): arma una vez cada controller con sus casos de uso y la
 * vista los pide por su accesor. No delega métodos ni tiene lógica; cada pantalla habla con su
 * controller. La sesión demo no es un controller: es el mismo {@link DemoCurrentUserProvider} que
 * leen los casos de uso.
 */
public record SkillSwapUiControllers(BusquedaController busqueda, PublicacionController publicaciones,
                                     ContratacionController contrataciones, CalificacionController calificaciones,
                                     WalletController wallet, NotificacionController notificaciones,
                                     PerfilController perfiles, SesionDemo sesionDemo)
        implements UiControllers {

    public static SkillSwapUiControllers from(SkillSwapApplicationWiring wiring) {
        var identity = wiring.currentUserProvider();
        ControllerExecutor operations = new UiOperationExecutor();
        return new SkillSwapUiControllers(
                new BusquedaUiController(wiring.discoveryService(), wiring.catalogService(), identity, operations),
                new PublicacionUiController(wiring.publicationService(), wiring.publicationService(),
                        identity, operations),
                new ContratacionUiController(wiring.bookingService(), identity, operations),
                new CalificacionUiController(wiring.ratingService(), identity, operations),
                new WalletUiController(wiring.walletService(), identity, operations),
                new NotificacionUiController(wiring.notificationService(), identity, operations),
                new PerfilUiController(wiring.profileService(), wiring.publicProfiles(), identity, operations),
                identity);
    }
}
