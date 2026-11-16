package ar.edu.uade.skillswap.controllers;

import ar.edu.uade.skillswap.application.model.RatingData.SubmitCommand;
import ar.edu.uade.skillswap.application.port.in.RatingUseCases;
import ar.edu.uade.skillswap.application.port.out.CurrentUserProvider;
import ar.edu.uade.skillswap.controllers.mapper.UiModelMapper;
import ar.edu.uade.skillswap.model.Models;
import java.util.Objects;

/**
 * Convierte y delega la calificación. El rango del puntaje es una regla de {@code Rating}, y la
 * unicidad y que solo califiquen los participantes de una contratación completada son del caso de uso.
 */
public final class CalificacionUiController implements UiControllers.CalificacionController {
    private final RatingUseCases ratings;
    private final CurrentUserProvider identity;
    private final ControllerExecutor operations;
    private final UiModelMapper mapper = new UiModelMapper();

    public CalificacionUiController(RatingUseCases ratings, CurrentUserProvider identity,
                                   ControllerExecutor operations) {
        this.ratings = Objects.requireNonNull(ratings);
        this.identity = Objects.requireNonNull(identity);
        this.operations = Objects.requireNonNull(operations);
    }

    @Override public Models.Calificacion calificar(String userId, String contratacionId,
                                                   int puntaje, String comentario) {
        return operations.execute(() -> {
            ControllerUser.requireActive(identity, userId);
            ControllerUser.required(contratacionId, "Falta la contratación");
            return mapper.toCalificacion(ratings.submit(
                    new SubmitCommand(contratacionId.trim(), puntaje, comentario)));
        });
    }
}
