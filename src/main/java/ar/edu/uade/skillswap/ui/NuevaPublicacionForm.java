package ar.edu.uade.skillswap.ui;

import ar.edu.uade.skillswap.model.Models.Categoria;
import ar.edu.uade.skillswap.model.Models.DatosBorrador;
import ar.edu.uade.skillswap.model.Models.Disponibilidad;
import ar.edu.uade.skillswap.model.Models.Modalidad;
import ar.edu.uade.skillswap.model.Models.TipoPublicacion;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static ar.edu.uade.skillswap.ui.Theme.button;

/**
 * Formulario de nueva publicación con todos los datos del borrador. Solo junta lo que se carga:
 * las reglas (precio, cupos, franjas superpuestas, etc.) las valida el dominio al crear el borrador.
 */
final class NuevaPublicacionForm extends JPanel {
    private static final DateTimeFormatter DIA_Y_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    final JComboBox<TipoPublicacion> tipo;
    final JTextField titulo = new JTextField(26);
    final JTextArea descripcion = new JTextArea(3, 26);
    final JComboBox<Categoria> categoria;
    final JComboBox<Modalidad> modalidad = new JComboBox<>(Modalidad.values());
    final JSpinner duracion = new JSpinner(new SpinnerNumberModel(60, 1, 600, 15));
    final JSpinner cupos = new JSpinner(new SpinnerNumberModel(6, 1, 100, 1));
    final JSpinner precio = new JSpinner(new SpinnerNumberModel(150, 1, 100_000, 10));
    final JTextField etiquetas = new JTextField(26);
    final JSpinner inicioFranja = new JSpinner(new SpinnerDateModel());
    final DefaultListModel<LocalDateTime> franjas = new DefaultListModel<>();

    NuevaPublicacionForm(List<TipoPublicacion> tipos, List<Categoria> categorias) {
        super(new GridBagLayout());
        tipo = new JComboBox<>(tipos.toArray(TipoPublicacion[]::new));
        categoria = new JComboBox<>(categorias.toArray(Categoria[]::new));
        descripcion.setLineWrap(true);
        descripcion.setWrapStyleWord(true);
        etiquetas.setToolTipText("Separadas por coma, por ejemplo: Java, Backend, Entrevistas");
        inicioFranja.setEditor(new JSpinner.DateEditor(inicioFranja, "dd/MM/yyyy HH:mm"));
        inicioFranja.setValue(toDate(LocalDateTime.now().plusDays(3).truncatedTo(ChronoUnit.HOURS)));

        fila("Tipo", tipo);
        fila("Título", titulo);
        fila("Descripción", new JScrollPane(descripcion));
        fila("Categoría", categoria);
        fila("Modalidad", modalidad);
        fila("Duración (min)", duracion);
        fila("Cupos", cupos);
        fila("Precio (SkillCoins)", precio);
        fila("Etiquetas", etiquetas);
        fila("Franjas", franjasDisponibles());
    }

    /** Lo cargado, tal cual, para crear el borrador. Cada franja dura la sesión y tiene los cupos de la publicación. */
    DatosBorrador datos() {
        int minutos = (Integer) duracion.getValue();
        int cuposPublicacion = (Integer) cupos.getValue();
        List<Disponibilidad> disponibilidad = new ArrayList<>();
        for (int i = 0; i < franjas.size(); i++) {
            LocalDateTime inicio = franjas.get(i);
            disponibilidad.add(new Disponibilidad(null, inicio, inicio.plusMinutes(minutos), cuposPublicacion));
        }
        Categoria elegida = (Categoria) categoria.getSelectedItem();
        return new DatosBorrador(titulo.getText(), descripcion.getText(), elegida == null ? null : elegida.nombre(),
                (Modalidad) modalidad.getSelectedItem(), minutos, cuposPublicacion, (Integer) precio.getValue(),
                etiquetasCargadas(), disponibilidad, (TipoPublicacion) tipo.getSelectedItem());
    }

    /** Agrega a la lista la franja que empieza en la fecha y hora elegidas. */
    void agregarFranja() {
        Date inicio = (Date) inicioFranja.getValue();
        franjas.addElement(LocalDateTime.ofInstant(inicio.toInstant(), ZoneId.systemDefault())
                .truncatedTo(ChronoUnit.MINUTES));
    }

    private List<String> etiquetasCargadas() {
        return Arrays.stream(etiquetas.getText().split(","))
                .map(String::trim)
                .filter(etiqueta -> !etiqueta.isEmpty())
                .toList();
    }

    private JComponent franjasDisponibles() {
        JList<LocalDateTime> lista = new JList<>(franjas);
        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.setVisibleRowCount(3);
        lista.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof LocalDateTime inicio) {
                    setText(inicio.format(DIA_Y_HORA) + " – "
                            + inicio.plusMinutes((Integer) duracion.getValue()).format(HORA));
                }
                return this;
            }
        });
        duracion.addChangeListener(event -> lista.repaint());

        JButton agregar = button("Agregar franja", false);
        agregar.addActionListener(event -> agregarFranja());
        JButton quitar = button("Quitar", false);
        quitar.addActionListener(event -> {
            int elegida = lista.getSelectedIndex();
            if (elegida >= 0) franjas.remove(elegida);
        });

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        acciones.add(inicioFranja);
        acciones.add(agregar);
        acciones.add(quitar);
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.add(acciones, BorderLayout.NORTH);
        panel.add(new JScrollPane(lista), BorderLayout.CENTER);
        return panel;
    }

    private void fila(String nombre, JComponent campo) {
        GridBagConstraints etiqueta = new GridBagConstraints();
        etiqueta.gridx = 0;
        etiqueta.anchor = GridBagConstraints.NORTHWEST;
        etiqueta.insets = new Insets(4, 0, 4, 12);
        add(new JLabel(nombre), etiqueta);

        GridBagConstraints valor = new GridBagConstraints();
        valor.gridx = 1;
        valor.fill = GridBagConstraints.HORIZONTAL;
        valor.weightx = 1;
        valor.insets = new Insets(4, 0, 4, 0);
        add(campo, valor);
    }

    private static Date toDate(LocalDateTime value) {
        return Date.from(value.atZone(ZoneId.systemDefault()).toInstant());
    }
}
