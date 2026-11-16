package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.application.port.out.CurrentUserProvider;
import ar.edu.uade.skillswap.application.port.out.ProfileRepository;
import ar.edu.uade.skillswap.controllers.UiControllers;
import ar.edu.uade.skillswap.controllers.mapper.UiModelMapper;
import ar.edu.uade.skillswap.domain.model.Profile;
import ar.edu.uade.skillswap.model.Models.UsuarioDemo;
import java.util.List;
import java.util.Objects;

/**
 * Contexto de la sesión de la demo Swing (login simulado), nunca autenticación real. Los usuarios
 * con los que se puede probar son los perfiles activos guardados: los que precarga el seed y los que
 * se registren. Los casos de uso leen el usuario activo como {@link CurrentUserProvider} y la vista
 * usa este contexto como {@link UiControllers.SesionDemo} para elegirlo.
 */
public final class DemoCurrentUserProvider implements CurrentUserProvider, UiControllers.SesionDemo {
    private final CurrentUserProvider original;
    private final ProfileRepository profiles;
    private final UiModelMapper mapper = new UiModelMapper();
    private String active;

    public DemoCurrentUserProvider(CurrentUserProvider original, ProfileRepository profiles) {
        this.original = Objects.requireNonNull(original);
        this.profiles = Objects.requireNonNull(profiles);
    }

    @Override public String currentUserId() { return active == null ? original.currentUserId() : active; }

    @Override public List<UsuarioDemo> usuariosDisponibles() {
        return profiles.findActive().stream()
                .map(profile -> new UsuarioDemo(profile.userId(), profile.displayName(),
                        mapper.toRol(profile.actorType())))
                .toList();
    }

    @Override public String usuarioActual() { return currentUserId(); }

    /** Solo se puede entrar como un perfil activo guardado. */
    @Override public void cambiarUsuario(String userId) {
        if (userId == null || userId.isBlank()) throw new IllegalArgumentException("Falta el usuario demo");
        String id = userId.trim();
        profiles.findByUserId(id).filter(Profile::active)
                .orElseThrow(() -> new IllegalArgumentException("Usuario demo desconocido o dado de baja"));
        active = id;
    }
}
