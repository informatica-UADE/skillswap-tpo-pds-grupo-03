package ar.edu.uade.skillswap.ui;

import ar.edu.uade.skillswap.infrastructure.SkillSwapApplicationWiring;
import ar.edu.uade.skillswap.infrastructure.SkillSwapUiControllers;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SkillSwapViewTest {
    @Test
    void elEstadoNoOcultaTituloObjetivoNiMensajeDeLaContratacion() throws Exception {
        try (var wiring = new SkillSwapApplicationWiring()) {
            var ui = SkillSwapUiControllers.from(wiring);
            var publication = ui.publicaciones().listarPropias("user-mateo").get(0);
            ui.sesionDemo().cambiarUsuario("mentor-ana");
            ui.contrataciones().solicitar("mentor-ana", publication.id(), "Mensaje visible", "Objetivo visible",
                    publication.franjasDisponibles().get(0).id());
            ui.sesionDemo().cambiarUsuario("user-mateo");
            SwingUtilities.invokeAndWait(() -> {
                var view = new SkillSwapView(ui);
                descendants(view).stream().filter(JButton.class::isInstance).map(JButton.class::cast)
                        .filter(button -> button.getText().contains("Contrataciones"))
                        .findFirst().orElseThrow().doClick();
                view.setSize(1365, 820);
                layout(view);
                for (String text : List.of(publication.titulo(), "Objetivo: Objetivo visible", "Mensaje: Mensaje visible")) {
                    JLabel label = descendants(view).stream().filter(JLabel.class::isInstance).map(JLabel.class::cast)
                            .filter(item -> text.equals(item.getText())).findFirst().orElseThrow();
                    assertTrue(label.getWidth() > 0 && label.getHeight() > 0, text + " quedó oculto");
                    assertTrue(label.getParent().getWidth() > 0, text + " no tiene espacio en la fila");
                }
            });
        }
    }

    @Test
    void elNombreDelUsuarioAbreSuPerfilPublicoDesdeTarjetasDetalleYContratacionesYVuelve() throws Exception {
        try (var wiring = new SkillSwapApplicationWiring()) {
            var ui = SkillSwapUiControllers.from(wiring);
            var publication = ui.publicaciones().listarPropias("user-mateo").get(0);
            ui.sesionDemo().cambiarUsuario("mentor-ana");
            ui.contrataciones().solicitar("mentor-ana", publication.id(), "", "Objetivo",
                    publication.franjasDisponibles().get(0).id());
            ui.sesionDemo().cambiarUsuario("user-mateo");
            SwingUtilities.invokeAndWait(() -> {
                var view = new SkillSwapView(ui);

                // Desde la tarjeta de Inicio, sin entrar al detalle.
                clickProfileLink(view, "Ana García");
                assertTrue(hasLabel(view, "Perfil público"));
                assertTrue(hasLabel(view, "Historial"));
                click(view, "← Volver");
                assertTrue(hasLabel(view, "Inicio"));

                // Desde Contrataciones: la contraparte.
                click(view, "✓  Contrataciones");
                clickProfileLink(view, "Ana García");
                assertTrue(hasLabel(view, "Perfil público"));
                click(view, "← Volver");
                assertTrue(hasLabel(view, "Tus contrataciones"));

                // Desde el detalle de una publicación: su autor.
                click(view, "⌕  Explorar");
                click(view, "Ver detalle");
                List<JLabel> authorLinks = profileLinks(view);
                assertEquals(1, authorLinks.size());
                click(authorLinks.get(0));
                assertTrue(hasLabel(view, "Perfil público"));
                assertTrue(hasLabel(view, "Publicaciones activas · Página 1 de 1"));
                click(view, "← Volver");
                assertTrue(hasLabel(view, "Detalle"));
            });
        }
    }

    @Test
    void elRecuadroDelPerfilActivoEsElQueCambiaDePerfilEnLaDemo() throws Exception {
        try (var wiring = new SkillSwapApplicationWiring()) {
            var ui = SkillSwapUiControllers.from(wiring);
            SwingUtilities.invokeAndWait(() -> {
                var view = new SkillSwapView(ui);
                var switcher = descendants(view).stream().filter(JPanel.class::isInstance).map(JPanel.class::cast)
                        .filter(panel -> "Cambiar de perfil".equals(panel.getToolTipText())).toList();
                assertEquals(1, switcher.size());
                assertTrue(descendants(switcher.get(0)).stream().filter(JLabel.class::isInstance)
                        .anyMatch(label -> "Mateo Silva".equals(((JLabel) label).getText())));
                assertTrue(switcher.get(0).getMouseListeners().length > 0);
                assertTrue(buttons(view).stream().noneMatch(button -> button.getText().contains("Cambiar usuario")));
            });
        }
    }

    private static void clickProfileLink(Container root, String name) {
        JLabel link = profileLinks(root).stream()
                .filter(label -> ("Ver el perfil público de " + name).equals(label.getToolTipText()))
                .findFirst().orElseThrow();
        assertEquals("<html><u>" + name + "</u></html>", link.getText());
        click(link);
    }

    private static void click(JLabel link) {
        var event = new MouseEvent(link, MouseEvent.MOUSE_CLICKED, 0, 0, 1, 1, 1, false);
        for (MouseListener listener : link.getMouseListeners()) listener.mouseClicked(event);
    }

    private static List<JLabel> profileLinks(Container root) {
        return descendants(root).stream().filter(JLabel.class::isInstance).map(JLabel.class::cast)
                .filter(label -> label.getToolTipText() != null
                        && label.getToolTipText().startsWith("Ver el perfil público de ")).toList();
    }

    private static void click(Container root, String text) {
        buttons(root).stream().filter(button -> text.equals(button.getText())).findFirst().orElseThrow().doClick();
    }

    private static List<JButton> buttons(Container root) {
        return descendants(root).stream().filter(JButton.class::isInstance).map(JButton.class::cast).toList();
    }

    private static boolean hasLabel(Container root, String text) {
        return descendants(root).stream().filter(JLabel.class::isInstance).map(JLabel.class::cast)
                .anyMatch(label -> text.equals(label.getText()));
    }

    private static List<Component> descendants(Container root) {
        List<Component> items = new ArrayList<>();
        for (Component component : root.getComponents()) {
            items.add(component);
            if (component instanceof Container child) items.addAll(descendants(child));
        }
        return items;
    }

    private static void layout(Container root) {
        root.doLayout();
        for (Component component : root.getComponents()) {
            if (component instanceof Container child) layout(child);
        }
    }
}
