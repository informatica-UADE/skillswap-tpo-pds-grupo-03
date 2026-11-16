# Validación visual del PR #65

Ejecutada el 15/11/2026 con Java 17, Swing y repositorios en memoria, sobre la revisión posterior a `3840cc9`. Se usaron las identidades y créditos ficticios de la demo. Este registro no es una prueba visual sobre MySQL ni de carga; el recorrido JPA equivalente se comprueba automáticamente en CI.

## Recorrido observado

| Paso | Acción y resultado |
| --- | --- |
| Publicar | Lucía creó y publicó `Validacion final PR65`, mentoría de Diseño, 60 minutos, 150 SkillCoins, con franja disponible. Apareció en el catálogo. |
| Solicitar | Ana abrió el detalle. Un objetivo vacío mostró `Falta el objetivo`. Con objetivo `Validar el recorrido completo` y mensaje `Hola Lucia, comprobemos la solicitud.`, la contratación quedó PENDIENTE. |
| Leer datos | Se vieron título, objetivo, mensaje y Lucía como contraparte. Al cambiar a Lucía, la contraparte fue Ana y apareció la acción autorizada para aceptar. |
| Aceptar | Lucía aceptó; el estado pasó a ACEPTADA. |
| Validar saldo | Ana intentó pagar con saldo cero: se mostró `Saldo insuficiente`, sin avanzar de estado. |
| Acreditar | Ana acreditó 500 ficticios; billetera y encabezado mostraron 500. |
| Pagar | Ana pagó 150; contratación CONFIRMADA, saldo de Ana 350 y de Lucía 150. El comportamiento actual transfiere directamente al proveedor; no se validó una custodia inexistente. |
| Completar | Las acciones permitidas llevaron a EN_CURSO y COMPLETADA. Los datos de la contratación siguieron visibles. |
| Calificar | Ana asignó 5 estrellas y `Excelente recorrido de prueba`. Se confirmó el registro. Un segundo intento mostró `Ya calificaste esta contratación`. |
| Reputación | Al cambiar a Lucía, el saludo y el pie correspondieron a ella; el perfil mostró 5,0 y una reseña, con 150 en el encabezado. Las tarjetas del catálogo mostraron la reputación actualizada. |
| Notificaciones | Lucía recibió solicitud, pago, inicio, finalización y calificación. `Marcar todas como leídas` dejó el contador en cero. |

## Evidencia

- [Publicación](evidencia/pr65/01-publicacion.png).
- [Identidad y saldo acreditado](evidencia/pr65/02-identidad-saldo.png).
- [Solicitud con título, contraparte, objetivo y mensaje](evidencia/pr65/03-solicitud.png).
- [Contratación completada y saldo de Ana](evidencia/pr65/04-completada.png).
- [Rechazo de calificación duplicada](evidencia/pr65/05-duplicado.png).
- [Reputación y saldo de Lucía](evidencia/pr65/06-reputacion.png).
- [Notificaciones leídas](evidencia/pr65/07-notificaciones.png).

La prueba detectó que la etiqueta de estado tapaba el texto de las contrataciones. Se corrigió el layout y se agregó una regresión de dimensiones visibles. También se eliminó la dependencia de una caché de búsquedas para el título y se corrigió la contraparte según la identidad activa, con regresión desde ambos participantes.

No se editaron diagramas ni el PDF histórico que los contiene. Los límites funcionales externos al PR se documentan en [el informe actualizado](informe-diseno.md).
