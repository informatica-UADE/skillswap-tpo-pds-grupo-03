package ar.edu.uade.skillswap.controllers;

import ar.edu.uade.skillswap.application.model.PublicationData.ChangeStatusCommand;
import ar.edu.uade.skillswap.application.model.PublicationData.PublicationView;
import ar.edu.uade.skillswap.application.port.in.PublicationCommandUseCases;
import ar.edu.uade.skillswap.application.port.in.PublicationQueryUseCases;
import ar.edu.uade.skillswap.application.port.out.CurrentUserProvider;
import ar.edu.uade.skillswap.controllers.mapper.UiModelMapper;
import ar.edu.uade.skillswap.model.Models;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/** Publication rules and author permissions are enforced by the application/domain. */
public final class PublicacionUiController implements UiControllers.PublicacionController {
    private final PublicationCommandUseCases commands;
    private final PublicationQueryUseCases queries;
    private final CurrentUserProvider identity;
    private final ControllerExecutor operations;
    private final UiModelMapper mapper = new UiModelMapper();

    public PublicacionUiController(PublicationCommandUseCases commands, PublicationQueryUseCases queries,
                                   CurrentUserProvider identity,
                                   ControllerExecutor operations) {
        this.commands = Objects.requireNonNull(commands);
        this.queries = Objects.requireNonNull(queries);
        this.identity = Objects.requireNonNull(identity);
        this.operations = Objects.requireNonNull(operations);
    }

    @Override
    public List<Models.Publicacion> listarPropias(String userId) {
        return operations.execute(() -> {
            ControllerUser.requireActive(identity, userId);
            return queries.listMyPublications().stream().map(this::toUi).toList();
        });
    }

    @Override
    public Models.Publicacion crearBorrador(String userId, Models.DatosBorrador datos) {
        return operations.execute(() -> {
            ControllerUser.requireActive(identity, userId);
            return toUi(commands.createDraft(mapper.toCreateDraftCommand(datos)));
        });
    }

    @Override
    public Models.Publicacion crearBorrador(String userId, String titulo, String descripcion,
                                           String categoria, Models.Modalidad modalidad,
                                           int duracion, int precio, Models.TipoPublicacion tipo) {
        // The supplied form uses one demo slot and the existing capacity of six.
        return operations.execute(() -> {
            ControllerUser.requireActive(identity, userId);
            LocalDateTime inicio = LocalDateTime.now().plusDays(1).withSecond(0).withNano(0);
            return toUi(commands.createDraft(mapper.toCreateDraftCommand(new Models.DatosBorrador(
                    titulo, descripcion, categoria, modalidad, duracion, 6, precio, List.of(),
                    List.of(new Models.Disponibilidad(null, inicio, inicio.plusMinutes(duracion), 6)), tipo))));
        });
    }

    @Override
    public List<Models.TipoPublicacion> tiposPublicacion(String userId) {
        return operations.execute(() -> {
            ControllerUser.requireActive(identity, userId);
            return queries.allowedPublicationTypes().stream().map(mapper::toTipoPublicacion).toList();
        });
    }

    @Override
    public Models.Publicacion editarBorrador(String userId, String publicacionId, Models.EdicionBorrador datos) {
        return operations.execute(() -> {
            ControllerUser.requireActive(identity, userId);
            ControllerUser.required(publicacionId, "Falta el identificador de publicación");
            return toUi(commands.editDraft(mapper.toEditDraftCommand(publicacionId.trim(), datos)));
        });
    }

    @Override
    public Models.Publicacion cambiarEstado(String userId, String publicacionId, Models.EstadoPublicacion estado) {
        return operations.execute(() -> {
            ControllerUser.requireActive(identity, userId);
            ControllerUser.required(publicacionId, "Falta el identificador de publicación");
            return toUi(commands.changeStatus(new ChangeStatusCommand(publicacionId.trim(),
                    mapper.toPublicationStatus(estado), "Cambio ejecutado desde la UI")));
        });
    }

    private Models.Publicacion toUi(PublicationView view) {
        return mapper.toPublicacion(view);
    }
}
