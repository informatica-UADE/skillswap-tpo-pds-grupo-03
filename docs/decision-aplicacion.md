# Capa de aplicación

La capa de aplicación coordina los casos de uso. Recibe comandos y consultas, obtiene al usuario autenticado por un puerto, carga agregados mediante repositorios, delega invariantes en el dominio y guarda los cambios. Los contratos de entrada y los resultados usan modelos de aplicación; no exponen entidades de dominio, persistencia ni modelos de Swing.

## Responsabilidades de los servicios

| Servicio | Responsabilidad | Casos relacionados |
| --- | --- | --- |
| `ProfileService` | Coordinar alta, consulta, actualización y baja lógica del perfil. | #14 |
| `PublicationService` | Coordinar borradores, edición, publicación y consulta propia; validar que la categoría exista en el catálogo y guardarla con su nombre registrado; las vistas incluyen el perfil público del autor. | #15, #37 |
| `CatalogApplicationService` | Consultar el catálogo jerárquico de categorías y habilidades en una lectura y devolverlo como vistas. El árbol y sus reglas son del dominio (Composite). Ver [decisión del catálogo](decision-catalogo-composite.md). | #37 |
| `DiscoveryService` | Rechazar una categoría que no está en el catálogo, delegar la búsqueda paginada en `PublicationRepository.searchPublished` (una categoría incluye su rama), armar las vistas con los datos públicos del autor (una lectura de perfiles por página) y pedir recomendaciones a la estrategia de dominio configurada. Ver [decisión de búsqueda](decision-busqueda.md). | #16–17 |
| `BookingService` | Orquestar solicitud, aceptación, pago, cancelación, reintegro, reprogramación y cierre dentro de transacciones. | #18–20 |
| `WalletService` | Consultar saldo y movimientos; los débitos y créditos se coordinan desde el caso de uso que los origina. | #19 |
| `RatingService` | Evitar duplicados y delegar al dominio la verificación de contratación y participantes; luego coordinar reputación y evento. | #21 |
| `NotificationService` | Consultar y marcar la bandeja del usuario; enviar avisos a través de un puerto. | #21, #25 |
| `PublicProfileFacade` | Consultar el perfil público de cualquier usuario activo: coordina en una lectura los datos públicos y la reputación del perfil, una página de publicaciones activas, los conteos de contrataciones completadas y las últimas calificaciones. Es una Facade: solo compone, sin reglas propias. Ver [decisión del perfil público](decision-perfil-publico.md). | #40 |

Los servicios concretos se implementan en sus issues específicos para evitar que una única clase concentre toda la lógica y para permitir trabajo paralelo sin duplicar responsabilidades. Las interfaces de entrada de este cambio fijan el contrato común; los puertos de salida aíslan repositorios, identidad, transacciones, eventos y entrega de notificaciones.

## Límites y dependencias

- Presentation depende de puertos de entrada, no de implementaciones de servicios.
- Application depende de Domain y de abstracciones propias; no importa UI, JPA, Hibernate ni SDK externos.
- Infrastructure implementa los puertos de salida y no define reglas de negocio.
- Las operaciones sensibles usan `TransactionManager`; los repositorios ofrecen lecturas bloqueables para las escrituras concurrentes.
- Los eventos y mensajes de notificación son modelos simples de aplicación, no dependen de un broker ni de un proveedor concreto.

El diagrama editable está en [`uml-aplicacion.puml`](uml-aplicacion.puml).
