# Auditoría de los diagramas UML de SkillSwap

## Propósito

Este documento explica y audita los dos diagramas PlantUML del proyecto:

- [Arquitectura de aplicación](./uml-aplicacion.puml): vista **macro** de las capas y sus colaboraciones.
- [Modelo de dominio](./uml-dominio.puml): vista **micro** de los agregados, reglas, estados y patrones del negocio.

Se deben leer en conjunto, pero no son duplicados: el primero explica **dónde vive cada responsabilidad y en qué dirección se relacionan las capas**; el segundo muestra **qué conceptos y reglas forman el dominio**. La auditoría compara su intención con el código del repositorio. Los diagramas son documentación de diseño, no sustituyen las pruebas ni una especificación exhaustiva de todas las clases.

## 1. UML de arquitectura: vista macro

### 1.1 Capas y responsabilidades

| Capa | Elementos representados | Responsabilidad |
|---|---|---|
| Presentation | `SkillSwapView`, `UiControllers`, modelos y mappers de UI | Presentar información y consumir contratos de controllers; no acceder directamente a entidades JPA o repositorios. |
| Application | Puertos de entrada, servicios, puertos de salida y modelos de aplicación | Ejecutar casos de uso, obtener el usuario actual, coordinar dominio y persistencia, gestionar transacciones y publicar eventos. |
| Domain | Agregados principales, `RefundPolicy`, `RecommendationStrategy`, estados y estrategias de precio | Mantener invariantes y reglas de negocio sin depender de UI, almacenamiento o proveedores externos. |
| Infrastructure | Wiring, adapters JPA/en memoria, identidad, transacciones, eventos y notificaciones | Implementar puertos, adaptar sistemas externos y ensamblar las implementaciones para los modos de ejecución. |

### 1.2 Dirección de dependencias

La intención arquitectónica que documenta el diagrama es:

1. Presentation consume contratos para invocar operaciones de Application.
2. Los servicios de Application implementan los puertos de entrada y dependen de puertos de salida, no de adapters concretos.
3. Application usa tipos y comportamiento del Domain.
4. Infrastructure implementa los puertos de salida de Application.
5. El composition root selecciona e instancia servicios y adapters.

Por eso, las relaciones de realización (`..|>`) entre servicios y puertos de entrada, y entre adapters y puertos de salida, son centrales para mostrar inversión de dependencias (DIP). El diagrama no debe interpretarse como que Domain depende de Infrastructure: las implementaciones concretas quedan hacia afuera.

### 1.3 Servicios y puertos

Se detallan siete servicios por área cohesiva: perfil, publicaciones, descubrimiento, contrataciones, billetera, calificaciones y notificaciones. Cada uno implementa su port de entrada. Las dependencias relevantes incluyen:

- `ProfileApplicationService`: perfil, usuario actual y transacciones.
- `PublicationApplicationService`: repositorios de publicaciones y perfiles, usuario actual y transacciones.
- `DiscoveryApplicationService`: repositorios de publicaciones y perfiles, estrategia de recomendación.
- `BookingApplicationService`: repositorios de contratación, publicaciones y billeteras; usuario actual, transacciones, política de reintegro y port de eventos.
- `RatingApplicationService`: repositorios de contratación, calificaciones y perfiles; usuario actual, transacciones y port de eventos.
- `WalletApplicationService`: repositorio de billetera, usuario actual y transacciones.
- `NotificationApplicationService`: repositorio de notificaciones, usuario actual y transacciones.

El nivel de detalle es deliberadamente selectivo: las reglas de cada agregado y los métodos de sus estrategias se encuentran en el UML de dominio y en el código.

### 1.4 Adapters, composición y eventos

