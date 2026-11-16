package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.application.event.DomainEvent;
import ar.edu.uade.skillswap.domain.model.ActorType;
import ar.edu.uade.skillswap.domain.model.Profile;
import ar.edu.uade.skillswap.domain.model.NotificationChannelType;
import ar.edu.uade.skillswap.infrastructure.notification.NotificationFactory;
import ar.edu.uade.skillswap.infrastructure.notification.email.MockEmailClient;
import ar.edu.uade.skillswap.infrastructure.notification.push.MockPushClient;
import ar.edu.uade.skillswap.infrastructure.persistence.H2Database;
import ar.edu.uade.skillswap.infrastructure.persistence.JpaTransactionManager;
import ar.edu.uade.skillswap.infrastructure.persistence.repository.JpaNotificationRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.repository.JpaProfileRepository;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NotificationEventObserverTest {
    @Test
    void notifiesOnlyTheOtherBookingParticipantAndAllowsMultipleObservers() {
        try (EntityManagerFactory database = H2Database.factory("observer")) {
            var transactions = new JpaTransactionManager(database);
            var repository = new JpaNotificationRepository(transactions);
            var publisher = new InMemoryDomainEventPublisher();
            var profiles = new JpaProfileRepository(transactions);
            transactions.required(() -> profiles.save(Profile.register("provider", ActorType.PROFESSIONAL, "Ana", "",
                    "UTC", List.of(), List.of(), Set.of(NotificationChannelType.IN_APP), List.of())));
            var factory = new NotificationFactory(repository, new MockEmailClient(), new MockPushClient());
            var observer = new NotificationEventObserver(profiles, factory);
            var secondObserverCalls = new AtomicInteger();
            publisher.subscribe("booking.requested", observer::onEvent);
            publisher.subscribe("booking.requested", event -> secondObserverCalls.incrementAndGet());

            transactions.required(() -> {
                publisher.publish(new DomainEvent("booking.requested", "Booking", "booking-1", "buyer",
                        LocalDateTime.now(), Map.of(
                                "buyerUserId", "buyer",
                                "providerUserId", "provider",
                                "detail", "Hay una nueva solicitud.")));
                return null;
            });

            assertEquals(0, repository.findByRecipientUserId("buyer").size());
            assertEquals(1, repository.findByRecipientUserId("provider").size());
            assertEquals(1, secondObserverCalls.get());
        }
    }

    @Test
    void reportsObserverFailuresAfterNotifyingRemainingObservers() {
        var publisher = new InMemoryDomainEventPublisher();
        var laterObserverCalls = new AtomicInteger();
        publisher.subscribe("booking.paid", event -> { throw new IllegalStateException("gateway unavailable"); });
        publisher.subscribe("booking.paid", event -> laterObserverCalls.incrementAndGet());
        var event = new DomainEvent("booking.paid", "Booking", "booking-2", "buyer",
                LocalDateTime.now(), Map.of());

        assertThrows(IllegalStateException.class, () -> publisher.publish(event));
        assertEquals(1, laterObserverCalls.get());
    }
}
