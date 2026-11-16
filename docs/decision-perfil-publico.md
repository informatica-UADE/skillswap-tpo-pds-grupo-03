# Decisión de diseño: perfil público con Facade — issue #40

## Problema

La consigna pide (§4.1) "consultar el perfil público con reputación, publicaciones activas e historial resumido", y la tabla de patrones (§7) sugiere Facade para la "vista resumida del perfil". En `main` solo existía `viewMyProfile()`: el titular podía ver su propio perfil, pero nadie podía ver el de otro usuario, y la pantalla mostraba `proyectosCompletados = 0` fijo.

Armar esa vista requiere leer cuatro subsistemas distintos, cada uno detrás de su puerto:

| Dato | Subsistema | Puerto |
| --- | --- | --- |
| Nombre, tipo de actor, biografía, habilidades, reputación y estado | Perfiles | `ProfileRepository` |
| Publicaciones activas | Publicaciones | `PublicationRepository` |
| Contrataciones completadas | Contrataciones | `BookingRepository` |
| Últimas calificaciones recibidas | Calificaciones | `RatingRepository` |

Sin un punto de entrada único, quien quiera mostrar el perfil (el controller de la UI u otra interfaz futura) tendría que conocer los cuatro puertos, el orden en que se leen y cómo se combinan.

## Decisión

1. **Caso de uso de lectura.** `PublicProfileUseCases.viewPublicProfile(PublicProfileQuery): PublicProfileView` es un puerto de entrada nuevo. Es una consulta: no modifica datos ni exige que el usuario consultado sea el usuario activo.
2. **Facade.** `PublicProfileFacade` implementa el puerto y coordina los cuatro subsistemas con un solo método. Es la única clase que conoce cómo se arma el perfil público; el cliente hace una llamada y no sabe qué repositorios intervienen.
3. **Consultas eficientes.** Cada puerto de salida suma una consulta específica, resuelta en la base:
   - `PublicationRepository.findPublishedByAuthor(autor, página, tamaño)`: solo publicaciones `PUBLISHED`, de la más reciente a la más antigua, paginadas con `LIMIT/OFFSET` y un `COUNT`.
   - `BookingRepository.countCompletedByProvider(usuario)` y `countCompletedByBuyer(usuario)`: dos `COUNT` sobre contrataciones `COMPLETED`.
   - `RatingRepository.findRecentBySubjectUserId(usuario, límite)`: las últimas N calificaciones con `ORDER BY ... LIMIT`.

   Usan los índices existentes (`idx_publications_author`, `idx_bookings_provider`, `idx_bookings_buyer`, `idx_ratings_subject`); no hace falta una migración. Ninguna carga listados completos para filtrarlos en memoria.
4. **Historial resumido.** Cantidad de contrataciones completadas como proveedor y como cliente, y las últimas 3 calificaciones recibidas (puntaje, comentario y fecha).
5. **Privacidad.** La vista expone nombre visible, tipo de actor, biografía, habilidades, reputación, cantidad de calificaciones, publicaciones activas e historial. Quedan afuera los intereses, la zona horaria, los canales de notificación, la disponibilidad, la billetera, las publicaciones que no están publicadas y quién escribió cada calificación.
6. **Perfil dado de baja.** Si el perfil no existe o está dado de baja, la consulta responde `NOT_FOUND` ("El perfil no existe o no está disponible") sin leer los otros subsistemas, así no se filtra ningún dato del titular.
7. **Consistencia.** Las cuatro lecturas se hacen dentro de una misma transacción (`TransactionManager.required`), para que la vista no mezcle datos de momentos distintos.

## Colaboraciones que simplifica Facade

```
PerfilUiController (Presentation)
        │ viewPublicProfile(query)
        ▼
«interface» PublicProfileUseCases ◁┄┄ PublicProfileFacade
                                        ├──▶ ProfileRepository       (perfil, reputación, estado)
                                        ├──▶ PublicationRepository   (página de publicaciones activas)
                                        ├──▶ BookingRepository       (conteos de completadas)
                                        ├──▶ RatingRepository        (últimas calificaciones)
                                        └──▶ TransactionManager      (lectura consistente)
```

