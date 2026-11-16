package ar.edu.uade.skillswap.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.edu.uade.skillswap.application.error.ApplicationErrorCode;
import ar.edu.uade.skillswap.application.error.ApplicationException;
import ar.edu.uade.skillswap.application.model.WalletData.MovementView;
import ar.edu.uade.skillswap.application.model.WalletData.WalletView;
import ar.edu.uade.skillswap.application.port.in.WalletUseCases;
import ar.edu.uade.skillswap.controllers.error.PresentationException;
import ar.edu.uade.skillswap.infrastructure.UiOperationExecutor;
import ar.edu.uade.skillswap.model.Models;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class WalletUiControllerTest {
    private static final LocalDateTime AT = LocalDateTime.of(2026, 11, 1, 10, 0);
    private final WalletSpy wallets = new WalletSpy();
    private final WalletUiController controller = new WalletUiController(wallets, () -> "u1", new UiOperationExecutor());

    @Test
    void consultaElSaldoYConvierteLosMovimientosDelUsuarioActivo() {
        assertEquals(380, controller.consultarSaldo("u1"));
        assertEquals(List.of(new Models.Movimiento("Pago", AT, -120, 380)), controller.listarMovimientos(" u1 "));
        assertEquals(2, wallets.views);
    }

    @Test
    void acreditaElImporteDeDemostracionYDevuelveElSaldoNuevo() {
        assertEquals(880, controller.acreditarDemo("u1", 500));
        assertEquals(500, wallets.credited);
    }

    @Test
    void rechazaOtroUsuarioOUnaIdentidadAusenteSinInvocarElCasoDeUso() {
        assertCode(Models.CodigoError.PERMISOS, () -> controller.consultarSaldo("u2"));
        assertCode(Models.CodigoError.PERMISOS, () -> controller.acreditarDemo("u2", 100));
        assertCode(Models.CodigoError.VALIDACION, () -> controller.listarMovimientos(" "));
        var anonymous = new WalletUiController(wallets, () -> null, new UiOperationExecutor());
        assertCode(Models.CodigoError.NO_AUTENTICADO, () -> anonymous.consultarSaldo("u1"));
        assertEquals(0, wallets.views + wallets.credits);
    }

    @Test
    void traduceLosErroresDelCasoDeUso() {
        wallets.failure = new ApplicationException(ApplicationErrorCode.NOT_FOUND, "No existe una billetera");
        assertCode(Models.CodigoError.NO_ENCONTRADO, () -> controller.consultarSaldo("u1"));
        wallets.failure = new ApplicationException(ApplicationErrorCode.VALIDATION, "El importe debe ser positivo");
        assertCode(Models.CodigoError.VALIDACION, () -> controller.acreditarDemo("u1", 0));
    }

    private static void assertCode(Models.CodigoError expected, Runnable action) {
        assertEquals(expected, assertThrows(PresentationException.class, action::run).error().codigo());
    }

    private static final class WalletSpy implements WalletUseCases {
        private int views;
        private int credits;
        private int credited;
        private RuntimeException failure;

        @Override public WalletView viewMyWallet() {
            views++;
            if (failure != null) throw failure;
            return new WalletView("u1", 380, List.of(new MovementView("m1", "Pago", -120, 380, AT)));
        }

        @Override public WalletView creditDemo(int amountInSkillCoins) {
            credits++;
            if (failure != null) throw failure;
            credited = amountInSkillCoins;
            return new WalletView("u1", 380 + amountInSkillCoins, List.of());
        }
    }
}
