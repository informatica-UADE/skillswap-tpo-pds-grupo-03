# SkillSwap - informe de diseño e integración

Trabajo Práctico Final - Proceso de Desarrollo de Software. Grupo 03.

Revisión del 15/11/2026 para el PR #65, sobre main en e64048c (#63/#64 y #75 integrados). Este informe documenta el código y la validación de la rama; no certifica funcionalidades de otros issues abiertos. Los diagramas y el PDF histórico que los contiene permanecen intactos. La versión actual sin UML se entrega en output/pdf/informe-diseno-actualizado.pdf.

## 1. Alcance ejecutable

La aplicación es una demo Java 17/Swing. Permite publicar, buscar y solicitar propuestas, aceptar, pagar con SkillCoins ficticios, iniciar, completar y calificar. El selector permite alternar entre Mateo, Ana y Lucía. Las identidades, email y push son simulados: no hay autenticación, envío externo ni pagos reales.

La aplicación siempre persiste en MySQL con repositorios JPA: no hay un modo en memoria. Flyway aplica migraciones V1-V5 y Hibernate valida el esquema. Las pruebas usan los mismos repositorios JPA sobre H2 y, con el perfil mysql-it, sobre MySQL real. #75 distingue profesionales y organizaciones y exige tipos explícitos. La demo no agrega alta visual de organizaciones ni un desafío inicial, pendientes de sus tareas correspondientes.

## 2. Capas y dependencias

