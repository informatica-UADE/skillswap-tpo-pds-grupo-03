# Decisión de diseño: catálogo jerárquico con Composite — issue #37

## Problema

La consigna pide buscar publicaciones por categoría y habilidad (§4.3), que el sistema sea extensible para "incorporar nuevas categorías" (§1), y su tabla de patrones (§7) sugiere Composite para el "catálogo jerárquico de categorías y habilidades".

En `main` la categoría era un texto libre de cada publicación:

- la lista de categorías de la pantalla se armaba leyendo **todas** las publicaciones publicadas (`DiscoveryUseCases.categories()`), así que una categoría sin publicaciones no existía y un error de tipeo creaba una categoría nueva;
- filtrar por "Tecnología" no encontraba una publicación de "Desarrollo de software", aunque es parte de esa área;
- nada impedía guardar una categoría inexistente.

El problema real es jerárquico: una categoría contiene subcategorías y habilidades, y operaciones como "qué habilidades hay en esta rama" o "esta categoría y todo lo que tiene adentro" se aplican igual a una rama entera que a un elemento suelto. Por eso Composite resuelve una variación concreta y no se agrega solo para sumar un patrón.

## Decisión

1. **Composite en Domain** (`domain.catalog`, sin dependencias de JPA ni SQL):
   - `CatalogItem` (componente, clase abstracta `sealed`): identificador, nombre, padre, profundidad y las operaciones comunes `stream()` (recorrido en preorden), `children()`, `skillNames()`, `categoryNames()` y `findCategory(nombre)`.
   - `CatalogCategory` (compuesto): contiene subcategorías y habilidades; `add(item)` es el único punto que modifica el árbol.
   - `CatalogSkill` (hoja): una habilidad.

   El cliente recorre una hoja o una rama con las mismas llamadas, sin preguntar el tipo.
2. **Catálogo fijo, cargado por migración.** No hay un actor administrador en la consigna; el catálogo lo crea la migración `V6__create_catalog.sql`. Agregar o reorganizar categorías es agregar una migración, sin cambiar código. Application solo lo consulta.
3. **La publicación referencia una categoría del catálogo.** Crear o editar un borrador valida que la categoría exista y la guarda con el nombre que registra el catálogo (`"tecnologia"` → `"Tecnología"`). La base lo garantiza además con una clave foránea.
4. **Buscar por una categoría incluye toda su rama.** Filtrar por "Tecnología" devuelve las publicaciones de Tecnología, de sus subcategorías a cualquier profundidad y las que tienen como etiqueta una habilidad de esa rama. Se resuelve dentro de la consulta paginada, sin cargar el catálogo.
5. **Etiquetas y habilidades del perfil siguen siendo texto libre.** Validarlas contra el catálogo cambiaría el alta de perfiles y publicaciones y los datos demo; queda fuera de este issue.
6. **UI con la jerarquía visible.** El combo de Explorar y el del formulario de nueva publicación muestran el árbol con sangría; Inicio muestra las categorías de primer nivel.

## Por qué Composite y no otra estructura

| Alternativa | Por qué no |
| --- | --- |
| Lista plana de categorías (un `enum` o tabla sin padre) | No representa "Tecnología contiene Desarrollo de software", que es lo que pide la búsqueda por rama. |
| Árbol con una sola clase `Category` que tiene una lista de habilidades | Funciona para dos niveles fijos, pero hojas y ramas quedan con operaciones distintas y cada recorrido pregunta "¿tiene hijos?". Composite da una interfaz única y recursiva. |
| Visitor sobre el árbol | Las operaciones son pocas y estables (recorrer, buscar, juntar nombres); un Visitor agregaría clases sin una variación que lo justifique. |

## Colaboraciones

```
SkillSwapView ──▶ BusquedaController / PublicacionController (Presentation, Models.Categoria)
                        │
          ┌─────────────┼──────────────────────────────┐
          ▼             ▼                              ▼
  CatalogUseCases   DiscoveryUseCases           PublicationUseCases       (Application, puertos de entrada)
          │             │  valida la categoría         │ valida y normaliza la categoría
          ▼             ▼                              ▼
  CatalogRepository ◀───┴──────────────────────────────┘                  (puerto de salida)
          │                     PublicationRepository.searchPublished  ─▶ filtra la rama en SQL
          ▼
  JpaCatalogRepository / InMemoryCatalogRepository ──▶ CatalogMapper ──▶ CatalogCategory / CatalogSkill (Domain)
```

