# SkillSwap - informe de diseño e integración

Trabajo Práctico Final - Proceso de Desarrollo de Software. Grupo 03.

Revisión del 15/11/2026 para el PR #65, sobre main en e64048c (#63/#64 y #75 integrados). Este informe documenta el código y la validación de la rama; no certifica funcionalidades de otros issues abiertos. El modelo editable y el PDF histórico con diagramas permanecen intactos; los fuentes PlantUML se actualizan para mostrar Facade y controllers. La versión actual del informe se entrega en output/pdf/informe-diseno-actualizado.pdf.

## 1. Alcance ejecutable

La aplicación es una demo Java 17/Swing. Permite publicar, buscar y solicitar propuestas, aceptar, pagar con SkillCoins ficticios, iniciar, completar y calificar. El selector permite alternar entre Mateo, Ana y Lucía. Las identidades, email y push son simulados: no hay autenticación, envío externo ni pagos reales.

La aplicación siempre persiste en MySQL con repositorios JPA: no hay un modo en memoria. Flyway aplica migraciones V1-V6 y Hibernate valida el esquema. Las pruebas usan los mismos repositorios JPA sobre H2 y, con el perfil mysql-it, sobre MySQL real. #75 distingue profesionales y organizaciones y exige tipos explícitos. La demo no agrega alta visual de organizaciones ni un desafío inicial, pendientes de sus tareas correspondientes.

## 2. Capas y dependencias

