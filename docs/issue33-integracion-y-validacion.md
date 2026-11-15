# Issue #33 — integración y entrega (sin UML)

## Objetivo y base

Conectar controllers, casos de uso y adapters y completar contratación/calificación.
La revisión del 15/11/2026 incorpora main en `e64048c`, que ya contiene
#63/#64, la actualización de CI #74 y el modelo de organizaciones/desafíos de
#75. El PR apunta a main y permanece en Draft.

StarUML y su exportación quedan fuera de este trabajo por solicitud de la responsable. No se generan ni editan diagramas en esta revisión.

## Integración corregida

- Se conservan PaginaPublicaciones, filtros, paginación y los datos reales del autor de Application.
- Perfil, publicaciones, búsqueda, contrataciones y calificaciones delegan en sus controllers dedicados; la fachada no duplica reglas ni transiciones.
- DemoCurrentUserProvider conserva la identidad inyectada hasta que se elige otro usuario de demo. Envuelve el adapter existente tanto en memoria como en JPA, evitando sustituirlo por una identidad fija o hacer un cast incompatible con JPA.
- El selector admite Mateo, Ana y Lucía. Cambiar identidad refresca saludo, pie lateral, saldo y pantalla; los permisos continúan en Application/Domain.
- Se conserva el modo --persistence=jpa y el cierre de la fábrica JPA.
- Los fixtures usan PublicationView.author y ProfileRepository del contrato actual, con el perfil requerido y sin reputaciones inventadas.
- Los contratos de registro y borrador conservan TipoActor/TipoPublicacion explícitos, incluidos los DTOs, el mapper, el mock y el formulario. La sesión demo conserva los permisos al alternar entre profesionales; el seed distingue mentorías y servicio.

## Validación de la revisión

La prueba específica de sesión pasó con tres ejecuciones: conserva la identidad inyectada, rechaza identidades inválidas sin cambiar la sesión y completa solicitar → aceptar → pagar → iniciar → completar → calificar desde WiredUiControllers sobre MySQL/JPA. También comprueba permisos de publicaciones, saldos de ambos participantes y reputación persistida.

Suite completa histórica del 05/11/2026: `mvn -Pmysql-it test`, **237 pruebas,
0 fallos, 0 errores y 0 omitidas**, con MySQL 8.4.13.

Validación local posterior a #75: compilación limpia y `mvn test`, **261 pruebas
detectadas, 222 ejecutadas sin fallos ni errores y 39 de MySQL omitidas**, porque
ese perfil no se habilitó localmente. La regresión de sesión crea un servicio como
Lucía, rechaza un desafío no permitido, cambia a Ana y verifica que no se puedan
listar ni operar publicaciones de otra identidad. Al volver a Lucía se conserva
el tipo del borrador. La prueba JPA del recorrido con calificación también
comprueba los tipos de publicaciones al cambiar de usuario.

`git diff --check` está limpio. Los checks de MySQL sobre la nueva cabeza se
informan por separado en GitHub; la validación visual y la cobertura siguen pendientes.

Las pruebas MySQL usan bases aleatorias aisladas y Flyway V1–V5. Comandos: mvn test y mvn -Pmysql-it test, con las variables documentadas en docs/pruebas-mysql.md. Los resultados históricos de 124 y 237 pruebas corresponden a cortes anteriores y no certifican esta integración.

## Pendientes de aceptación

| Criterio | Estado |
| --- | --- |
| Composición memoria/JPA, mocks y selector | Integrados; prueba específica verificada |
| Recorrido mediante controllers/casos de uso sobre JPA | Verificado automáticamente; no equivale a interacción visual |
| Recorrido visual completo en Swing | Pendiente de ejecución y registro manual |
| Migraciones, rollback, concurrencia y consultas | Cubiertos por la suite de integración; no certifican carga de producción |
| Informe de decisiones final | Pendiente de completar y entregar |
| Cobertura 80% Domain/Application | Pendiente de medición |
| StarUML y PDF | Excluidos de este trabajo; entrega por separado |

La base contiene main con #63 y #75 integrados. Se requieren los checks
remotos y una aprobación sobre la nueva cabeza; las pruebas locales no reemplazan
esos controles. Este PR no se presenta como aceptación completa ni se fusiona en
esta revisión. No se modifican UML ni exportaciones.

El diff contra main conserva exclusivamente la integración de sesión y
calificación, sus pruebas y documentación. Los controllers de perfil,
publicaciones y contrataciones, junto con el modelo de organizaciones/desafíos,
ya pertenecen a la base y no se vuelven a presentar como funcionalidad pendiente.
Los archivos UML de #75 se heredan exactamente de main: no se editan ni se generan
diagramas y el diff de este PR contra main no agrega cambios UML.
