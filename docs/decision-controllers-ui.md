# Decisión de diseño: un controller por pantalla — issue #32

## Problema

La UI (`SkillSwapView`) recibe un `UiControllers` y le pide el controller de cada pantalla. Hasta este cambio, ese objeto era `WiredUiControllers`, una sola clase de Infrastructure que:

- implementaba seis interfaces de controller y **delegaba método por método** búsqueda, publicaciones y perfil en los controllers reales;
- **tenía la lógica** de billetera, notificaciones y sesión demo, sin controller propio y **sin validar que el `userId` fuera el usuario activo**: `consultarSaldo("otro")` devolvía el saldo del usuario activo;
- armaba la contraparte de cada contratación llamando a `DiscoveryUseCases.author()` **una vez por fila** (N+1) y casteaba el proveedor de identidad a `DemoCurrentUserProvider`.

Cualquier pantalla nueva o cambio de contrato tocaba esa clase, que dependía de todos los casos de uso. Tenía baja cohesión y alto acoplamiento.

## Decisión

1. **Un controller por pantalla**, en `controllers`, con el mismo patrón que los existentes (#28, #30, #31, #33):
   - nuevos: `WalletUiController` y `NotificacionUiController`, que no existían;
   - se ordenó `CalificacionUiController` (ver más abajo);
   - todos reciben un puerto de entrada, la identidad activa y un `ControllerExecutor`.
2. **La sesión demo no es un controller.** Es solo el contexto de la demo: con qué usuarios se puede probar y cuál está activo. Lo resuelve `DemoCurrentUserProvider`, que ya era la identidad activa que leen los casos de uso (`CurrentUserProvider`), y además implementa `UiControllers.SesionDemo` para la vista:
   - `usuariosDisponibles()` sale de la base: los perfiles activos (`ProfileRepository.findActive()`), con su nombre y su tipo (profesional u organización). No hay una lista fija en el código; un perfil que se registre o se dé de baja aparece o desaparece;
   - `cambiarUsuario(id)` solo acepta un perfil activo;
   - en la vista, el recuadro del perfil activo (abajo a la izquierda) es el que abre el selector. Se quitó el botón "Cambiar usuario demo" de la navegación.

   Es el login simulado que pide la consigna, no un caso de uso, así que no se agrega un puerto en Application solo para la demo. La wiring expone el proveedor con su tipo concreto y ya no hace falta castear.
3. **`WiredUiControllers` se elimina.** Lo reemplaza `SkillSwapUiControllers`, un `record` que implementa `UiControllers`: `from(wiring)` arma cada controller una vez y la vista lo obtiene con su accesor. No delega métodos ni tiene lógica. La vista provista por la cátedra no cambia.
4. **La contraparte llega desde Application.**
   - `BookingView` trae `buyerDisplayName` y `providerDisplayName`;
   - `BookingApplicationService` los lee para todas las contrataciones del listado con **una sola** consulta de perfiles (`ProfileRepository.findByUserIds`), igual que la búsqueda con los autores;
   - `ContratacionUiController` elige cuál mostrar según el usuario activo;
   - `DiscoveryUseCases.author()` queda sin uso y se elimina.
5. **Iniciar lo hace el proveedor y completar el cliente.** Antes cualquiera de los dos participantes podía hacer las dos cosas. Completar cierra la contratación y habilita las calificaciones, así que lo confirma quien recibió el servicio; iniciar lo hace quien da la sesión. La regla está en `Booking.start` y `Booking.complete`, con el mismo `requireActor` que ya usaban aceptar (proveedor) y pagar (cliente); el otro participante recibe `FORBIDDEN`. `ContratacionUiController.puedeAvanzar` muestra el botón solo a quien corresponde.

## Controllers, casos de uso y responsabilidades

| Pantalla | Controller | Caso de uso (puerto) | El controller hace | Las reglas están en |
| --- | --- | --- | --- | --- |
| Explorar / Inicio | `BusquedaUiController` | `DiscoveryUseCases` | Convierte filtros y página; recomendaciones del usuario activo. | Repositorio (filtros y paginación), `RecommendationStrategy`. |
| Mis publicaciones | `PublicacionUiController` | `PublicationCommandUseCases`, `PublicationQueryUseCases` | Convierte borradores y estados; valida el usuario activo. | `Publication`, `PublicationBuilder`, estados (State). |
| Contrataciones | `ContratacionUiController` | `BookingUseCases` | Convierte comandos; arma la contraparte con el usuario activo. | `Booking`, estados, `Wallet`, `RefundPolicy`, `BookingApplicationService` (transacciones). |
| Calificar | `CalificacionUiController` | `RatingUseCases` | Valida usuario y contratación, delega y convierte. | `Rating` (puntaje 1 a 5, participantes, no autocalificarse), `RatingApplicationService` (una por autor y contratación). |
| SkillCoins | `WalletUiController` | `WalletUseCases` | Valida el usuario activo; convierte saldo y movimientos. | `Wallet` (saldo no negativo, importe positivo, movimientos en orden). |
| Notificaciones | `NotificacionUiController` | `NotificationUseCases` | Valida el usuario activo y el id; convierte. | `NotificationApplicationService`: solo el destinatario lee y marca las suyas. |
| Mi perfil / Perfil público | `PerfilUiController` | `ProfileUseCases`, `PublicProfileUseCases` | Convierte; el perfil público no exige usuario activo. | `Profile`, `PublicProfileFacade`. |
| Cambiar de perfil (recuadro del perfil activo) | Ninguno: `UiControllers.SesionDemo`, implementada por `DemoCurrentUserProvider` (Infrastructure) | — (lee `ProfileRepository.findActive()`) | Lista los perfiles activos con su tipo y cambia la identidad activa. | `Profile` (activo o dado de baja). |

### Qué se corrigió además en los controllers

- **`CalificacionUiController` repetía la regla del puntaje (1 a 5)**, que es de `Rating`. Ahora solo valida la entrada (usuario y contratación) y delega: un puntaje inválido llega como `VALIDATION` desde el dominio y se muestra igual. La conversión a `Models.Calificacion` pasó a `UiModelMapper.toCalificacion`, como el resto.
- **`ContratacionUiController` recibía una función de conversión desde afuera.** Ahora usa `UiModelMapper.toContratacion(view, usuarioActivo)` como los demás controllers.

## Rol de cada componente en este problema

| Componente | Clases | Rol |
| --- | --- | --- |
| Entidades de dominio | `Wallet`, `Rating`, `Booking` | Reglas de saldo y movimientos, de calificación y de estados. No cambian en este issue. |
| DTO de Application | `WalletView`, `MovementView`, `NotificationView`, `RatingView`, `BookingView` (con los nombres de las partes) | Datos simples que devuelven los casos de uso; nunca entidades. |
| DTO de Presentation | `Models.Movimiento`, `Models.Notificacion`, `Models.Calificacion`, `Models.Contratacion` | Lo que muestra la vista. |
| Service | `WalletApplicationService`, `NotificationApplicationService`, `RatingApplicationService`, `BookingApplicationService` | Orquestan; toman el usuario del puerto `CurrentUserProvider`, no del parámetro de la UI. |
| Repository | `WalletRepository`, `NotificationRepository`, `RatingRepository`, `BookingRepository`, `ProfileRepository` | Interfaces en Application; implementaciones JPA y en memoria en Infrastructure. Sin cambios, salvo que `BookingApplicationService` ahora usa `ProfileRepository.findByUserIds`. |
| Controller | Los de la tabla anterior | Reciben la acción, validan el usuario activo y la entrada, convierten y delegan. |
| Infrastructure | `SkillSwapUiControllers`, `DemoCurrentUserProvider`, `UiOperationExecutor`, `SkillSwapApplicationWiring` | Composición y detalles técnicos: arma los controllers, el contexto de la sesión demo y la traducción de errores de persistencia. |

**Dependencias.** La vista depende de las interfaces de `UiControllers` y de `Models`. Cada controller depende de un puerto de entrada, de `CurrentUserProvider` y de `ControllerExecutor`, nunca de Infrastructure; `PresentationContractsTest` lo verifica para todos. Infrastructure arma todo y es la única que conoce las clases concretas.

## Validaciones, errores y transacciones

- **Entrada (controller):**
  - el `userId` tiene que coincidir con el usuario activo. Sin identidad responde `NO_AUTENTICADO`, con otro usuario `PERMISOS` y con un id vacío `VALIDACION`, siempre antes de invocar el caso de uso;
  - los ids obligatorios (contratación, notificación) no pueden venir vacíos.
- **Negocio:**
  - el importe positivo y el saldo suficiente son de `Wallet`;
  - el puntaje, los participantes y la autocalificación son de `Rating`;
  - una calificación duplicada responde `CONFLICT` desde el service;
  - una notificación de otro destinatario responde `NOT_FOUND` y no se revela que exista.
- **Traducción:** `UiOperationExecutor` y `UiErrorMapper` convierten todo en `PresentationException(ErrorPresentacion)`. Si hubiera HTTP, la traducción sería: `VALIDATION` → 400, `UNAUTHENTICATED` → 401, `FORBIDDEN` → 403, `NOT_FOUND` → 404, `CONFLICT` e `INVALID_STATE` → 409, y persistencia o inesperado → 500.
- **Transacciones (Application):**
  - acreditar créditos demo, marcar leída y marcar todas son una transacción cada una;
  - la lectura de la billetera y el listado de contrataciones no abren transacción explícita. El listado hace una consulta de contrataciones, una de perfiles y una por publicación para el título (ver "Pendientes").

## Lo que no se agregó

- **Un puerto de Application para cambiar la identidad:** solo lo usaría la demo; la consigna excluye la autenticación real.
- **Una entidad de dominio `Notification`:** la bandeja no tiene comportamiento más allá de "es del destinatario" y "está leída". La pertenencia la garantiza el caso de uso con una consulta filtrada por destinatario. Si aparecieran reglas (vencimiento, prioridades), ahí correspondería una entidad.
- **Interfaces para los controllers nuevos:** ya existen (`WalletController`, `NotificacionController`); son el contrato con la vista.

## Estrategia de pruebas

| Prueba | Tipo | Cubre |
| --- | --- | --- |
| `WalletUiControllerTest` | Unitaria de Presentation con doble del puerto | Saldo y movimientos convertidos; crédito demo; otro usuario, identidad ausente o id vacío sin invocar el caso de uso; billetera inexistente e importe inválido traducidos. |
| `NotificacionUiControllerTest` | Unitaria de Presentation con doble del puerto | Listado convertido; marcar una (con el id recortado) y todas; validaciones de identidad e id sin invocar el caso de uso; notificación de otro destinatario como `NO_ENCONTRADO`. |
| `CalificacionUiControllerTest` | Unitaria de Presentation con doble del puerto | Comando convertido y una sola delegación; identidad y contratación sin invocar el caso de uso; duplicada como `CONFLICTO`, no autorizada como `PERMISOS`, puntaje inválido como `VALIDACION` (lo decide el dominio). |
| `ContratacionUiControllerTest` | Unitaria de Presentation | La contraparte es la otra parte según el usuario activo; delegación de cada operación; en cada estado, el botón de avanzar lo ve solo quien puede hacer la transición. |
| `DomainRulesTest` | Unitaria de dominio | El cliente no puede iniciar ni el proveedor completar. |
| `DemoCurrentUserProviderTest` | Unitaria de Infrastructure | Los usuarios disponibles son los perfiles activos, ordenados por nombre y con su tipo; cambiar de usuario cambia la identidad que leen los casos de uso; un perfil inexistente o dado de baja se rechaza y no cambia nada. |
| `BookingApplicationServiceTest` | Unitaria de Application con dobles | Nombres de ambas partes con una sola lectura de perfiles; una parte sin perfil se muestra con su id. |
| `BookingApplicationMapperTest`, `UiModelMapperTest` | Unitarias de conversión | Los nombres viajan en `BookingView`; la contraparte se elige según el usuario activo. |
| `PresentationContractsTest` | Contrato de Presentation | Ningún controller depende de Infrastructure ni de JPA; los DTO solo tienen datos simples. |
| `SkillSwapUiControllersTest` | Integración de Presentation con Application y memoria | Cada pantalla recibe su controller; billetera y bandeja siguen a la sesión demo y rechazan a otro usuario; los flujos que ya cubría el test anterior. |
| `MySqlPersistenceIntegrationTest` | Integración con MySQL | Los perfiles del seed son los usuarios de la demo; el recorrido completo a través de los controllers sobre JPA. |
| `SkillSwapViewTest` | Pantalla Swing | La vista funciona con la composición nueva; el recuadro del perfil activo es el que cambia de perfil. |

## Supuestos

- La vista sigue pasando el `userId` en cada llamada y lo mantiene sincronizado con la sesión demo; los controllers lo validan contra la identidad activa.
- Un participante sin perfil no debería existir por las claves foráneas; si aparece, se muestra con su identificador en lugar de fallar el listado.
- `SesionDemo` solo existe en la composición de la demo; el Mock UI Kit no la tiene y el recuadro del perfil queda como informativo.
- Solo el cliente completa. Si nunca lo hace, la contratación queda en curso y nadie puede calificar. Hoy no hay plata retenida (el proveedor cobra al pagar), así que se acepta. Con la custodia de #19 convendría que el proveedor marque la sesión como finalizada y que se complete sola si el cliente no confirma en un plazo.
- Para recorrer publicar → solicitar → aceptar → pagar → completar → calificar hacen falta dos perfiles: el proveedor publica, acepta e inicia; el cliente solicita, paga y completa; los dos se califican. El seed actual tiene tres profesionales y ninguna organización.

## Pendientes detectados (fuera de este issue)

- **`ContratacionUiController.puedeAvanzar()` y `avanzar()`** repiten qué parte puede hacer cada transición según el estado. La regla ya la impone `Booking`; el controller la duplica solo para mostrar u ocultar el botón "Avanzar estado" del kit. Lo correcto sería que Application informe las acciones permitidas para el usuario.
- **Título de cada contratación:** `BookingApplicationService` sigue leyendo la publicación una vez por contratación. Resolverlo requiere una consulta por lote en `PublicationRepository`.

## Contratos de los controllers nuevos

| Operación | Entrada | Salida | Errores | Efectos |
| --- | --- | --- | --- | --- |
| `WalletController.consultarSaldo(userId)` | Usuario activo | Saldo en SkillCoins | `NO_AUTENTICADO`, `PERMISOS`, `VALIDACION`, `NO_ENCONTRADO` | Ninguno |
| `WalletController.listarMovimientos(userId)` | Usuario activo | `List<Movimiento>` en orden | Ídem | Ninguno |
| `WalletController.acreditarDemo(userId, importe)` | Usuario activo, importe positivo | Saldo nuevo | Ídem, más `VALIDACION` por importe | Acredita en una transacción |
| `NotificacionController.listarNotificaciones(userId)` | Usuario activo | `List<Notificacion>` | `NO_AUTENTICADO`, `PERMISOS`, `VALIDACION` | Ninguno |
| `NotificacionController.marcarLeida(userId, id)` | Usuario activo, id | — | Ídem, más `NO_ENCONTRADO` si no es suya | Marca en una transacción |
| `NotificacionController.marcarTodasLeidas(userId)` | Usuario activo | — | `NO_AUTENTICADO`, `PERMISOS`, `VALIDACION` | Marca en una transacción |
| `CalificacionController.calificar(userId, contratacionId, puntaje, comentario)` | Usuario activo, contratación | `Calificacion` | Ídem, más `CONFLICTO` (duplicada), `PERMISOS` (no participante), `ESTADO` (no completada), `VALIDACION` (puntaje) | Guarda la calificación y recalcula la reputación en una transacción |
| `SesionDemo.usuariosDisponibles()` | — | `List<UsuarioDemo>` (id, nombre, tipo) de los perfiles activos | — | Ninguno |
| `SesionDemo.cambiarUsuario(userId)` | Perfil activo | — | `VALIDACION` | Cambia la identidad activa |
