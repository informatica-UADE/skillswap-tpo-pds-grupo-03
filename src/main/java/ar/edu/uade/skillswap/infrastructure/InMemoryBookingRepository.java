package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.application.port.out.BookingRepository;
import ar.edu.uade.skillswap.domain.model.Booking;
import ar.edu.uade.skillswap.domain.model.BookingStatus;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryBookingRepository implements BookingRepository {
    private final Map<String, Booking> bookings = new LinkedHashMap<>();

    @Override
    public Optional<Booking> findById(String bookingId) {
        return Optional.ofNullable(bookings.get(bookingId));
    }

    @Override
    public Optional<Booking> findByIdForUpdate(String bookingId) {
        return findById(bookingId);
    }

    @Override
    public List<Booking> findByParticipantUserId(String userId) {
        return bookings.values().stream()
                .filter(booking -> booking.buyerUserId().equals(userId)
                        || booking.providerUserId().equals(userId))
                .toList();
    }

    @Override
    public long countCompletedByProvider(String providerUserId) {
        return bookings.values().stream()
                .filter(booking -> booking.providerUserId().equals(providerUserId))
                .filter(booking -> booking.status() == BookingStatus.COMPLETED)
                .count();
    }

    @Override
    public long countCompletedByBuyer(String buyerUserId) {
        return bookings.values().stream()
                .filter(booking -> booking.buyerUserId().equals(buyerUserId))
                .filter(booking -> booking.status() == BookingStatus.COMPLETED)
                .count();
    }

    @Override
    public Booking save(Booking booking) {
        bookings.put(booking.id(), booking);
        return booking;
    }
}