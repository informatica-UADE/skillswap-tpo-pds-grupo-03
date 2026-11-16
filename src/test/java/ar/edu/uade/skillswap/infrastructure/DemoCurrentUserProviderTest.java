package ar.edu.uade.skillswap.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.edu.uade.skillswap.domain.model.ActorType;
import ar.edu.uade.skillswap.domain.model.Profile;
import ar.edu.uade.skillswap.model.Models.UsuarioDemo;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DemoCurrentUserProviderTest {
    private final InMemoryProfileRepository profiles = new InMemoryProfileRepository();

    DemoCurrentUserProviderTest() {
        profiles.save(profile("user-mateo", "Mateo Silva"));
        profiles.save(profile("mentor-ana", "Ana García"));
        Profile inactive = profile("mentor-lu", "Lucía Pérez");
        inactive.deactivate("mentor-lu");
        profiles.save(inactive);
    }

    @Test
    void preservesTheInjectedIdentityUntilTheUserExplicitlySwitches() {
        String[] original = {"user-mateo"};
        var identity = new DemoCurrentUserProvider(() -> original[0], profiles);
        assertEquals("user-mateo", identity.currentUserId());
        original[0] = "mentor-ana";
        assertEquals("mentor-ana", identity.currentUserId());
        identity.cambiarUsuario(" user-mateo ");
        assertEquals("user-mateo", identity.currentUserId());
        assertEquals("user-mateo", identity.usuarioActual());
    }

    @Test
    void losUsuariosDisponiblesSonLosPerfilesActivosGuardadosOrdenadosPorNombre() {
        var identity = new DemoCurrentUserProvider(() -> "user-mateo", profiles);
        assertEquals(List.of(new UsuarioDemo("mentor-ana", "Ana García", "Profesional"),
                new UsuarioDemo("user-mateo", "Mateo Silva", "Profesional")), identity.usuariosDisponibles());
        assertEquals("Ana García · Profesional", identity.usuariosDisponibles().get(0).toString());

        profiles.save(Profile.register("acme", ActorType.ORGANIZATION, "Acme", "", "UTC", Set.of(), Set.of(),
                Set.of()));
        profiles.save(profile("nuevo", "Beto Ruiz"));
        assertEquals(new UsuarioDemo("acme", "Acme", "Organización"), identity.usuariosDisponibles().get(0));
        assertEquals("Beto Ruiz", identity.usuariosDisponibles().get(2).nombre());
        identity.cambiarUsuario("nuevo");
        assertEquals("nuevo", identity.currentUserId());
    }

    @Test
    void rechazaUsuariosSinPerfilDadosDeBajaOVaciosSinCambiarLaSesion() {
        var identity = new DemoCurrentUserProvider(() -> "user-mateo", profiles);
        identity.cambiarUsuario("mentor-ana");
        assertThrows(IllegalArgumentException.class, () -> identity.cambiarUsuario("unknown"));
        assertThrows(IllegalArgumentException.class, () -> identity.cambiarUsuario("mentor-lu"));
        assertThrows(IllegalArgumentException.class, () -> identity.cambiarUsuario(" "));
        assertThrows(IllegalArgumentException.class, () -> identity.cambiarUsuario(null));
        assertEquals("mentor-ana", identity.currentUserId());
    }

    private static Profile profile(String userId, String name) {
        return Profile.register(userId, ActorType.PROFESSIONAL, name, "", "UTC", Set.of(), Set.of(), Set.of());
    }
}