## Casos de uso y responsabilidad de cada Service

| Caso de uso | Service | Responsabilidad | No hace |
| --- | --- | --- | --- |
| Consultar el catálogo | `CatalogApplicationService.viewCatalog()` | Lee el árbol en una transacción de lectura y lo convierte en `CatalogNodeView`. | No arma la jerarquía (la arma el repositorio con las reglas del dominio) ni la aplana para la pantalla (eso es de Presentation). |
| Buscar publicaciones por categoría | `DiscoveryApplicationService.search()` | Rechaza una categoría que no está en el catálogo y delega filtros, orden y paginación. | No recorre el árbol ni filtra en memoria: la rama la resuelve la consulta. |
| Crear y editar un borrador | `PublicationApplicationService.createDraft/editDraft()` | Valida que la categoría exista y la pasa al agregado con el nombre del catálogo, en la misma transacción que guarda. | No decide si una publicación puede publicarse sin categoría: eso es del agregado `Publication`. |

`DiscoveryUseCases.categories()` se eliminó: la lista sale del catálogo, no de las publicaciones.

## Rol de cada componente

| Componente | Clases | Rol en este problema |
| --- | --- | --- |
| Entidades de dominio | `CatalogItem`, `CatalogCategory`, `CatalogSkill` | Representan el árbol y protegen sus invariantes (ver la tabla siguiente). |
| DTO de Application | `CatalogData.CatalogNodeView`, `CatalogItemKind` | Árbol de solo lectura para Presentation. No contiene entidades. |
| DTO de Presentation | `Models.Categoria(nombre, nivel)` | Una opción de categoría con su nivel; `toString()` agrega la sangría. `Categoria.TODAS` es la opción "sin filtro" de Explorar. |
| Puerto de entrada | `CatalogUseCases` | Contrato del caso de uso "consultar el catálogo". |
| Service | `CatalogApplicationService` | Orquesta la lectura (ver la tabla anterior). |
| Mapper de Application | `CatalogApplicationMapper` | Convierte el Composite en vistas con la misma recursión para hojas y ramas. |
| Puerto de salida | `CatalogRepository` | `findRootCategories()` y `findCategoryName(nombre)`. Definido en Application, la capa que lo necesita. |
| Infrastructure | `JpaCatalogRepository`, `CatalogMapper`, `CatalogCategoryEntity`, `CatalogSkillEntity`, `InMemoryCatalogRepository`, `DemoCatalog`, migración V6 | Persisten y reconstruyen el árbol; el filtro por rama vive en `JpaPublicationRepository` e `InMemoryPublicationRepository`. |
| Controller | `BusquedaUiController.categorias()` | Llama al caso de uso y aplana el árbol en preorden con `UiModelMapper.toCategorias`; descarta las habilidades. No exige usuario activo porque no modifica información. |

### Dónde vive cada regla

| Regla | Dónde | Por qué ahí |
| --- | --- | --- |
| Un elemento tiene un solo padre | `CatalogCategory.add` | Es quien conoce el árbol al momento de agregar. |
| No hay ciclos: una categoría no se agrega dentro de sí misma ni de sus descendientes | `CatalogCategory.add` (vía `isAncestorOrSelf`) | Ídem. |
| Los hijos de una categoría no repiten nombre | `CatalogCategory.add` | Ídem. |
| El nombre de una categoría es único en todo el árbol | `CatalogCategory.add`, más `UNIQUE (name)` en la base | Las publicaciones referencian la categoría por nombre. |
| Los nombres se comparan sin mayúsculas ni acentos | `CatalogItem.key` y la colación `utf8mb4_0900_ai_ci` | Igual que la búsqueda (V4). |
| La categoría de una publicación existe en el catálogo | `PublicationApplicationService`, más la FK `fk_publications_category` | Es una referencia a otro agregado: `Publication` no conoce el catálogo, así que la valida quien orquesta. La FK es la última barrera. |
| Una publicación necesita categoría para publicarse | `Publication` (ya existía) | Es una invariante del agregado. |
| Las filas guardadas forman un árbol válido (padre existente, sin ciclos, ruta coherente) | `CatalogMapper` | Son errores que solo pueden aparecer en las filas; el árbol se arma con `add`, así que además se aplican las reglas del dominio. |

## Persistencia

