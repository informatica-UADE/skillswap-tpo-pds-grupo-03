package ar.edu.uade.skillswap.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class Models {
    private Models() {}

    public enum Modalidad { REMOTA, PRESENCIAL, HIBRIDA }
    public enum TipoActor { PROFESIONAL, ORGANIZACION }
    public enum EstadoPublicacion { BORRADOR, PUBLICADA, PAUSADA, CERRADA, CANCELADA }

    /** Tipo de publicación; {@code toString()} es el texto que muestra la pantalla. */
    public enum TipoPublicacion {
        MENTORIA("Mentoría"), SERVICIO("Servicio"), DESAFIO("Desafío");

        private final String etiqueta;

        TipoPublicacion(String etiqueta) { this.etiqueta = etiqueta; }

        @Override public String toString() { return etiqueta; }
    }
    public enum EstadoContratacion { PENDIENTE, ACEPTADA, CONFIRMADA, EN_CURSO, COMPLETADA, RECHAZADA, CANCELADA }
    public enum CanalNotificacion { EMAIL, PUSH, IN_APP }

    /**
     * Categoría del catálogo jerárquico (#37) con su nivel en el árbol: cero para una categoría raíz.
     * {@code toString()} la muestra con sangría según el nivel, para los combos de la pantalla.
     */
    public record Categoria(String nombre, int nivel) {
        /** Opción "sin filtro" de Explorar; no es una categoría del catálogo. */
        public static final Categoria TODAS = new Categoria("Todas", 0);

        public Categoria {
            if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("Falta el nombre de la categoría");
            if (nivel < 0) throw new IllegalArgumentException("El nivel no puede ser negativo");
        }

        @Override public String toString() { return "    ".repeat(nivel) + nombre; }
    }

    /**
     * Perfil guardado con el que se puede probar la demo y su tipo ("Profesional" u "Organización").
     * {@code toString()} es lo que muestra el selector de perfiles.
     */
    public record UsuarioDemo(String id, String nombre, String rol) {
        @Override public String toString() { return nombre + " · " + rol; }
    }

    public record Disponibilidad(String id, LocalDateTime inicio, LocalDateTime fin, int cupos) {}

    public record NuevoPerfil(String nombre, String bio, String zonaHoraria,
                              List<String> habilidades, List<String> intereses,
                              Set<CanalNotificacion> canales, List<Disponibilidad> disponibilidad,
                              TipoActor tipoActor) {
        public NuevoPerfil {
            habilidades = List.copyOf(habilidades == null ? List.of() : habilidades);
            intereses = List.copyOf(intereses == null ? List.of() : intereses);
            canales = Set.copyOf(canales == null ? Set.of() : canales);
            disponibilidad = List.copyOf(disponibilidad == null ? List.of() : disponibilidad);
        }
    }

    public record DatosBorrador(String titulo, String descripcion, String categoria,
                                Modalidad modalidad, int duracionMinutos, int cupos, int precio,
                                List<String> etiquetas, List<Disponibilidad> disponibilidad,
                                TipoPublicacion tipo) {
        public DatosBorrador {
            etiquetas = List.copyOf(etiquetas == null ? List.of() : etiquetas);
            disponibilidad = List.copyOf(disponibilidad == null ? List.of() : disponibilidad);
        }
    }

    public record EdicionBorrador(String titulo, String descripcion, String categoria,
                                 Modalidad modalidad, int duracionMinutos, int cupos,
                                 List<String> etiquetas) {
        public EdicionBorrador {
            etiquetas = List.copyOf(etiquetas == null ? List.of() : etiquetas);
        }
    }

    public enum CodigoError {
        VALIDACION, NO_AUTENTICADO, NO_ENCONTRADO, PERMISOS, ESTADO,
        CONFLICTO, PERSISTENCIA, INESPERADO
    }

    public record ErrorPresentacion(CodigoError codigo, String mensaje) {
        public ErrorPresentacion {
            Objects.requireNonNull(codigo, "Falta el código del error");
            if (mensaje == null || mensaje.isBlank()) {
                throw new IllegalArgumentException("Falta el mensaje del error");
            }
        }
    }

    public record Perfil(String id, String nombre, String rol, String bio,
                         List<String> habilidades, double reputacion,
                         int resenas, int proyectosCompletados) {
        public Perfil {
            habilidades = List.copyOf(habilidades == null ? List.of() : habilidades);
        }
    }

    public record FranjaDisponibilidad(String id, LocalDateTime inicio,
                                       LocalDateTime fin, int cuposDisponibles) {
        public FranjaDisponibilidad {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("Falta identificador de franja");
            Objects.requireNonNull(inicio, "Falta el inicio de la franja");
            Objects.requireNonNull(fin, "Falta el fin de la franja");
            if (!inicio.isBefore(fin)) throw new IllegalArgumentException("El inicio debe ser anterior al fin");
            if (cuposDisponibles < 0) throw new IllegalArgumentException("Los cupos no pueden ser negativos");
        }

        public boolean disponible() { return cuposDisponibles > 0; }
    }

    public record Publicacion(String id, String titulo, String descripcion,
                              String categoria, Modalidad modalidad, int duracionMinutos,
                              int precio, double reputacion, String proveedor, String proveedorId,
                              List<String> etiquetas, EstadoPublicacion estado,
                              boolean destacada, List<FranjaDisponibilidad> franjasDisponibles,
                              TipoPublicacion tipo) {
        public Publicacion {
            etiquetas = List.copyOf(etiquetas == null ? List.of() : etiquetas);
            franjasDisponibles = List.copyOf(franjasDisponibles == null ? List.of() : franjasDisponibles);
            Objects.requireNonNull(tipo, "Falta el tipo de publicación");
        }
    }

    /** {@code contraparteId} identifica al otro participante para abrir su perfil público; vacío si no se conoce. */
    public record Contratacion(String id, String titulo, String contraparte, String contraparteId,
                               LocalDateTime fecha, int precio, EstadoContratacion estado,
                               String objetivo, String mensajeCliente, String franjaId) {
        public Contratacion(String id, String titulo, String contraparte, LocalDateTime fecha,
                            int precio, EstadoContratacion estado) {
            this(id, titulo, contraparte, "", fecha, precio, estado, "", "", "");
        }
    }

    /** Reseña recibida que muestra el perfil público, sin identificar a quien la escribió. */
    public record Resena(int puntaje, String comentario, LocalDateTime fecha) {}

    /**
     * Perfil público de cualquier usuario activo: sus datos de presentación, una página de sus
     * publicaciones activas, las contrataciones completadas según el rol y las últimas reseñas.
     */
    public record PerfilPublico(Perfil perfil, PaginaPublicaciones publicaciones,
                                int completadasComoProveedor, int completadasComoCliente,
                                List<Resena> ultimasResenas) {
        public static final int TAMANIO_PAGINA = 6;

        public PerfilPublico {
            Objects.requireNonNull(perfil, "Falta el perfil");
            Objects.requireNonNull(publicaciones, "Faltan las publicaciones");
            ultimasResenas = List.copyOf(ultimasResenas == null ? List.of() : ultimasResenas);
        }
    }

    public record Calificacion(String id, String contratacionId, int puntaje,
                               String comentario, LocalDateTime fecha) {}

    public record Movimiento(String concepto, LocalDateTime fecha, int importe, int saldo) {}

    public record Notificacion(String id, String titulo, String detalle,
                               LocalDateTime fecha, boolean leida, String tipo) {}

    /**
     * Filtros de la pantalla Explorar. Los textos vacíos, "Todas" y los valores nulos significan
     * "sin filtro". El orden usa las etiquetas de la pantalla: "Relevancia", "Mejor reputación",
     * "Menor precio" o "Más recientes".
     */
    public record FiltrosBusqueda(String texto, String categoria, Modalidad modalidad, String habilidad,
                                  Integer precioMinimo, Integer precioMaximo, Double reputacionMinima,
                                  String orden, int pagina, int tamanio) {
        public static final int TAMANIO_PAGINA = 20;

        /** Firma usada por la UI provista: sin filtros nuevos, primera página. */
        public FiltrosBusqueda(String texto, String categoria, Modalidad modalidad, int precioMaximo, String orden) {
            this(texto, categoria, modalidad, null, null, precioMaximo > 0 ? precioMaximo : null, null,
                    orden, 0, TAMANIO_PAGINA);
        }
    }

    /** Página de resultados de búsqueda; {@code pagina} empieza en cero. */
    public record PaginaPublicaciones(List<Publicacion> items, int pagina, int tamanio,
                                      long total, int totalPaginas) {
        public PaginaPublicaciones {
            items = List.copyOf(items == null ? List.of() : items);
        }

        public boolean hayAnterior() { return pagina > 0; }
        public boolean haySiguiente() { return pagina + 1 < totalPaginas; }
    }
}
