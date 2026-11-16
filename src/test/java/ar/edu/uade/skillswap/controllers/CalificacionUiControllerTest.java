package ar.edu.uade.skillswap.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.edu.uade.skillswap.application.error.ApplicationErrorCode;
import ar.edu.uade.skillswap.application.error.ApplicationException;
import ar.edu.uade.skillswap.application.model.RatingData.RatingView;
import ar.edu.uade.skillswap.application.model.RatingData.SubmitCommand;
import ar.edu.uade.skillswap.application.port.in.RatingUseCases;
import ar.edu.uade.skillswap.controllers.error.PresentationException;
import ar.edu.uade.skillswap.infrastructure.UiOperationExecutor;
import ar.edu.uade.skillswap.model.Models;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class CalificacionUiControllerTest {
    private static final LocalDateTime AT = LocalDateTime.of(2026, 11, 1, 10, 0);
    private final RatingSpy ratings = new RatingSpy();
    private final CalificacionUiController controller =
            new CalificacionUiController(ratings, () -> "u1", new UiOperationExecutor());

    @Test
    void convierteLaEntradaDelegaUnaVezYConvierteElResultado() {
        var result = controller.calificar("u1", " b1 ", 5, "Muy buena");

        assertEquals(new SubmitCommand("b1", 5, "Muy buena"), ratings.command);
        assertEquals(1, ratings.calls);
        assertEquals(new Models.Calificacion("r1", "b1", 5, "Muy buena", AT), result);
    }

    @Test
    void rechazaOtroUsuarioOUnaContratacionSinIdSinInvocarElCasoDeUso() {
        assertCode(Models.CodigoError.PERMISOS, () -> controller.calificar("u2", "b1", 5, ""));
        assertCode(Models.CodigoError.VALIDACION, () -> controller.calificar("u1", " ", 5, ""));
        var anonymous = new CalificacionUiController(ratings, () -> null, new UiOperationExecutor());
        assertCode(Models.CodigoError.NO_AUTENTICADO, () -> anonymous.calificar("u1", "b1", 5, ""));
        assertEquals(0, ratings.calls);
    }

    /** El puntaje lo valida {@code Rating}; el controller no repite la regla. */
    @Test
    void traduceCalificacionDuplicadaNoAutorizadaOPuntajeInvalido() {
        ratings.failure = new ApplicationException(ApplicationErrorCode.CONFLICT, "La contratación ya fue calificada");
        assertCode(Models.CodigoError.CONFLICTO, () -> controller.calificar("u1", "b1", 5, ""));
        ratings.failure = new ApplicationException(ApplicationErrorCode.FORBIDDEN, "Solo califican los participantes");
        assertCode(Models.CodigoError.PERMISOS, () -> controller.calificar("u1", "b1", 5, ""));
        ratings.failure = new ApplicationException(ApplicationErrorCode.VALIDATION,
                "La calificación debe estar entre 1 y 5");
        assertCode(Models.CodigoError.VALIDACION, () -> controller.calificar("u1", "b1", 9, ""));
        assertEquals(9, ratings.command.score());
    }

    private static void assertCode(Models.CodigoError expected, Runnable action) {
        assertEquals(expected, assertThrows(PresentationException.class, action::run).error().codigo());
    }

    private static final class RatingSpy implements RatingUseCases {
        private SubmitCommand command;
        private int calls;
        private RuntimeException failure;

        @Override public RatingView submit(SubmitCommand command) {
            calls++;
            this.command = command;
            if (failure != null) throw failure;
            return new RatingView("r1", command.bookingId(), "u1", "u2", command.score(), command.comment(), AT);
        }
    }
}