El cliente pasa de cuatro dependencias y una secuencia de lecturas a una dependencia y una llamada. Si mañana el historial incluye otro dato, cambia la Facade y su vista; el controller no.

**Por qué es Facade y no un caso de uso más:** en el material de la materia, Facade expone "una interfaz simple para un subsistema complejo" y es "la única clase acoplada a los componentes" que oculta (ejemplo `HomeTheaterFacade.watchMovie()`). Acá los componentes son cuatro subsistemas independientes y la Facade no agrega reglas: solo compone. Si tomara decisiones de negocio, sería un servicio común con otro nombre.

**Por qué no es el controller:** la teoría distingue ambos roles ("¿Controller es un Facade?"). El controller recibe eventos de la interfaz y traduce; la Facade simplifica un subsistema y no conoce la UI. Por eso vive en Application, detrás de un puerto de entrada, y la podría usar cualquier interfaz (Swing hoy, una API mañana).

## Rol de cada componente en esta consulta

| Componente | Clase | Responsabilidad | No hace |
| --- | --- | --- | --- |
| Entidad de dominio | `Profile`, `Publication`, `Booking`, `Rating` | Guardan sus reglas: el perfil conoce su reputación (derivada de calificaciones verificadas) y si está activo; la publicación, su estado; la contratación, si está completada. | No conocen la vista pública ni la persistencia. |
| DTO de entrada | `PublicProfileQuery` | Valida la entrada (usuario presente, página ≥ 0, tamaño entre 1 y 50). | No contiene reglas de negocio. |
| DTO de salida (modelo de lectura) | `PublicProfileView`, `CompletedBookingsSummary`, `ReceivedRatingView` | Transporta solo datos públicos y simples. Contiene `PublicationView` (otro DTO), nunca entidades. | No tiene comportamiento. |
| Puerto de entrada | `PublicProfileUseCases` | Contrato del caso de uso para Presentation. | — |
| Facade (Application) | `PublicProfileFacade` | Coordina las cuatro lecturas en una transacción y arma la vista. | No decide qué es visible ni calcula reputación. |
| Mapper de Application | `PublicProfileApplicationMapper` | Elige qué datos del perfil se publican y convierte entidades en DTOs. | No accede a repositorios. |
| Puertos de salida | `ProfileRepository`, `PublicationRepository`, `BookingRepository`, `RatingRepository` | Interfaces definidas en Application, la capa que las necesita. | No conocen JPA. |
| Infraestructura | `Jpa*Repository` | Implementan las consultas sobre MySQL (H2 en las pruebas). | No agregan reglas. |
| Controller (Presentation) | `PerfilUiController.verPerfilPublico(perfilUserId, pagina)` | Arma `PublicProfileQuery` con el tamaño de página de la pantalla, delega en el puerto y convierte la vista a `Models.PerfilPublico` con `UiModelMapper`. No exige que el perfil sea del usuario activo. | No arma el perfil ni conoce los repositorios. |
| Vista (Swing) | `SkillSwapView.showPublicProfile` | Muestra encabezado, habilidades, historial, últimas reseñas y publicaciones activas con Anterior/Siguiente. Se abre haciendo clic en el nombre de un usuario, que se muestra como enlace (azul y subrayado): el autor en las tarjetas de publicaciones (Inicio, Explorar y el propio perfil público) y en el detalle, y la contraparte en Contrataciones. | No llama a Application: solo usa `UiControllers`. |

### Dónde vive cada regla