| Capa | Responsabilidad y evidencia |
| --- | --- |
| Presentation | SkillSwapView consume el contrato UiControllers; BusquedaUiController, PublicacionUiController, ContratacionUiController, PerfilUiController, WalletUiController, CalificacionUiController y NotificacionUiController implementan contratos cohesivos por pantalla. Convierten DTOs y delegan en puertos de entrada. PresentationException y UiActionHandler muestran errores estables. |
| Application | Servicios por capacidad, puertos y comandos/resultados. Identifican al actor, coordinan agregados, transacciones y eventos. No importan Swing ni JPA. |
| Domain | Profile, Publication, Booking, Wallet, Rating, estados, políticas, Builder y cotizaciones. Conserva invariantes y depende de Java estándar. |
| Infrastructure | Repositorios JPA, mappers, migraciones, transacciones y adapters. Implementa los puertos internos. |
| Composición | SkillSwapApplicationWiring selecciona implementaciones. SkillSwapUiControllers (#32) arma un controller por pantalla (búsqueda, publicaciones, contrataciones, calificaciones, billetera, notificaciones y perfil) y la vista habla con cada uno; no delega métodos ni tiene lógica. La sesión demo no es un controller: DemoCurrentUserProvider es la identidad activa y lista los perfiles activos de la base para cambiar de usuario. |

Las entidades JPA no atraviesan los puertos. Los resultados de Application y DTOs de UI contienen datos, enums y colecciones copiadas. `SkillSwapView` depende de `UiControllers`; `MockSkillSwapControllers` implementa esos contratos para el Mock UI Kit, mientras `SkillSwapUiControllers` compone los controllers reales. No existe un `RecomendacionController` independiente: `BusquedaUiController.recomendar(userId)` delega en `DiscoveryUseCases.recommend(limit)`.

Los controllers verifican que el `userId` coincida con la identidad activa y validan datos de entrada requeridos. La autorización sobre recursos y participantes permanece en Application/Domain. `UiModelMapper` convierte los view models de `Models` a comandos/consultas y traduce resultados de Application a modelos de UI. Se documentan los contratos existentes: por ejemplo, UI recibe `Models.Contratacion` (no `BookingView`) y la billetera devuelve el saldo como `int` y los movimientos como `List<Models.Movimiento>`; no se agregan DTOs ficticios como `WalletSummaryView`. La interfaz `ControllerExecutor`, implementada por `UiOperationExecutor`, centraliza la traducción de errores de aplicación/persistencia a `PresentationException` con `ErrorPresentacion`; `UiActionHandler` los presenta sin exponer detalles técnicos. Ningún controller consulta repositorios ni usa SQL/JPA. `PresentationContractsTest` comprueba que los controllers dedicados no expongan entidades ni dependan de persistencia.

## 3. Responsabilidades del negocio

| Componente | Regla o coordinación |
| --- | --- |
| Profile | Titularidad, actividad, habilidades/intereses y reputación derivada. Tipo de actor fijo al registrar. |
| Publication | Autoría, borrador, estados y disponibilidad. ActorType valida el tipo elegido. |
| Booking | Participantes, transiciones, mensaje, objetivo y franja independientes; reprogramación e historial. Cada transición tiene su actor: el proveedor acepta, rechaza e inicia; el cliente paga y completa (#32). |
| Wallet / SkillCoins | Montos, saldo y ledger. Application coordina transferencias entre billeteras. |
| Rating | Contratación completada, participantes y puntaje. Application y una restricción persistente protegen unicidad. |
| Servicios | Cargan mediante puertos, delegan reglas, guardan resultados y publican eventos después del commit. |

El controller valida el usuario contra la identidad activa. Application/Domain conservan la autorización sobre recursos. El selector demo no sustituye estas reglas ni sirve como autenticación de producción.

## 4. Patrones implementados

| Patrón | Implementación y justificación |
| --- | --- |
| Adapter | IdentityProviderAdapter convierte la respuesta externa a currentUserId(). Email/Push traducen NotificationMessage a clientes mock; InApp adapta el envío al guardado. Aísla contratos mediante composición. |
| Simple Factory | NotificationFactory.create(channel) selecciona Email, Push o InApp y devuelve NotificationGateway. Centraliza creación. Es una técnica creacional, fuera de los 23 patrones GoF. |
| Observer | InMemoryDomainEventPublisher registra suscriptores. Eventos de contratación/calificación activan notificaciones. Es síncrono y en memoria, sin entrega durable. |
| Strategy | RecommendationStrategy: afinidad/popularidad. RefundPolicy: estándar/flexible. El wiring elige afinidad y estándar; las alternativas están probadas, no seleccionadas por una pantalla. |
| State | BookingStates y PublicationStates determinan transiciones válidas; el agregado cambia estado y conserva historial. |
| Builder | PublicationBuilder reúne datos obligatorios/opcionales y construye un borrador válido con tipos explícitos. |
| Decorator | Promociones fijas/porcentuales acumulables en domain/pricing, probadas a nivel de cotización. La contratación toma el precio base: no se afirma integración de promociones desde la UI. |
| Facade | PublicProfileFacade implementa PublicProfileUseCases: una sola consulta arma el perfil público (§4.1) coordinando ProfileRepository, PublicationRepository, BookingRepository y RatingRepository en una transacción de lectura. Devuelve PublicProfileView, un Read Model inmutable sin entidades JPA ni datos privados. Solo compone; las reglas quedan en las entidades y las consultas. Decisión en decision-perfil-publico.md (#40). |
| Composite | CatalogItem (componente), CatalogCategory (compuesto) y CatalogSkill (hoja) forman el catálogo jerárquico de categorías y habilidades. El mismo recorrido funciona para una hoja y para una rama; la categoría protege el árbol (un padre, sin ciclos, nombres únicos). Buscar por una categoría incluye toda su rama, resuelto en la consulta con la ruta materializada. Decisión en decision-catalogo-composite.md (#37). |
| Singleton localizado | Los enums reutilizan estados inmutables. No hay un Singleton mutable de aplicación o sesión. |
| Repository / DAO | Puertos y adaptadores aíslan almacenamiento. Patrón de acceso a datos, distinto del catálogo GoF. |
| GRASP Controller / MVC | Los siete controllers de Presentation reciben eventos por interfaces de `UiControllers`, propagan identidad, convierten DTOs y delegan cada capacidad en sus puertos de entrada. La fachada no justifica concentrar áreas distintas en un God Controller. |

Agregar un canal modifica el enum y switch de Simple Factory, su adapter y configuración; los casos de uso conservan el contrato. No se afirma OCP sin modificaciones en toda la solución.

Factory Method se descartó por ausencia de una variación de creación mediante subclases; ver decision-factory-method-vs-simple-factory.md. El nombre create o la ausencia de static no cambian esa clasificación. Abstract Factory no tiene una familia de productos relacionados que construir. La decisión formal de CQRS (#42) permanece fuera de esta entrega.

En concreto, sin esta Facade `PerfilUiController` tendría que depender de cuatro puertos, conocer el orden de las lecturas y combinar datos de perfiles, publicaciones, contrataciones y calificaciones. Eso aumentaría el acoplamiento de Presentation y mezclaría la traducción de acciones de UI con la composición del perfil. Con `PublicProfileUseCases` como único puerto de entrada, el controller convierte la solicitud y delega; `PublicProfileFacade` concentra solo la coordinación de esta consulta.

La solución respeta Clean Architecture y DIP: Presentation depende de `PublicProfileUseCases`, no de `PublicProfileFacade`; la Facade, en Application, depende de interfaces de salida definidas por esa capa, implementadas en Infrastructure. El mapper convierte los agregados en records inmutables de Application y permite publicar solo los campos autorizados. SRP se mantiene porque la Facade no decide reglas de visibilidad, no calcula reputación ni persiste: esas responsabilidades permanecen en el perfil, las consultas de repositorio y los adapters correspondientes. Los diagramas [`uml-aplicacion.puml`](uml-aplicacion.puml) y [`uml-dominio.puml`](uml-dominio.puml) muestran la colaboración y dejan explícito que la Facade no pertenece a Domain.

## 5. GRASP y SOLID

Information Expert ubica reglas en quien conoce los datos. Creator aparece en métodos de creación y Builder. Controller recibe acciones. Polymorphism se aplica a State, Strategy y cotizaciones. Low Coupling, Indirection y Protected Variations se apoyan en puertos/adapters. Pure Fabrication ubica mappers/transacciones fuera del dominio. High Cohesion exige separar capacidades.

SRP separa crear, adaptar, coordinar y persistir. Para GRASP Controller, cada controller recibe eventos de un área y delega: búsqueda/recomendaciones, publicaciones, contrataciones, perfil, calificaciones, billetera o notificaciones. Esta alta cohesión evita que un controller monolítico concentre cambios y reglas ajenas; el bajo acoplamiento proviene de `UiControllers` y de los puertos de entrada, no de dependencias con servicios concretos, dominio o persistencia. El controller solo compara identidad activa y valida forma básica de entrada; reglas de autorización y negocio siguen en Application/Domain. DIP se mantiene porque Presentation consume interfaces y Application define los puertos de salida que implementa Infrastructure. #32 separó billetera y notificaciones en controllers propios, dejó la sesión demo como contexto de identidad y eliminó la clase que concentraba todas las pantallas; no se declara una aplicación perfecta de todos los principios.

## 6. Persistencia y consistencia

Flyway versiona esquema y restricciones: V2 conserva el mensaje opcional, V3 ordena el ledger, V4 adapta comparaciones de búsqueda, V5 agrega tipos y V6 crea el catálogo jerárquico con la clave foránea de la categoría de cada publicación. Hibernate usa validate.

JpaTransactionManager comparte EntityManager, confirma o revierte y ejecuta callbacks después del commit. Los repositorios bloquean recursos para cupos, pagos y calificaciones; las billeteras se adquieren en orden estable. MySQL usa READ_COMMITTED. La búsqueda pagina/filtra en persistencia y completa autores por lotes.

Los repositorios en memoria y SynchronousTransactionManager se eliminaron: no aportaban rollback ni garantías de concurrencia y duplicaban cada consulta con una segunda implementación que había que mantener igual a la de JPA. JPA conserva datos y el seed evita duplicados.

El pago de main transfiere directamente al proveedor. No existe custodia ni liberación al completar. Esta diferencia respecto del criterio de #19 es una brecha de negocio preexistente, fuera de la integración #65, que se debe resolver por separado. El guion ya no describe una custodia inexistente. Preparar este PR no certifica terminado todo el proyecto.

## 7. Correcciones encontradas al probar Swing

BorderLayout colocaba texto y estado de una contratación en la misma región; la etiqueta tapaba título, contraparte, objetivo y mensaje. Ahora el estado integra el panel de texto y los datos permanecen visibles.

BookingView incorpora el título que Application obtiene desde la publicación. Presentation deja de depender de una caché de búsquedas; cambiar de usuario o crear otra fachada conserva el título. La contraparte se calcula con la identidad activa, sin fijar Mateo.

SkillSwapViewTest reproduce el problema de layout y verifica dimensiones visibles. SkillSwapUiControllersTest comprueba título sin caché y contraparte desde ambos participantes. Se mantienen permisos, saldo, unicidad, tipos y paginación. Los diagramas PlantUML ahora documentan los controllers y sus contratos sin conectarlos directamente con Domain o persistencia.

## 8. Cobertura reproducible

JaCoCo 0.8.16 se activa con el perfil coverage. Para el objetivo de #33 se exige al menos 80% de líneas en Domain y 80% en Application por separado. No se excluyen clases de esas capas para mejorar resultados. Las ramas se informan como métrica adicional.

| Capa | Líneas cubiertas / total | Líneas | Ramas |
| --- | --- | --- | --- |
| Domain | 658 / 720 | 91,39% | 248 / 374 = 66,31% |
| Application | 534 / 627 | 85,17% | 120 / 194 = 61,86% |

Son resultados locales sin MySQL, suficientes para superar el umbral. mysql-it agrega las pruebas reales; consultar los checks de cada SHA. La cobertura no demuestra ausencia de errores ni ejercicio de todas las ramas.

Comandos reproducibles:

- mvn -Pcoverage clean verify
- powershell -File scripts/coverage-summary.ps1
- mvn -Pmysql-it,coverage clean verify (con las variables de pruebas documentadas)

HTML/XML: target/site/jacoco. Resumen: target/coverage-summary.json. Evidencia de esta revisión: docs/evidencia/pr65/cobertura-local.json. Maven falla si alguna de las dos capas baja del 80% de líneas. GitHub ejecuta pruebas y cobertura juntos.

## 9. Validación y entrega

La suite local ejecuta 224 pruebas correctamente y omite explícitamente 39 de MySQL sin el perfil: 263 detectadas, cero fallos y errores. La suite real cubre migraciones, round-trip, rollback, carreras de último cupo/doble pago/calificación y reinicio, más el recorrido mediante controllers. Los checks del PR registran el resultado exacto de la cabeza final.

El recorrido interactivo se registra en validacion-visual-pr65.md con capturas de publicación, solicitud, saldo, finalización y calificación. Usa datos ficticios y memoria; no se presenta como recorrido visual sobre MySQL ni prueba de carga. El flujo JPA equivalente se verifica automáticamente.

Este Markdown es fuente del PDF actualizado, generado con scripts/generar-informe.py. El .mdj y el PDF histórico con diagramas permanecen intactos; los diagramas PlantUML fuente incluyen ahora la colaboración de la Facade y la capa de Presentation. La revisión humana de la cabeza final corresponde a la persona revisora; no se fabrica ni se sustituye.

## 10. Referencias y trazabilidad

- #65 / #33: integración, sesión demo, calificación, pruebas y entrega.
- #63/#64: controllers dedicados integrados en la base.
- #75: profesionales/organizaciones y tipos explícitos.
- #25 / #41: Adapter, Simple Factory y decisión de Factory Method.
- #19, #32, #37, #40, #42: diferencias o tareas del equipo que este PR no cierra.
- JaCoCo: https://www.jacoco.org/jacoco/trunk/doc/maven.html
- Umbrales: https://www.jacoco.org/jacoco/trunk/doc/check-mojo.html
- Repositorio: https://github.com/informatica-UADE/skillswap-tpo-pds-grupo-03
