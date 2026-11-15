package ar.edu.uade.skillswap.ui;

import ar.edu.uade.skillswap.infrastructure.SkillSwapApplicationWiring;
import ar.edu.uade.skillswap.infrastructure.WiredUiControllers;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SkillSwapViewTest {
    @Test
    void elEstadoNoOcultaTituloObjetivoNiMensajeDeLaContratacion() throws Exception {
        try (var wiring = new SkillSwapApplicationWiring()) {
            var ui = new WiredUiControllers(wiring);
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