| Capa | Responsabilidad y evidencia |
| --- | --- |
| Presentation | SkillSwapView, UiControllers y controllers de búsqueda, perfil, publicaciones, contratación y calificación. Convierten datos y delegan. PresentationException y UiActionHandler muestran errores estables. |
| Application | Servicios por capacidad, puertos y comandos/resultados. Identifican al actor, coordinan agregados, transacciones y eventos. No importan Swing ni JPA. |
| Domain | Profile, Publication, Booking, Wallet, Rating, estados, políticas, Builder y cotizaciones. Conserva invariantes y depende de Java estándar. |
| Infrastructure | Repositorios JPA, mappers, migraciones, transacciones y adapters. Implementa los puertos internos. |
| Composición | SkillSwapApplicationWiring selecciona implementaciones. SkillSwapUiControllers (#32) arma un controller por pantalla (búsqueda, publicaciones, contrataciones, calificaciones, billetera, notificaciones y perfil) y la vista habla con cada uno; no delega métodos ni tiene lógica. La sesión demo no es un controller: DemoCurrentUserProvider es la identidad activa y lista los perfiles activos de la base para cambiar de usuario. |

Las entidades JPA no atraviesan los puertos. Los resultados de Application y DTOs de UI contienen datos, enums y colecciones copiadas. PresentationContractsTest comprueba que los controllers dedicados no expongan entidades ni dependan de persistencia.

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
| Facade | PublicProfileFacade implementa PublicProfileUseCases: una sola consulta arma el perfil público (§4.1) coordinando perfiles, publicaciones, contrataciones y calificaciones en una transacción de lectura. Solo compone; las reglas quedan en las entidades y las consultas. Decisión en decision-perfil-publico.md (#40). |
| Singleton localizado | Los enums reutilizan estados inmutables. No hay un Singleton mutable de aplicación o sesión. |
| Repository / DAO | Puertos y adaptadores aíslan almacenamiento. Patrón de acceso a datos, distinto del catálogo GoF. |
| MVC / Controller | Swing presenta, controllers convierten/delegan y Application/Domain ejecutan. La fachada no justifica concentrar reglas de todas las áreas. |

Agregar un canal modifica el enum y switch de Simple Factory, su adapter y configuración; los casos de uso conservan el contrato. No se afirma OCP sin modificaciones en toda la solución.

Factory Method se descartó por ausencia de una variación de creación mediante subclases; ver decision-factory-method-vs-simple-factory.md. El nombre create o la ausencia de static no cambian esa clasificación. Abstract Factory no tiene una familia de productos relacionados que construir. Composite (#37) y la decisión formal de CQRS (#42) permanecen fuera de esta entrega.

## 5. GRASP y SOLID

Information Expert ubica reglas en quien conoce los datos. Creator aparece en métodos de creación y Builder. Controller recibe acciones. Polymorphism se aplica a State, Strategy y cotizaciones. Low Coupling, Indirection y Protected Variations se apoyan en puertos/adapters. Pure Fabrication ubica mappers/transacciones fuera del dominio. High Cohesion exige separar capacidades.

SRP separa crear, adaptar, coordinar y persistir. DIP mantiene contratos internos independientes. ISP agrupa capacidades. OCP/LSP permiten sustituir estrategias/adapters respetando contratos, con el compromiso explícito de la fábrica. #32 separó billetera y notificaciones en controllers propios, dejó la sesión demo como contexto de identidad y eliminó la clase que concentraba todas las pantallas; no se declara una aplicación perfecta de todos los principios.

## 6. Persistencia y consistencia

Flyway versiona esquema y restricciones: V2 conserva el mensaje opcional, V3 ordena el ledger, V4 adapta comparaciones de búsqueda y V5 agrega tipos. Hibernate usa validate.

JpaTransactionManager comparte EntityManager, confirma o revierte y ejecuta callbacks después del commit. Los repositorios bloquean recursos para cupos, pagos y calificaciones; las billeteras se adquieren en orden estable. MySQL usa READ_COMMITTED. La búsqueda pagina/filtra en persistencia y completa autores por lotes.

Los repositorios en memoria y SynchronousTransactionManager se eliminaron: no aportaban rollback ni garantías de concurrencia y duplicaban cada consulta con una segunda implementación que había que mantener igual a la de JPA. JPA conserva datos y el seed evita duplicados.

El pago de main transfiere directamente al proveedor. No existe custodia ni liberación al completar. Esta diferencia respecto del criterio de #19 es una brecha de negocio preexistente, fuera de la integración #65, que se debe resolver por separado. El guion ya no describe una custodia inexistente. Preparar este PR no certifica terminado todo el proyecto.

## 7. Correcciones encontradas al probar Swing

BorderLayout colocaba texto y estado de una contratación en la misma región; la etiqueta tapaba título, contraparte, objetivo y mensaje. Ahora el estado integra el panel de texto y los datos permanecen visibles.

BookingView incorpora el título que Application obtiene desde la publicación. Presentation deja de depender de una caché de búsquedas; cambiar de usuario o crear otra fachada conserva el título. La contraparte se calcula con la identidad activa, sin fijar Mateo.

SkillSwapViewTest reproduce el problema de layout y verifica dimensiones visibles. SkillSwapUiControllersTest comprueba título sin caché y contraparte desde ambos participantes. Se mantienen permisos, saldo, unicidad, tipos y paginación. No se editaron diagramas.

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

Este Markdown es fuente del PDF actualizado, generado con scripts/generar-informe.py. Los .mdj, .puml y el PDF histórico con diagramas permanecen intactos. La revisión humana de la cabeza final corresponde a la persona revisora; no se fabrica ni se sustituye.

## 10. Referencias y trazabilidad

- #65 / #33: integración, sesión demo, calificación, pruebas y entrega.
- #63/#64: controllers dedicados integrados en la base.
- #75: profesionales/organizaciones y tipos explícitos.
- #25 / #41: Adapter, Simple Factory y decisión de Factory Method.
- #19, #32, #37, #40, #42: diferencias o tareas del equipo que este PR no cierra.
- JaCoCo: https://www.jacoco.org/jacoco/trunk/doc/maven.html
- Umbrales: https://www.jacoco.org/jacoco/trunk/doc/check-mojo.html
- Repositorio: https://github.com/informatica-UADE/skillswap-tpo-pds-grupo-03