**Tablas.** `catalog_categories (id, parent_id, name, path, sort_order)` y `catalog_skills (id, category_id, name, sort_order)`. La jerarquía se guarda dos veces: `parent_id` (para reconstruir el árbol) y la **ruta materializada** `path` (`/tecnologia/desarrollo-software/`). La ruta permite filtrar una rama con `path LIKE '/tecnologia/%'`, que usa el índice único de `path`.

| Alternativa para consultar una rama | Por qué no |
| --- | --- |
| Recorrer el árbol en Java y pasar la lista de categorías a la consulta | Obliga a cargar el catálogo en cada búsqueda. |
| `WITH RECURSIVE` (MySQL 8) | JPQL y Criteria no lo soportan; habría que escribir SQL nativo en la consulta paginada. |
| Tabla de clausura (ancestro, descendiente) | Más filas y mantenimiento para un catálogo fijo y chico. |

La redundancia entre `parent_id` y `path` la controla `CatalogMapper`: si no coinciden, el catálogo guardado se rechaza.

**Referencia por nombre.** `publications.category` sigue guardando el nombre y ahora es clave foránea a `catalog_categories.name` (único). Se eligió el nombre y no el id porque es lo que ya usan el agregado, los DTO, la UI y la búsqueda; cambiarlo a id obligaba a modificar todos esos contratos. Como contrapartida, renombrar una categoría requiere una migración que actualice ambas tablas; con un catálogo fijo es aceptable.

**Datos existentes.** La migración no pierde publicaciones: un borrador con categoría vacía queda en `NULL`, y una categoría que no está en el catálogo se conserva como categoría raíz con id `anterior-<hash>`.

**Entidades JPA separadas del dominio.** El Composite usa referencias al padre, una lista de hijos y recursión. Mapearlo directamente (`@OneToMany` autorreferenciado) metería anotaciones, proxies y carga perezosa en el dominio, y la ruta materializada es un detalle de la base. Por eso hay entidades planas (`CatalogCategoryEntity`, `CatalogSkillEntity`) y `CatalogMapper` arma el árbol con `CatalogCategory.add`. El catálogo se lee con **dos consultas** (categorías y habilidades), sin N+1.

**Modo en memoria.** `DemoCatalog` arma el mismo árbol que V6; `MySqlCatalogRepositoryTest` verifica que coincidan. `InMemoryCatalogRepository` devuelve copias para que nadie modifique el catálogo desde afuera, igual que JPA, que reconstruye uno nuevo en cada lectura.

## Errores, validaciones y transacciones

- **Validación de entrada:** `SearchQuery` y los comandos de publicación ya recortan y validan sus campos.
- **Validación de negocio:**
  - una categoría fuera del catálogo responde `ApplicationException(VALIDATION)`, tanto al buscar como al crear o editar un borrador, sin consultar publicaciones ni guardar nada;
  - las reglas del árbol son `IllegalArgumentException` del dominio.
- **Datos inválidos guardados:** `CatalogMapper` lanza `IllegalStateException("El catálogo guardado es inválido: …")`. No es un error del usuario sino de los datos.
- **Traducción en Presentation:** `UiErrorMapper` convierte `VALIDATION` en `VALIDACION` y la `IllegalStateException` en `ESTADO`.
- **Producción con HTTP:** el TP no expone una API REST. Si la expusiera, el mapeo sería `VALIDATION` → 400 y el catálogo guardado inválido → 500, porque es un error del servidor y no un conflicto de estado del cliente.
- **Transacciones:**
  - `viewCatalog()` hace sus dos consultas en una transacción de lectura;
  - crear y editar validan la categoría dentro de la misma transacción que guarda la publicación;
  - la búsqueda valida la categoría y luego consulta, sin transacción explícita. Es aceptable porque el catálogo solo cambia por migración.

## Interfaces y lo que no se agregó

- **`CatalogRepository`:** necesidad real; Application no conoce JPA y hay dos implementaciones (memoria y JPA).
- **`CatalogUseCases`:** Presentation depende del contrato, igual que con el resto de los casos de uso.
- **Casos de uso de alta y edición del catálogo:** no se agregaron porque la consigna no define quién administra el catálogo; agregarlos obligaba a inventar un rol y sus permisos.
- **Un repositorio por nodo, un `SkillRepository` o un Visitor:** no resuelven nada del problema.
- **Caché del catálogo:** no se agregó; son dos consultas sobre tablas chicas.
- **Una interfaz Java para `CatalogItem`:** es una clase abstracta `sealed` porque comparte estado (id, nombre, padre) y comportamiento; una interfaz obligaría a duplicarlos en hoja y compuesto.

