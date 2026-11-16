package ar.edu.uade.skillswap.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.edu.uade.skillswap.application.error.ApplicationErrorCode;
import ar.edu.uade.skillswap.application.error.ApplicationException;
import ar.edu.uade.skillswap.application.model.NotificationData.NotificationView;
import ar.edu.uade.skillswap.application.port.in.NotificationUseCases;
import ar.edu.uade.skillswap.controllers.error.PresentationException;
import ar.edu.uade.skillswap.infrastructure.UiOperationExecutor;
import ar.edu.uade.skillswap.model.Models;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class NotificacionUiControllerTest {
    private static final LocalDateTime AT = LocalDateTime.of(2026, 11, 1, 10, 0);
    private final InboxSpy inbox = new InboxSpy();
    private final NotificacionUiController controller =
            new NotificacionUiController(inbox, () -> "u1", new UiOperationExecutor());

    @Test
    void listaYConvierteLasNotificacionesDelUsuarioActivo() {
        assertEquals(List.of(new Models.Notificacion("n1", "Pago", "Confirmado", AT, false, "PAGO")),
                controller.listarNotificaciones("u1"));
    }

    @Test
    void marcaUnaOTodasComoLeidasDelegandoEnElCasoDeUso() {
        controller.marcarLeida("u1", " n1 ");
        controller.marcarTodasLeidas("u1");

        assertEquals(List.of("read:n1", "readAll"), inbox.calls);
    }

    @Test
    void rechazaOtroUsuarioUnaIdentidadAusenteOUnaNotificacionSinIdSinInvocarElCasoDeUso() {
        assertCode(Models.CodigoError.PERMISOS, () -> controller.listarNotificaciones("u2"));
        assertCode(Models.CodigoError.PERMISOS, () -> controller.marcarTodasLeidas("u2"));
        assertCode(Models.CodigoError.VALIDACION, () -> controller.marcarLeida("u1", " "));
        var anonymous = new NotificacionUiController(inbox, () -> " ", new UiOperationExecutor());
        assertCode(Models.CodigoError.NO_AUTENTICADO, () -> anonymous.marcarLeida("u1", "n1"));
        assertEquals(List.of(), inbox.calls);
    }

    @Test
    void unaNotificacionDeOtroDestinatarioSeInformaComoNoEncontrada() {
        inbox.failure = new ApplicationException(ApplicationErrorCode.NOT_FOUND,
                "No existe una notificación para el usuario autenticado");

        assertCode(Models.CodigoError.NO_ENCONTRADO, () -> controller.marcarLeida("u1", "de-otro"));
    }

    private static void assertCode(Models.CodigoError expected, Runnable action) {
        assertEquals(expected, assertThrows(PresentationException.class, action::run).error().codigo());
    }

    private static final class InboxSpy implements NotificationUseCases {
        private final List<String> calls = new ArrayList<>();
        private RuntimeException failure;

        @Override public List<NotificationView> listMine() {
            return List.of(new NotificationView("n1", "Pago", "Confirmado", AT, false, "PAGO"));
        }

        @Override public void markRead(String notificationId) {
            if (failure != null) throw failure;
            calls.add("read:" + notificationId);
        }

        @Override public void markAllRead() {
            calls.add("readAll");
        }
    }
}
