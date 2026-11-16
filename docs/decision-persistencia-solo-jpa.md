# Decisión: persistencia solo con JPA y MySQL

## Problema

La consigna pide persistir con MySQL, Hibernate y JPA (§8). El proyecto tenía además una implementación en memoria de cada repositorio (`InMemoryProfileRepository`, `InMemoryPublicationRepository`, `InMemoryBookingRepository`, `InMemoryWalletRepository`, `InMemoryRatingRepository` e `InMemoryNotificationRepository`) y `SynchronousTransactionManager`, y la aplicación arrancaba en memoria salvo que se pasara `--persistence=jpa`.

Eso traía tres problemas:

- **Dos implementaciones de cada consulta.** La búsqueda, el perfil público y los conteos había que escribirlos dos veces y mantenerlos iguales (por ejemplo, la comparación sin acentos de MySQL replicada en Java).
- **Un modo que no cumple la consigna.** En memoria no hay rollback, ni bloqueo de cupos, ni datos que sobrevivan al cierre. La demo por defecto mostraba justamente lo que la consigna no pide.
- **Pruebas que validaban la copia y no la persistencia real.** Varias pruebas de integración pasaban por los repositorios en memoria y no por los JPA, que son los que se entregan.

## Decisión

1. **Se eliminan los repositorios en memoria y `SynchronousTransactionManager`.** Cada puerto de salida queda con una sola implementación, la JPA, en `infrastructure.persistence.repository`.
2. **La aplicación siempre usa MySQL.** `SkillSwapApplication` arma la wiring con `SkillSwapApplicationWiring.persistent()`: Flyway migra y Hibernate valida el esquema. Las credenciales salen de `SKILLSWAP_DB_URL`, `SKILLSWAP_DB_USER` y `SKILLSWAP_DB_PASSWORD`; si falta alguna, la aplicación no arranca e indica cuál. El flag `--persistence=jpa` desaparece.
3. **Las pruebas usan los mismos repositorios JPA.**
   - Las que no necesitan MySQL real corren sobre H2 en memoria, con una base nueva por prueba (`H2Database`, solo en `src/test`). Hibernate genera el esquema desde las entidades.
   - Lo propio de MySQL (migraciones de Flyway, colación sin acentos, bloqueos y concurrencia) sigue en las pruebas del perfil `mysql-it`.
4. **Se elimina `InAppNotificationGateway`**, que no usaba nadie y dependía del repositorio en memoria de notificaciones.

## Lo que se mantiene

| Componente | Por qué |
| --- | --- |
| `InMemoryDomainEventPublisher` | No es un repositorio: es el bus de eventos del Observer, y la composición JPA lo usa para las notificaciones. Que guarde las suscripciones en memoria es lo esperado. |
| `MockSkillSwapControllers` | Es el Mock UI Kit de la cátedra (§10); no usa los repositorios. |
| Los dobles de prueba de los services (`Stub*`, `ImmediateTransactionManager`, etc.) | Son los "puertos simulados" que pide la consigna para probar los casos de uso (§12). |
| `DemoCurrentUserProvider` y los datos de demostración | El login simulado y los datos reproducibles que pide la consigna; ya funcionaban con JPA. |

## Dependencias y responsabilidades

- **Application** sigue dependiendo solo de los puertos (`ProfileRepository`, `PublicationRepository`, `PublicationQueryPort`, etc.) y de `TransactionManager`. No cambia: la decisión es de Infrastructure y de la composición.
- **Infrastructure** implementa cada puerto una sola vez con JPA, y `JpaTransactionManager` define los límites transaccionales.
- **`SkillSwapApplicationWiring`** es el único lugar que conoce las clases concretas. Recibe la `EntityManagerFactory` desde afuera (`usingJpa`): la aplicación le pasa la de MySQL y las pruebas, la de H2.

## Estrategia de pruebas

| Prueba | Base | Cubre |
| --- | --- | --- |
| Domain y services (`*ApplicationServiceTest`, `DomainRulesTest`, etc.) | Ninguna, con dobles | Reglas de negocio y orquestación. Sin cambios. |
| `SkillSwapUiControllersTest`, `SkillSwapViewTest`, `SkillSwapApplicationWiringTest`, `ProfilePublicationControllersIntegrationTest` | H2 | Controllers, casos de uso y repositorios JPA juntos, con los datos de demostración. |
| `DemoCurrentUserProviderTest`, `NotificationEventObserverTest` | H2 | Identidad de la demo y notificaciones sobre los repositorios JPA. |
| `JpaPublicProfileQueriesTest`, `JpaPublicationQueryAdapterTest`, `JpaBookingClientMessageTest` | H2 | Consultas y mapeos JPA. |
| `MySqlPersistenceIntegrationTest`, `JpaPublicationSearchContractTest`, `MySqlPublicProfileQueriesTest` | MySQL (`mysql-it`) | Migraciones, búsqueda sin acentos, rollback, bloqueos y concurrencia. |

Al eliminar la versión en memoria, el contrato de búsqueda (`PublicationSearchContract`) corre solo contra MySQL, porque depende de la colación de la base. `mvn test` sin perfil ya no lo ejecuta; lo ejecuta el CI, que corre `mysql-it`.

## Supuestos

- Quien ejecuta la aplicación tiene un MySQL 8.4 disponible; el README indica cómo levantarlo con Docker.
- H2 en modo MySQL alcanza para las pruebas que no dependen de la colación ni de la concurrencia de MySQL; esas siguen en `mysql-it`.
- Las fechas que se guardan pierden los nanosegundos (MySQL y H2 guardan hasta microsegundos), así que las pruebas que comparan fechas leídas de la base las truncan.