## Estrategia de pruebas

| Prueba | Tipo | Cubre |
| --- | --- | --- |
| `CatalogCompositeTest` | Unitaria de Domain | Recorrido igual para hoja y rama; habilidades y categorías de un subárbol; padre y profundidad; búsqueda sin mayúsculas ni acentos; hijos inmodificables desde afuera; ciclos, doble padre, hermanos repetidos, categoría repetida en otra rama; datos obligatorios. |
| `CatalogApplicationServiceTest` | Unitaria de Application con un doble del puerto | El árbol se devuelve como vistas anidadas y en orden, en una transacción. |
| `PublicationApplicationServiceTest`, `DiscoveryApplicationServiceTest` | Unitarias de Application con dobles | La categoría se guarda con el nombre del catálogo; una categoría inexistente es `VALIDATION` sin guardar ni consultar publicaciones. |
| `CatalogRepositoryContract` | Contrato del puerto de salida | Árbol completo con orden y niveles; cada lectura es un árbol nuevo; categorías de cualquier nivel por nombre; una habilidad no es una categoría. |
| `InMemoryCatalogRepositoryTest`, `JpaCatalogRepositoryTest` | Contrato en memoria y JPA sobre H2 | Que ambos almacenamientos se comporten igual. |
| `CatalogMapperTest` | Unitaria de Infrastructure | Filas inválidas: padre o categoría inexistente, ciclo, ruta desactualizada, nombres repetidos. |
| `PublicationSearchContract` | Contrato de la búsqueda (memoria y MySQL) | Filtro por rama a cualquier profundidad, por etiqueta de una habilidad de la rama, sin incluir padres ni hermanas, con paginación y total en la consulta, y categoría inexistente sin resultados. |
| `MySqlCatalogRepositoryTest` | Integración con MySQL real y Flyway | El árbol de V6 coincide con `DemoCatalog`; comparación sin acentos; la FK rechaza una categoría inexistente y acepta `NULL`; la migración conserva categorías anteriores y deja en `NULL` los borradores vacíos. |
| `BusquedaUiControllerTest` | Unitaria de Presentation | Aplana el árbol en preorden con su nivel y sin habilidades; no exige usuario activo; traduce errores. |
| `WiredUiControllersTest` | Integración de Presentation con Application | Crear en una subcategoría y encontrarla al buscar por sus ancestros; categoría fuera del catálogo como `VALIDACION` al buscar y al crear. |
| `SkillSwapViewTest` | Pantalla Swing | Inicio muestra solo el primer nivel; el combo de Explorar muestra la sangría; filtrar por "Diseño UX" encuentra la publicación etiquetada con sus habilidades. |

## Supuestos

- El catálogo es fijo y lo administra el equipo con migraciones.
- Una publicación puede pertenecer a cualquier categoría, no solo a las que no tienen subcategorías.
- El nombre de una categoría es único en todo el catálogo; el de una habilidad, dentro de su categoría.
- Una publicación entra en una rama por su categoría o por tener como etiqueta una habilidad de esa rama.
- Un borrador puede no tener categoría; publicarlo la exige, como antes.
- Las categorías de publicaciones anteriores al catálogo se conservan como raíces para no perder datos.

## Contratos de los casos de uso

| Caso de uso | Puerto | Entrada | Salida | Errores | Efectos | Transacción |
| --- | --- | --- | --- | --- | --- | --- |
| Consultar el catálogo | `CatalogUseCases.viewCatalog()` | — | `List<CatalogNodeView>`: raíces con subcategorías y habilidades en orden de presentación | Catálogo guardado inválido (`IllegalStateException`) | Ninguno | Una de lectura (dos consultas) |
| Buscar publicaciones | `DiscoveryUseCases.search(SearchQuery)` | Criterios; la categoría, si viene, debe existir | Página de publicaciones de la rama | `VALIDATION` si la categoría no existe | Ninguno | Lecturas sin transacción explícita |
| Crear / editar borrador | `PublicationUseCases.createDraft` / `editDraft` | Comando con la categoría | `PublicationView` con el nombre de la categoría del catálogo | `VALIDATION` si la categoría no existe; los errores de antes | Guarda la publicación | Una, con la validación incluida |
