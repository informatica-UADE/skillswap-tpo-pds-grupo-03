package ar.edu.uade.skillswap.controllers;

import ar.edu.uade.skillswap.model.Models.*;
import java.util.List;

/**
 * UI contracts: success results contain only simple view models.
 * Real implementations translate failures to PresentationException, whose error()
 * contains the stable presentation code and the user-facing message.
 */
public interface UiControllers {
    interface BusquedaController {
        PaginaPublicaciones buscar(FiltrosBusqueda filtros);
        List<Publicacion> recomendar(String userId);
        /** Categorías del catálogo en preorden, con su nivel para mostrar la jerarquía. */
        List<Categoria> categorias();
    }

    interface PublicacionController {
        List<Publicacion> listarPropias(String userId);
        /** Crea un borrador del tipo elegido, que debe ser uno de {@link #tiposPublicacion}. */
        Publicacion crearBorrador(String userId, String titulo, String descripcion, String categoria,
                                  Modalidad modalidad, int duracion, int precio, TipoPublicacion tipo);
        /** Tipos de publicación que puede crear el usuario, para ofrecerlos en el formulario. */
        List<TipoPublicacion> tiposPublicacion(String userId);
        Publicacion crearBorrador(String userId, DatosBorrador datos);
        Publicacion editarBorrador(String userId, String publicacionId, EdicionBorrador datos);
        Publicacion cambiarEstado(String userId, String publicacionId, EstadoPublicacion estado);
    }

    interface ContratacionController {
        List<Contratacion> listarContrataciones(String userId);
        Contratacion solicitar(String userId, String publicacionId, String mensajeCliente,
                               String objetivo, String franjaId);
        Contratacion aceptar(String userId, String contratacionId);
        Contratacion rechazar(String userId, String contratacionId, String motivo);
        Contratacion pagar(String userId, String contratacionId);
        Contratacion cancelar(String userId, String contratacionId);
        Contratacion cancelar(String userId, String contratacionId, String motivo);
        Contratacion proponerReprogramacion(String userId, String contratacionId, String nuevaFranjaId);
        Contratacion aceptarReprogramacion(String userId, String contratacionId);
        Contratacion iniciar(String userId, String contratacionId);
        Contratacion completar(String userId, String contratacionId);
        // Operaciones de compatibilidad con el Mock UI Kit.
        Contratacion avanzar(String userId, String contratacionId);
        boolean puedeAvanzar(String userId, String contratacionId);
    }

    interface CalificacionController {
        Calificacion calificar(String userId, String contratacionId, int puntaje, String comentario);
    }

    /**
     * Login simulado de la demostración local: los usuarios con los que se puede probar la demo y
     * el que está activo. No es un controller ni autenticación real; no hay caso de uso detrás.
     */
    interface SesionDemo {
        /** Perfiles activos guardados (los precargados y los que se registren), ordenados por nombre. */
        List<UsuarioDemo> usuariosDisponibles();
        String usuarioActual();
        /** Cambia el usuario activo; tiene que ser uno de {@link #usuariosDisponibles()}. */
        void cambiarUsuario(String userId);
    }

    interface WalletController {
        int consultarSaldo(String userId);
        List<Movimiento> listarMovimientos(String userId);
        int acreditarDemo(String userId, int importe);
    }

    interface NotificacionController {
        List<Notificacion> listarNotificaciones(String userId);
        void marcarLeida(String userId, String notificacionId);
        void marcarTodasLeidas(String userId);
    }

    interface PerfilController {
        Perfil crearPerfil(String userId, NuevoPerfil datos);
        Perfil verPerfil(String userId);
        /**
         * Perfil público de cualquier usuario activo. No modifica información, así que no exige
         * ser el usuario activo. {@code pagina} empieza en cero y pagina las publicaciones activas.
         */
        PerfilPublico verPerfilPublico(String perfilUserId, int pagina);
    }

    BusquedaController busqueda();
    PublicacionController publicaciones();
    ContratacionController contrataciones();
    CalificacionController calificaciones();
    /** Solo la composición de la demo tiene sesión; el Mock UI Kit no. */
    default SesionDemo sesionDemo() { return null; }
    WalletController wallet();
    NotificacionController notificaciones();
    PerfilController perfiles();
}
