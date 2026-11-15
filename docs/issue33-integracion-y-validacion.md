# Issue #33 — integración y entrega (sin UML)

## Objetivo y base

Conectar controllers, casos de uso y adapters y completar contratación/calificación.
La revisión del 15/11/2026 incorpora main en `9bdd0c8`, con #63/#64 integrados
mediante `103fd9b` y la actualización de CI #74. El PR pasa a apuntar a main y
permanece en Draft. #75 se corrige en su propia rama y todavía no forma parte de
main; su modelo de organizaciones/desafíos deberá incorporarse aquí cuando se integre.

StarUML y su exportación quedan fuera de este trabajo por solicitud de la responsable. No se generan ni editan diagramas en esta revisión.

## Integración corregida

- Se conservan PaginaPublicaciones, filtros, paginación y los datos reales del autor de Application.
- Perfil, publicaciones, búsqueda, contrataciones y calificaciones delegan en sus controllers dedicados; la fachada no duplica reglas ni transiciones.
- DemoCurrentUserProvider conserva la identidad inyectada hasta que se elige otro usuario de demo. Envuelve el adapter existente tanto en memoria como en JPA, evitando sustituirlo por una identidad fija o hacer un cast incompatible con JPA.
- El selector admite Mateo, Ana y Lucía. Cambiar identidad refresca saludo, pie lateral, saldo y pantalla; los permisos continúan en Application/Domain.
- Se conserva el modo --persistence=jpa y el cierre de la fábrica JPA.
- Los fixtures usan PublicationView.author y ProfileRepository del contrato actual, con el perfil requerido y sin reputaciones inventadas.

## Validación de la revisión

La prueba específica de sesión pasó con tres ejecuciones: conserva la identidad inyectada, rechaza identidades inválidas sin cambiar la sesión y completa solicitar → aceptar → pagar → iniciar → completar → calificar desde WiredUiControllers sobre MySQL/JPA. También comprueba permisos de publicaciones, saldos de ambos participantes y reputación persistida.

Suite completa histórica del 05/11/2026: `mvn -Pmysql-it test`, **237 pruebas,
0 fallos, 0 errores y 0 omitidas**, con MySQL 8.4.13.

Validación local del 15/11/2026: `mvn clean test`, **231 pruebas detectadas,
194 ejecutadas sin fallos ni errores y 37 de MySQL omitidas**, porque ese perfil
no se habilitó localmente. Los conflictos de UiControllers y WiredUiControllers
se resuelven conservando calificación, sesión demo y delegación a los controllers
de main. La actualización posterior de CI #74 no cambia código Java ni recursos.
`git diff --check` está limpio. Los checks de MySQL sobre la nueva cabeza se
informan por separado en GitHub; la validación visual y la cobertura siguen pendientes.

Las pruebas MySQL usan bases aleatorias aisladas y Flyway V1–V4. Comandos: mvn test y mvn -Pmysql-it test, con las variables documentadas en docs/pruebas-mysql.md. Los resultados históricos de 124 pruebas corresponden al corte anterior del 02/11 y no certifican esta integración.

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

La base se actualiza a main porque #63 ya está integrado. Se requieren los checks
remotos y una aprobación sobre la nueva cabeza; las pruebas locales no reemplazan
esos controles. Este PR no se presenta como aceptación completa ni se fusiona en
esta revisión. No se modifican UML ni exportaciones.
