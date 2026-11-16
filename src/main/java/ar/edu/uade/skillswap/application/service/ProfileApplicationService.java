package ar.edu.uade.skillswap.application.service;

import ar.edu.uade.skillswap.application.error.ApplicationErrorCode;
import ar.edu.uade.skillswap.application.error.ApplicationException;
import ar.edu.uade.skillswap.application.mapper.ProfileApplicationMapper;
import ar.edu.uade.skillswap.application.model.ProfileData.ProfileView;
import ar.edu.uade.skillswap.application.model.ProfileData.RegisterCommand;
import ar.edu.uade.skillswap.application.model.ProfileData.UpdateCommand;
import ar.edu.uade.skillswap.application.model.PublicProfileData.CompletedBookingsSummary;
import ar.edu.uade.skillswap.application.port.in.ProfileUseCases;
import ar.edu.uade.skillswap.application.port.out.BookingRepository;
import ar.edu.uade.skillswap.application.port.out.CurrentUserProvider;
import ar.edu.uade.skillswap.application.port.out.ProfileRepository;
import ar.edu.uade.skillswap.application.port.out.TransactionManager;
import ar.edu.uade.skillswap.domain.model.Profile;
import java.util.Objects;

/** Orchestrates profile use cases while Profile owns its invariants. */
public final class ProfileApplicationService implements ProfileUseCases {
    private final ProfileRepository profiles;
    private final BookingRepository bookings;
    private final CurrentUserProvider currentUser;
    private final TransactionManager transactions;

    public ProfileApplicationService(ProfileRepository profiles, BookingRepository bookings,
                                     CurrentUserProvider currentUser, TransactionManager transactions) {
        this.profiles = Objects.requireNonNull(profiles);
        this.bookings = Objects.requireNonNull(bookings);
        this.currentUser = Objects.requireNonNull(currentUser);
        this.transactions = Objects.requireNonNull(transactions);
    }

    @Override
    public ProfileView register(RegisterCommand command) {
        Objects.requireNonNull(command, "Faltan los datos de registro");
        String actor = UseCaseSupport.currentUserId(currentUser);
        return transactions.required(() -> UseCaseSupport.translateDomainFailures(() -> {
            if (profiles.findByUserIdForUpdate(actor).isPresent()) {
                throw new ApplicationException(ApplicationErrorCode.CONFLICT,
                        "Ya existe un perfil para el usuario autenticado");
            }
            Profile profile = Profile.register(actor, command.actorType(), command.displayName(),
                    command.biography(), command.timeZone(), command.skills(), command.interests(),
                    command.notificationChannels(), ProfileApplicationMapper.toAvailabilitySlots(command.availability()));
            return toView(profiles.save(profile));
        }));
    }

    @Override
    public ProfileView update(UpdateCommand command) {
        Objects.requireNonNull(command, "Faltan los datos de actualización");
        String actor = UseCaseSupport.currentUserId(currentUser);
        return transactions.required(() -> UseCaseSupport.translateDomainFailures(() -> {
            Profile profile = profiles.findByUserIdForUpdate(actor)
                    .orElseThrow(() -> notFound("No existe un perfil para el usuario autenticado"));
            profile.updateDetails(actor, command.displayName(), command.biography(), command.timeZone(),
                    command.skills(), command.interests());
            return toView(profiles.save(profile));
        }));
    }

    @Override
    public ProfileView viewMyProfile() {
        String actor = UseCaseSupport.currentUserId(currentUser);
        return transactions.required(() -> toView(profiles.findByUserId(actor)
                .orElseThrow(() -> notFound("No existe un perfil para el usuario autenticado"))));
    }

    @Override
    public void activateMyProfile() {
        String actor = UseCaseSupport.currentUserId(currentUser);
        transactions.required(() -> UseCaseSupport.translateDomainFailures(() -> {
            Profile profile = profiles.findByUserIdForUpdate(actor)
                    .orElseThrow(() -> notFound("No existe un perfil para el usuario autenticado"));
            profile.activate(actor);
            profiles.save(profile);
        }));
    }

    @Override
    public void deactivateMyProfile() {
        String actor = UseCaseSupport.currentUserId(currentUser);
        transactions.required(() -> UseCaseSupport.translateDomainFailures(() -> {
            Profile profile = profiles.findByUserIdForUpdate(actor)
                    .orElseThrow(() -> notFound("No existe un perfil para el usuario autenticado"));
            profile.deactivate(actor);
            profiles.save(profile);
        }));
    }

    /** El historial completado se cuenta en la base, igual que en el perfil público. */
    private ProfileView toView(Profile profile) {
        String userId = profile.userId();
        return ProfileApplicationMapper.toView(profile, new CompletedBookingsSummary(
                bookings.countCompletedByProvider(userId), bookings.countCompletedByBuyer(userId)));
    }

    private static ApplicationException notFound(String message) {
        return new ApplicationException(ApplicationErrorCode.NOT_FOUND, message);
    }
}