| Regla | Dónde | Por qué ahí |
| --- | --- | --- |
| Un perfil dado de baja no se muestra | `Profile.active()` (entidad); la Facade solo lo consulta | El estado del perfil es del perfil. |
| La reputación sale de calificaciones verificadas | `Profile.recalculateReputation` (ya existía) | La Facade lee el valor; no lo recalcula. |
| Solo las publicaciones `PUBLISHED` son visibles para otros | Estado de `Publication` (State) y la consulta del repositorio | Es el mismo criterio del catálogo público (`searchPublished`, #16). Se aplica en la consulta para poder paginar en la base sin traer todo. |
| Una contratación cuenta como historial solo si está `COMPLETED` | Estado de `Booking` (State) y el `COUNT` del repositorio | Igual que el anterior: contar en la base, no en memoria. |
| Qué datos del perfil son públicos | `PublicProfileApplicationMapper` (Application) | Es una decisión del modelo de lectura, no una regla del agregado. Ponerla en `Profile` acoplaría la entidad a una pantalla. |

## Errores, validaciones y transacciones

- **Validación de entrada:** `PublicProfileQuery` rechaza usuario vacío, página negativa y tamaños fuera de 1..50 con `IllegalArgumentException`. Una consulta nula es `ApplicationException(VALIDATION)`.
- **Reglas de negocio:** las protegen las entidades (estado del perfil, de la publicación y de la contratación).
- **Errores de Application:** `NOT_FOUND` para perfil inexistente o dado de baja; `VALIDATION` para entrada inválida.
- **Traducción en Presentation:** `UiErrorMapper` ya convierte `ApplicationException` y `IllegalArgumentException` en `ErrorPresentacion` (`NO_ENCONTRADO`, `VALIDACION`), y `UiOperationExecutor` traduce los errores de persistencia a `PERSISTENCIA` sin mostrar detalles técnicos.
- **Producción con HTTP:** el TP no expone una API REST (la consigna lo excluye). Si se expusiera, la traducción iría en Presentation con este mapeo: `VALIDATION → 400`, `UNAUTHENTICATED → 401`, `FORBIDDEN → 403`, `NOT_FOUND → 404`, `CONFLICT` e `INVALID_STATE → 409`, error de persistencia o inesperado → `500`, sin exponer la excepción interna.
- **Transacción:** la Facade abre una transacción de lectura con el puerto `TransactionManager`. En JPA, los cuatro repositorios comparten el mismo `EntityManager` y la misma conexión.

## Entidades de dominio y entidades JPA

Se mantienen separadas, como en el resto del proyecto: el dominio no depende de Jakarta Persistence y los repositorios JPA convierten con mappers. Para esta consulta eso significa que las consultas nuevas se escriben sobre `PublicationEntity`, `BookingEntity` y `RatingEntity`, y devuelven entidades de dominio que la Facade convierte en DTOs. El costo es un mapeo más; la ventaja es que el estado `COMPLETED` o `PUBLISHED` sigue siendo un concepto del dominio y la base se puede cambiar sin tocar la Facade.

## Interfaces y lo que no se agregó

- **`PublicProfileUseCases`:** hay una necesidad real. Presentation depende del contrato y no de la Facade concreta, igual que con el resto de los casos de uso.
- **Puertos de salida:** se extendieron los existentes. No se creó un repositorio "de perfil público", porque cada dato pertenece a un agregado que ya tiene su repositorio.
- **No se agregó:**
  - una entidad `PublicProfile`: es una proyección de lectura, no un concepto con reglas propias;
  - una migración: los índices necesarios ya existen;
  - un modelo CQRS separado: se analiza en #42;
  - un repositorio de lectura aparte;
  - una caché.

  Ninguno resuelve un problema concreto del enunciado.

## Estrategia de pruebas

| Prueba | Tipo | Cubre |
| --- | --- | --- |
| `PublicProfileFacadeTest` | Unitaria de Application, con dobles de los cuatro repositorios y de `TransactionManager` | Arma la vista completa; pide una sola página y conteos (los dobles fallan si se usan los listados completos); la vista no tiene campos privados ni el autor de cada calificación; perfil dado de baja o inexistente → `NOT_FOUND` sin leer otros subsistemas; validación de entrada; las cuatro lecturas en una sola transacción. |
| `PublicProfileQueriesContract` | Contrato ejecutable de los puertos de salida | Solo publicaciones `PUBLISHED` del autor, orden y paginación con total; conteos de `COMPLETED` por rol; últimas N calificaciones ordenadas. |
| `JpaPublicProfileQueriesTest` | Integración de Infrastructure con JPA sobre H2 (corre siempre) | Las consultas JPQL y los mapeos. |
| `MySqlPublicProfileQueriesTest` | Integración con MySQL real y las migraciones de Flyway (perfil `mysql-it`) | Las mismas consultas sobre el esquema real, con claves foráneas e índices. |

| `PerfilUiControllerTest` | Unitaria de Presentation, con dobles de los puertos | Convierte la consulta (usuario, página, tamaño) y la vista; no exige el usuario activo; valida antes de llamar al puerto; traduce `NOT_FOUND` a `NO_ENCONTRADO`. "Mi perfil" muestra el rol y los proyectos completados reales. |
| `SkillSwapUiControllersTest` | Integración de Presentation con Application y repositorios JPA sobre H2 | El autor de una publicación se identifica por `proveedorId`; después de un ciclo completo (solicitar, aceptar, pagar, iniciar, completar, calificar) el perfil público muestra la contratación completada, la reputación y la reseña, y "Mi perfil" del cliente cuenta la completada. |
| `SkillSwapViewTest` | Prueba de la pantalla Swing | El nombre abre el perfil público desde una tarjeta de Inicio (sin entrar al detalle), desde Contrataciones y desde el detalle de una publicación, y "← Volver" regresa a la pantalla de origen. |
| `PresentationContractsTest` | Contrato de los DTO | `PublicProfileData` y `Models.PerfilPublico` solo contienen datos simples y otros DTO. |

Las reglas de los agregados (estados de publicación y contratación, reputación) ya están cubiertas por las pruebas de dominio existentes.

## Supuestos

- Un perfil público se puede consultar sin estar autenticado como su titular: es solo lectura y no expone datos privados.
- Una publicación `PAUSED` es visible solo para su autor (§4.2), así que no aparece en el perfil público.
- El historial cuenta todas las contrataciones completadas del titular, en cualquier rol, aunque la publicación ya esté cerrada.
- Las calificaciones se muestran sin su autor para no exponer con quién contrató cada usuario.
- Organizaciones y profesionales tienen el mismo perfil público (#38).

## Conexión con la UI

Se conectó después de #65:

- `PerfilController.verPerfilPublico(perfilUserId, pagina)` devuelve `Models.PerfilPublico` (perfil, página de publicaciones de `TAMANIO_PAGINA = 6`, completadas por rol y `Resena` sin autor).
- Para llegar al perfil, la vista necesita el id del otro usuario: `Models.Publicacion` suma `proveedorId` y `Models.Contratacion` suma `contraparteId`. Antes solo traían el nombre.
- "Mi perfil" y el Inicio dejan de mostrar valores fijos: `ProfileView` suma `completedBookings` (las mismas dos consultas de `BookingRepository`, dentro de la transacción de `ProfileApplicationService`) y el rol sale del tipo de actor ("Profesional" u "Organización"). "Mi perfil" no pasa por la Facade porque el titular ve su perfil aunque esté dado de baja.

### El nombre de la contraparte no pasa por la Facade

Contrataciones muestra el nombre de la otra parte, que se tiene que ver aunque esa persona haya dado de baja su perfil; canalizarlo por la Facade haría fallar la lista con perfiles inactivos y haría cuatro consultas por fila. Hasta #32 se resolvía con `DiscoveryUseCases.author()`, una consulta por contratación; desde #32 `BookingView` trae los nombres de ambas partes, leídos en una sola consulta de perfiles, y `author()` se eliminó (ver [decisión de controllers](decision-controllers-ui.md)).

## Contrato del caso de uso

| Campo | Valor |
| --- | --- |
| Caso de uso | Consultar el perfil público de un usuario |
| Puerto | `PublicProfileUseCases.viewPublicProfile(PublicProfileQuery)` |
| Entrada | `userId` (obligatorio), `page` ≥ 0 y `size` entre 1 y 50 para las publicaciones activas |
| Salida | `PublicProfileView` con datos públicos, reputación, cantidad de calificaciones, página de publicaciones activas, contrataciones completadas por rol y últimas 3 calificaciones |
| Errores | `VALIDATION` (entrada inválida) y `NOT_FOUND` (perfil inexistente o dado de baja) |
| Efectos | Ninguno: es una consulta |
| Transacción | Una transacción de lectura para las cuatro consultas |
