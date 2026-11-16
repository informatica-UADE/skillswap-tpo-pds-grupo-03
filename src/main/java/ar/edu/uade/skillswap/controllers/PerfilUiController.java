package ar.edu.uade.skillswap.controllers;

import ar.edu.uade.skillswap.application.port.in.ProfileUseCases;
import ar.edu.uade.skillswap.application.port.in.PublicProfileUseCases;
import ar.edu.uade.skillswap.application.port.out.CurrentUserProvider;
import ar.edu.uade.skillswap.controllers.mapper.UiModelMapper;
import ar.edu.uade.skillswap.model.Models;
import java.util.Objects;

/** Thin profile controller: validates identity, converts data and delegates to the input ports. */
public final class PerfilUiController implements UiControllers.PerfilController {
    private final ProfileUseCases profiles;
    private final PublicProfileUseCases publicProfiles;
    private final CurrentUserProvider identity;
    private final ControllerExecutor operations;
    private final UiModelMapper mapper = new UiModelMapper();

    public PerfilUiController(ProfileUseCases profiles, PublicProfileUseCases publicProfiles,
                              CurrentUserProvider identity, ControllerExecutor operations) {
        this.profiles = Objects.requireNonNull(profiles);
        this.publicProfiles = Objects.requireNonNull(publicProfiles);
        this.identity = Objects.requireNonNull(identity);
        this.operations = Objects.requireNonNull(operations);
    }

    @Override
    public Models.Perfil crearPerfil(String userId, Models.NuevoPerfil datos) {
        return operations.execute(() -> {
            ControllerUser.requireActive(identity, userId);
            return mapper.toPerfil(profiles.register(mapper.toRegisterCommand(datos)));
        });
    }

    @Override
    public Models.Perfil verPerfil(String userId) {
        return operations.execute(() -> {
            ControllerUser.requireActive(identity, userId);
            return mapper.toPerfil(profiles.viewMyProfile());
        });
    }

    /** Consulta pública: no modifica información, así que no exige el usuario activo. */
    @Override
    public Models.PerfilPublico verPerfilPublico(String perfilUserId, int pagina) {
        return operations.execute(() -> mapper.toPerfilPublico(
                publicProfiles.viewPublicProfile(mapper.toPublicProfileQuery(perfilUserId, pagina))));
    }
}
