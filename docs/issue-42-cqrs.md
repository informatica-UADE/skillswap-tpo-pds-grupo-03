# Issue #42: CQRS incremental para publicaciones

## Decisión y alcance

Se implementa una separación pragmática de comandos y consultas en el catálogo de publicaciones,
sin introducir una segunda base de datos ni consistencia eventual. Ambas rutas usan MySQL y los
cambios se hacen dentro de las transacciones existentes. El beneficio concreto está en la búsqueda
paginada: antes se reconstruían agregados `Publication` y sus relaciones para convertirlos después
en vistas; ahora la lectura proyecta directamente los campos necesarios.

La separación es a nivel de contratos y adaptadores, no de procesos ni almacenamiento:

- **Escritura:** los casos de uso de publicación usan `PublicationRepository` para cargar y guardar
  agregados. Las invariantes y transiciones siguen delegadas a `Publication`; los cambios son
  transaccionales.
- **Lectura del catálogo:** la búsqueda, categorías y resumen público de autor usan
  `PublicationQueryPort`. El adaptador JPA devuelve `PublicationView`, aplica filtros, orden,
  conteo y paginación en la base.
- **Recomendaciones:** continúan usando agregados y `RecommendationStrategy`, ya que la estrategia
  de dominio opera sobre `Profile` y `Publication`. No se incorporó una segunda proyección de
  recomendaciones sin una necesidad medida.
- **Listado propio:** conserva el caso de uso existente y su DTO; no es parte de la optimización
  de búsqueda del catálogo.

## Contratos y modelos

| Paquete / clase | Responsabilidad |
|---|---|
| `application.model.PublicationData.CreateDraftCommand` | Datos inmutables de entrada para crear un borrador. |
| `application.model.PublicationData.EditDraftCommand` | Datos inmutables de entrada para editar un borrador. |
| `application.model.PublicationData.ChangeStatusCommand` | Solicitud de transición de estado. |
| `application.model.DiscoveryData.SearchQuery` | Filtros, orden y página del catálogo; valida sus límites. |
| `application.model.PublicationData.PublicationView` | Proyección de lectura del catálogo, sin exponer entidades JPA. |
| `application.port.in.PublicationCommandUseCases` | Contratos de creación, edición y cambio de estado. |
| `application.port.in.PublicationQueryUseCases` | Consultas de publicaciones propias y tipos permitidos. |
| `application.port.out.PublicationRepository` | Persistencia/carga de agregados y paginación existente de publicaciones del perfil público; no contiene la búsqueda del catálogo. |
| `application.port.out.PublicationQueryPort` | Búsqueda paginada, categorías y datos públicos de autor. |
| `infrastructure.persistence.repository.JpaPublicationQueryAdapter` | Proyección JPA paginada para el catálogo. |
| `infrastructure.InMemoryPublicationRepository` | Implementación en memoria de los puertos de agregado y consulta. |

`PublicationApplicationService` implementa los dos puertos de entrada para conservar la
orquestación de publicación, pero sus contratos de comandos y consultas son distintos.
`DiscoveryApplicationService` coordina descubrimiento y depende explícitamente del puerto de
consulta para el catálogo.

## Flujos

### Comando: publicar un borrador

```text
PublicacionUiController
  -> PublicationCommandUseCases.changeStatus(ChangeStatusCommand)
  -> PublicationApplicationService
  -> TransactionManager.required(...)
  -> PublicationRepository.findByIdForUpdate(id)
  -> Publication.publish(actor)          // valida permisos e invariantes del agregado
  -> PublicationRepository.save(aggregate)
  -> respuesta DTO para la interfaz
```

La transacción cubre la carga del agregado, la transición de dominio y el guardado. La búsqueda
posterior ve el cambio desde la misma base; no hay sincronización entre almacenes.

### Query: buscar publicaciones

```text
BusquedaUiController
  -> DiscoveryUseCases.search(SearchQuery)
  -> DiscoveryApplicationService
  -> PublicationQueryPort.searchPublished(SearchQuery)
  -> JpaPublicationQueryAdapter
       -> consulta paginada de columnas y autor + consulta de conteo
       -> consultas por lote para etiquetas y disponibilidad de la página
  -> PageResult<PublicationView>
```

El adaptador no reconstruye agregados para formar el catálogo. La página y el total se resuelven
en persistencia; etiquetas y disponibilidad se recuperan por lote para los IDs de esa página,
evitando una consulta individual por publicación. El contrato común de búsqueda valida el mismo
comportamiento en memoria y en JPA.

## Persistencia, consistencia y límites

- MySQL sigue siendo el único almacenamiento y fuente de verdad.
- No se añade caché, réplica de lectura, mensajería ni consistencia eventual.
- Los agregados `Booking`, `Wallet` y `Publication` mantienen sus invariantes; CQRS no las desplaza
  a los controllers ni a las proyecciones.
- `PublicationQueryPort` proyecta únicamente la lectura del catálogo. Los flujos de recomendación
  y el listado propio no se migran en esta iteración: así se evita ampliar el cambio sin una mejora
  demostrada.
- Los comandos devuelven DTOs de aplicación consumidos por la UI, nunca entidades de dominio o
  entidades JPA.

## Pruebas y verificaciones

- `PublicationSearchContract` comprueba filtros, visibilidad, orden, conteo y límites de paginación
  para los adaptadores en memoria y MySQL.
- `DiscoveryApplicationServiceTest` comprueba la delegación de criterios, la respuesta proyectada,
  la propagación de errores y que la búsqueda no consulta agregados de perfiles.
- `ProfilePublicationControllersIntegrationTest` cubre comandos y consultas contra el dominio y
  repositorios en memoria, incluida la transición a publicado y el listado propio.
- `MySqlPersistenceIntegrationTest` cubre la búsqueda paginada/proyectada y la persistencia de los
  cambios en MySQL cuando se habilita el perfil `mysql-it`.

Ejecución habitual: `mvn test`. Las pruebas de MySQL son optativas y requieren las variables de
entorno descritas en `docs/pruebas-mysql.md`; se ejecutan con `mvn -Pmysql-it test`.
