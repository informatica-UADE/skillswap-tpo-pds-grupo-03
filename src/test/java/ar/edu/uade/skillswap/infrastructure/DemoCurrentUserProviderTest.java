package ar.edu.uade.skillswap.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.edu.uade.skillswap.application.port.out.ProfileRepository;
import ar.edu.uade.skillswap.domain.model.ActorType;
import ar.edu.uade.skillswap.domain.model.Profile;
import ar.edu.uade.skillswap.infrastructure.persistence.H2Database;
import ar.edu.uade.skillswap.infrastructure.persistence.JpaTransactionManager;
import ar.edu.uade.skillswap.infrastructure.persistence.repository.JpaProfileRepository;
import jakarta.persistence.EntityManagerFactory;
import ar.edu.uade.skillswap.model.Models.UsuarioDemo;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DemoCurrentUserProviderTest {
    private final EntityManagerFactory factory = H2Database.factory("demo_user");
    private final JpaTransactionManager transactions = new JpaTransactionManager(factory);
    private final ProfileRepository profiles = new JpaProfileRepository(transactions);

    DemoCurrentUserProviderTest() {
        save(profile("user-mateo", "Mateo Silva"));
        save(profile("mentor-ana", "Ana García"));
        Profile inactive = profile("mentor-lu", "Lucía Pérez");
        inactive.deactivate("mentor-lu");
        save(inactive);
    }

    @AfterEach
    void closeDatabase() {
        factory.close();
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

        save(Profile.register("acme", ActorType.ORGANIZATION, "Acme", "", "UTC", Set.of(), Set.of(),
                Set.of()));
        save(profile("nuevo", "Beto Ruiz"));
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

    private void save(Profile profile) {
        transactions.required(() -> profiles.save(profile));
    }

    private static Profile profile(String userId, String name) {
        return Profile.register(userId, ActorType.PROFESSIONAL, name, "", "UTC", Set.of(), Set.of(), Set.of());
    }
}
