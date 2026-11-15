# Issue #33 — integración y validación del PR #65

Revisión del 15/11/2026 sobre `main` en `e64048c`, con #63/#64 y #75 integrados. El PR conserva los controllers dedicados de perfil, publicaciones y contrataciones de la base y agrega la integración de calificación y sesión demo.

## Cambios y regresiones

- `CalificacionUiController` conecta la vista con Application y conserva autorización, estado completado y unicidad de calificación.
- `DemoCurrentUserProvider` envuelve el proveedor existente en memoria/JPA y conserva su identidad hasta una selección explícita. Mateo, Ana y Lucía refrescan saludo, pie, saldo y pantalla sin sustituir permisos de Application/Domain.
- Los contratos y formularios conservan `TipoActor`/`TipoPublicacion` de #75, la búsqueda paginada y los autores reales.
- La lista de contrataciones muestra título, objetivo, mensaje y estado juntos. Application aporta el título; la fachada no depende de una caché ni fija a Mateo como contraparte.
- Las pruebas reproducen el layout defectuoso, la lectura sin caché y la contraparte desde ambos participantes. Se conserva el cierre de la fábrica JPA.

## Evidencia de aceptación

| Criterio | Resultado |
| --- | --- |
| Composición memoria/JPA y selector | Verificados automáticamente; sin reemplazar autenticación real |
| Recorrido publicar → solicitar → aceptar → pagar → completar → calificar | Ejecutado en Swing; [registro y siete capturas](validacion-visual-pr65.md) |
| Identidad, saldo, reputación y notificaciones | Verificados en la UI; objetivo vacío, saldo insuficiente y duplicado rechazados |
| Pruebas locales | `mvn -Pcoverage clean verify`: 263 detectadas, 224 ejecutadas, 39 MySQL omitidas; cero fallos y errores |
| Cobertura de líneas | Domain 91,39% (658/720), Application 85,17% (534/627); umbral de 80% separado por capa |
| Migraciones, rollback, concurrencia, paginación y recorrido JPA | Suite `mysql-it` en MySQL real; consultar el check del SHA final del PR |
| Informe de diseño | [Fuente actualizada](informe-diseno.md) y [PDF sin UML](../output/pdf/informe-diseno-actualizado.pdf) |
| UML | Excluido por instrucción de la responsable; `.mdj`, `.puml` y PDF histórico intactos |
| Aprobación de la cabeza final | Corresponde a la revisión humana; no se sustituye con tests |

## Reproducción

```powershell
mvn -Pcoverage clean verify
powershell -File scripts/coverage-summary.ps1
mvn exec:java
```

El perfil opcional `coverage` genera HTML/XML/CSV en `target/site/jacoco` y falla si Domain o Application no alcanza el 80% de líneas. No excluye clases de esas capas. El resumen versionado de esta ejecución está en [cobertura-local.json](evidencia/pr65/cobertura-local.json); las ramas se informan sin imponerles el umbral de líneas.

Con las variables de [pruebas-mysql.md](pruebas-mysql.md), ejecutar `mvn -Pmysql-it,coverage clean verify`. El workflow de MySQL usa ese comando y publica resultados y cobertura. La cobertura local no incluye los tests MySQL omitidos; los resultados de CI corresponden al SHA indicado en GitHub. Las cifras históricas de 124, 237 y 267 pruebas pertenecen a revisiones anteriores.

La validación visual usa memoria. La suite JPA comprueba la misma composición sobre una base aislada y Flyway V1–V5, incluidos permisos, saldos, tipos y reputación persistida; no certifica carga de producción ni todos los intercalados posibles.

## Límites del alcance

El selector, email/push y SkillCoins son de demostración. Memoria no ofrece rollback ni bloqueo. El pago actual transfiere directamente al proveedor: la custodia indicada en #19 es una brecha preexistente y se documenta para resolver por separado. Wallet/Notificacion controllers dedicados (#32), Composite (#37), Facade de perfil (#40) y evaluación CQRS (#42) no se implementan dentro de esta integración.

El PR se prepara para revisión hacia `main`, sin fusionarlo. El diff no agrega cambios UML; los archivos heredados de #75 conservan el contenido de la base.
