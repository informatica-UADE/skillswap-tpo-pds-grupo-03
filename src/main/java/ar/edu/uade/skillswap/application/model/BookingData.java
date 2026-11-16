package ar.edu.uade.skillswap.application.model;

import ar.edu.uade.skillswap.domain.model.BookingStatus;
import java.time.LocalDateTime;

/** Input and output models for a booking workflow. */
public final class BookingData {
    private BookingData() {}

    public record RequestCommand(String publicationId, String clientMessage, String objective, String slotId) {
        public RequestCommand(String publicationId, String objective, String slotId) {
            this(publicationId, "", objective, slotId);
        }
    }
    public record BookingCommand(String bookingId) {}
    public record RejectCommand(String bookingId, String reason) {}
    public record CancelCommand(String bookingId, String reason) {}
    public record RescheduleCommand(String bookingId, String newSlotId) {}

    /**
     * Contratación con los nombres visibles de ambas partes, para que la interfaz muestre la
     * contraparte sin consultar cada perfil por separado.
     */
    public record BookingView(String id, String publicationId, String buyerUserId,
                              String providerUserId, String objective, String clientMessage, String slotId,
                              int priceInSkillCoins, int refundInSkillCoins,
                              BookingStatus status, LocalDateTime createdAt, LocalDateTime paidAt,
                              String publicationTitle, String buyerDisplayName, String providerDisplayName) {}
}
