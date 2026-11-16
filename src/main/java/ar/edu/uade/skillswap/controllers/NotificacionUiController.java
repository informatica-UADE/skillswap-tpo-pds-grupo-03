package ar.edu.uade.skillswap.controllers;

import ar.edu.uade.skillswap.application.port.in.NotificationUseCases;
import ar.edu.uade.skillswap.application.port.out.CurrentUserProvider;
import ar.edu.uade.skillswap.controllers.mapper.UiModelMapper;
import ar.edu.uade.skillswap.model.Models;
import java.util.List;
import java.util.Objects;

/**
 * Centro de notificaciones: valida que el usuario sea el activo y delega. Que solo el destinatario
 * pueda leer y marcar sus notificaciones lo controla el caso de uso.
 */
public final class NotificacionUiController implements UiControllers.NotificacionController {
    private final NotificationUseCases notifications;
    private final CurrentUserProvider identity;
    private final ControllerExecutor operations;
    private final UiModelMapper mapper = new UiModelMapper();

    public NotificacionUiController(NotificationUseCases notifications, CurrentUserProvider identity,
                                    ControllerExecutor operations) {
        this.notifications = Objects.requireNonNull(notifications);
        this.identity = Objects.requireNonNull(identity);
        this.operations = Objects.requireNonNull(operations);
    }

    @Override
    public List<Models.Notificacion> listarNotificaciones(String userId) {
        return operations.execute(() -> {
            ControllerUser.requireActive(identity, userId);
            return notifications.listMine().stream().map(mapper::toNotificacion).toList();
        });
    }

    @Override
    public void marcarLeida(String userId, String notificacionId) {
        operations.execute(() -> {
            ControllerUser.requireActive(identity, userId);
            ControllerUser.required(notificacionId, "Falta la notificación");
            notifications.markRead(notificacionId.trim());
            return null;
        });
    }

    @Override
    public void marcarTodasLeidas(String userId) {
        operations.execute(() -> {
            ControllerUser.requireActive(identity, userId);
            notifications.markAllRead();
            return null;
        });
    }
}