- Los puertos de repositorio tienen implementaciones en memoria y JPA. Los ports de transacción también tienen variantes en memoria y JPA.
- `IdentityProviderAdapter` implementa `CurrentUserProvider`.
- `SkillSwapApplicationWiring` compone los servicios y configura el dispatcher de eventos. La aplicación puede usar implementaciones en memoria o JPA.
- `BookingApplicationService` y `RatingApplicationService` publican eventos a través de `DomainEventPublisher`; el port no conoce los observadores ni los canales.
- El publicador en memoria despacha eventos a observadores registrados. El observador de contrataciones y el publicador/observador de calificaciones consultan preferencias del perfil y usan `NotificationFactory`.
- `NotificationFactory` selecciona `EmailNotificationAdapter`, `PushNotificationAdapter` o `InAppNotificationAdapter`, que implementan `NotificationGateway`. Email y push usan clientes mock; la bandeja interna usa el repositorio de notificaciones.
- Los eventos de negocio se envían después de confirmar la transacción, evitando notificar como exitosas operaciones revertidas.

`NotificationUseCases` representa la bandeja del usuario (listar y marcar notificaciones leídas), no el envío por email/push. El diagrama mantiene ambos flujos separados.

### 1.5 Alcance y simplificaciones

- La clase `WiredUiControllers` está en el paquete Java `infrastructure` porque es el adapter que conecta contratos de UI con el wiring y los servicios. La vista está en `ui`; el contrato está en `controllers`.
- El diagrama modela conceptualmente la entrada mediante puertos, pero la implementación UI actual usa `SkillSwapApplicationWiring` para obtener servicios concretos; por lo tanto, no es un call graph literal de cada operación.
- No todas las capacidades expuestas por los ports tienen hoy una acción de UI. El diagrama incluye `RatingUseCases`, pero eso no implica que la UI permita enviar una calificación por un controller dedicado.
- El bloque de modelos agrupa comandos/consultas y resultados para mantener legible la vista macro; no enumera cada record y mapper.
- La búsqueda del catálogo y `searchPublished` aparecen como contexto del repositorio (#16); esa nota no atribuye dicha entrega a otra persona ni modifica el alcance del UML de dominio.

## 2. UML de dominio: vista micro

### 2.1 Agregados y responsabilidades

- **`Profile`** concentra identidad, tipo de actor (`PROFESSIONAL` u `ORGANIZATION`), habilidades, intereses, canales, disponibilidad, estado activo y reputación. Protege las modificaciones del titular y calcula la reputación a partir de calificaciones verificadas.
- **`Publication`** mantiene el tipo (`MENTORSHIP`, `SERVICE` o `CHALLENGE`), datos de la oferta, precio base, etiquetas, disponibilidad, actividad de solicitudes, versión, estado e historial. El dominio valida que el tipo de publicación sea permitido para el tipo de actor que la crea; las demás reglas de ciclo de vida son comunes.
- **`Booking`** representa una contratación por identificadores de participantes/publicación, objetivo, mensaje, franja, pagos, reintegro, reprogramación, estado e historial. Protege participantes y transiciones del ciclo de vida.
- **`Wallet`** conserva saldo y ledger consistentes. `SkillCoins` representa importes no negativos y operaciones aritméticas verificadas.
- **`Rating`** tiene identificador y ciclo de persistencia propios. Valida que la contratación esté completada, identifica a la contraparte y valida el puntaje. La unicidad por autor y contratación se coordina en Application/persistencia.
- **`AvailabilitySlot`**, `LedgerEntry` y `StatusHistoryEntry` representan disponibilidad, movimientos e historial. El diagrama marca explícitamente como value objects aquellos registros inmutables modelados de esa forma; no se debe inferir que el estereotipo UML por sí solo implemente inmutabilidad.

Las referencias `publicationId` y `bookingId` se muestran como dependencias por identificador, no como composición directa de objetos entre agregados. Los historiales y colecciones contenidos por agregados se representan con composición cuando su ciclo de vida pertenece al agregado.

### 2.2 State

`Publication` delega decisiones del ciclo de vida en `PublicationState`; `Booking` hace lo propio con `BookingState`. Los estados del diagrama corresponden al código:

- Publicación: `DRAFT`, `PUBLISHED`, `PAUSED`, `CLOSED`, `CANCELLED`.
- Contratación: `PENDING`, `ACCEPTED`, `CONFIRMED`, `IN_PROGRESS`, `COMPLETED`, `REJECTED`, `CANCELLED`.

Los objetos concretos son enums singleton internos de `PublicationStates` y `BookingStates`; no son clases públicas independientes. El pago transiciona a `CONFIRMED`, no a un estado separado `PAID`. Los agregados registran el historial de transición. Application publica eventos después del commit: los objetos State no envían notificaciones ni conocen infraestructura.

### 2.3 Strategy, Builder y Decorator

- **Strategy:** `RecommendationStrategy` tiene `SkillAffinityStrategy` y `RecentPopularityStrategy`; `RefundPolicy` tiene `StandardRefundPolicy` y `FlexibleRefundPolicy`. Los servicios reciben las abstracciones y no contienen esos algoritmos. La composición actual elige una variante por wiring.
- **Builder:** `PublicationBuilder` valida la presencia de datos requeridos y crea un borrador, delegando al agregado la protección de sus invariantes.
- **Decorator:** `PriceQuote` permite envolver `BasePrice` con promociones fijas o porcentuales. `PriceDecorator` contiene la cotización envuelta y evita resultados negativos.
- **State:** las transiciones válidas varían según el estado actual y son delegadas a implementaciones polimórficas.

Estos cuatro patrones tienen variaciones de negocio reconocibles y evidencia de implementación en Domain. La arquitectura también contiene Observer, adapters y una Simple Factory para notificaciones; la fábrica simple es una decisión de construcción de adapters, no debe confundirse con un quinto patrón GoF formal si la rúbrica distingue esa clasificación.

### 2.4 Límites de consistencia importantes

- El dominio valida que el solicitante no sea el autor y que exista cupo; la aceptación de una contratación coordina reserva y persistencia en Application.
- La reserva concurrente del último cupo se apoya en transacción/locks de Infrastructure. No es una garantía exclusiva de `Booking` ni de `BookingState`.
- `Wallet` protege una billetera individual. El débito al comprador, crédito al proveedor y actualización de contratación deben ejecutarse dentro de la misma transacción de aplicación.
- `Rating` no modifica directamente `Profile`. `RatingApplicationService` persiste el rating y solicita al perfil recalcular la reputación.

## 3. Evaluación frente al TP

En conjunto, las dos vistas documentan los aspectos principales solicitados: separación de capas, puertos, inversión de dependencias, modelo de negocio, relaciones, estados y patrones con responsabilidades distinguibles. El UML de arquitectura funciona como mapa de alto nivel; el UML de dominio añade el detalle de clases que no conviene duplicar en el primero.

La documentación no implica por sí sola que toda la aplicación esté conectada visualmente, ni acredita cobertura de pruebas, concurrencia de producción o integración con proveedores reales. Esos puntos se validan en código, pruebas unitarias/de integración y ejecución. Para una defensa, conviene recorrer un caso completo —solicitar, aceptar, pagar, completar y calificar— señalando en cada paso qué parte decide, qué parte coordina y qué parte persiste.

## 4. Referencias de implementación

- [Servicios y puertos de Application](../src/main/java/ar/edu/uade/skillswap/application)
- [Dominio](../src/main/java/ar/edu/uade/skillswap/domain)
- [Adapters y composition root](../src/main/java/ar/edu/uade/skillswap/infrastructure)
- [Contratos de controllers](../src/main/java/ar/edu/uade/skillswap/controllers/UiControllers.java)
- [UML de arquitectura](./uml-aplicacion.puml)
- [UML de dominio](./uml-dominio.puml)
- [Informe de decisiones de diseño](./informe-diseno.md)
- [Auditoría de patrones, GRASP y SOLID](./auditoria-patrones-y-grasp.md)
