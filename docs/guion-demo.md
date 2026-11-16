# Guion de demo: SkillSwap

## Objetivo

Mostrar la experiencia Swing conectada a casos de uso de Application para perfiles, publicaciones, búsqueda, contrataciones, billetera, calificaciones y notificaciones. La app inicia como `Mateo Silva`; al hacer clic en el recuadro del perfil activo (abajo, en la barra lateral) se elige cualquier perfil activo cargado, con su tipo. Usa repositorios en memoria.

## Preparación

Desde la raíz del repositorio, ejecutar:

```bash
mvn clean test
mvn exec:java
```

La primera orden valida compilación y pruebas; la segunda abre la ventana Swing. No se requiere una base de datos para esta demo.

## Recorrido disponible

1. **Inicio y recomendaciones.** Mostrar el inicio, las recomendaciones y las categorías.
2. **Explorar una mentoría.** Abrir **Explorar**, filtrar por texto/categoría/orden y entrar en el detalle de una publicación. El combo de categorías muestra el catálogo jerárquico con sangría: elegir **Tecnología** incluye sus subcategorías, y **Diseño UX** encuentra la publicación de Lucía por sus habilidades (#37).
3. **Solicitar.** Presionar **Solicitar mentoría**. La solicitud real de aplicación aparece en **Contrataciones** como `PENDIENTE`; se puede cancelar como comprador.
4. **Gestionar una contratación.** Las acciones de transición aparecen según el participante activo. Hacer clic en el recuadro del perfil activo para seleccionar al proveedor de la solicitud y aceptarla; volver a Mateo para pagar; el proveedor inicia la sesión y el cliente la completa. Comprobar que el saludo, nombre, avatar y rol del pie lateral corresponden a cada identidad y que el saldo del encabezado coincide con **SkillCoins**. Después de pagar, completar, cancelar o acreditar, volver a comparar ambos saldos.
5. **Publicar.** Abrir **Mis publicaciones** → **+ Nueva publicación** y completar tipo, título, descripción, categoría del catálogo, modalidad, duración, cupos, precio, etiquetas (separadas por coma) y una o más franjas (fecha y hora de inicio; cada franja dura la sesión). Si el dominio rechaza algún dato, se muestra el error y el formulario vuelve con lo cargado. Presionar **Publicar**: la publicación aparece en la búsqueda, también al filtrar por sus etiquetas.
6. **Billetera y notificaciones.** **SkillCoins** consulta saldo y ledger; **Acreditar demo** registra un movimiento en el agregado Wallet. **Notificaciones** permite marcar una o todas como leídas.
7. **Calificar y mostrar el perfil.** Como participante de una contratación completada, usar **Calificar**, ingresar puntuación y comentario y comprobar la reputación del destinatario. Verificar también el rechazo de una calificación duplicada. La sesión de demo no reemplaza autenticación real.

El recorrido se ejecutó el 15/11/2026 con Lucía como proveedora y Ana como compradora. El [registro visual con capturas](validacion-visual-pr65.md) documenta las acciones, saldos y mensajes observados.

## Límites de esta demo

La demo integra los servicios y reglas disponibles, pero todavía no es una entrega de producción:

- El formulario de solicitud permite elegir horario e ingresar objetivo y mensaje. Verificar esos datos al recorrer la contratación.
- El pago actual debita al comprador y acredita directamente al proveedor. No hay custodia ni liberación al completar; esa diferencia respecto del criterio de #19 se debe resolver por separado. Los reembolsos siguen la política disponible. Son saldos ficticios y no existe pasarela de pago.
- El selector lista los perfiles activos guardados (con el seed: `user-mateo`, `mentor-ana`, `mentor-lu`); no es un sistema de autenticación.
- En memoria, `SynchronousTransactionManager` no aporta rollback ni bloqueo y el estado se pierde al reiniciar. El modo `--persistence=jpa` usa MySQL y `JpaTransactionManager`, con la configuración del README.
- El botón de calificación está disponible en contrataciones completadas. El registro visual comprueba la reputación de Lucía en 5,0 con una reseña y el rechazo de duplicados.

La persistencia de #61 ya está integrada en la base y se conserva en esta rama junto con la búsqueda paginada de #71. Los proveedores de email/push siguen siendo mock. Consultar `issue33-integracion-y-validacion.md` para las pruebas de integración, la validación visual y la cobertura, que se reportan por separado.
