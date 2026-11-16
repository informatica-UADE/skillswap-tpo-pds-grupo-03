# SkillSwap

SkillSwap es un marketplace académico para ofrecer y contratar mentorías. Este repositorio corresponde al Trabajo Práctico de Proceso de Desarrollo de Software.

## Estado actual

La aplicación ejecutable es una demo de escritorio en Java Swing. La vista usa controllers que conectan perfiles, publicaciones, búsqueda, contrataciones, billetera y notificaciones con servicios de Application. El modo predeterminado usa memoria; `--persistence=jpa` conecta esos mismos servicios a MySQL mediante repositorios JPA. `MockSkillSwapControllers` permanece disponible para pruebas y recorridos aislados.

El modo en memoria no requiere MySQL y pierde los datos al cerrar. El modo JPA conserva los datos entre ejecuciones; Flyway aplica las migraciones antes de que Hibernate valide el esquema. Ambos modos usan identidades y proveedores de demostración y SkillCoins ficticios; no hay autenticación ni pasarela de pago reales.

## Requisitos

- JDK 17 o superior.
- Apache Maven 3.9 o superior.
- Git.

No se necesitan variables de entorno ni credenciales para ejecutar el modo en memoria. El modo JPA requiere una base MySQL 8.4 y las variables indicadas abajo.

## Obtener el proyecto

```bash
git clone https://github.com/informatica-UADE/skillswap-tpo-pds-grupo-03.git
cd skillswap-tpo-pds-grupo-03
```

## Compilar y probar

```bash
mvn clean test
```

Maven compila con Java 17 y ejecuta las pruebas automatizadas con JUnit.

Para medir y exigir al menos **80% de líneas en Domain y Application por separado**:

```powershell
mvn -Pcoverage clean verify
powershell -File scripts/coverage-summary.ps1
```

El HTML queda en `target/site/jacoco/index.html`. La revisión local del PR #65 obtuvo **91,39% en Domain y 85,17% en Application**, sin excluir clases de esas capas. El perfil `mysql-it,coverage` agrega MySQL real y también comprueba ambos umbrales en CI. Consultar [resultados y límites](docs/issue33-integracion-y-validacion.md).

## Ejecutar la aplicación

```bash
mvn exec:java
```

Se abre la ventana **SkillSwap · Mock UI Kit**. Para cerrarla, cerrá la ventana normalmente.

### Ejecutar con persistencia MySQL

Crear primero una base vacía dedicada a la demo y un usuario con permisos sobre ella, incluidos los necesarios para migraciones y tablas temporales. En PowerShell, configurar los datos de esa base:

```powershell
$env:SKILLSWAP_DB_URL = 'jdbc:mysql://127.0.0.1:3306/skillswap?connectionTimeZone=UTC'
$env:SKILLSWAP_DB_USER = 'skillswap'
$env:SKILLSWAP_DB_PASSWORD = '<contraseña del usuario de la demo>'
mvn exec:java '-Dexec.args=--persistence=jpa'
```

La ventana indica **SkillSwap · MySQL**. En la primera ejecución se crean perfiles, billeteras y publicaciones de muestra. Al reiniciar no se reemplazan perfiles, no se reinician saldos ni se duplican la bienvenida o las publicaciones, incluso si estas fueron pausadas. Al cerrar se liberan las conexiones JPA. En esta rama el selector demo permite alternar entre Mateo, Ana y Lucía tanto en memoria como con JPA; no reemplaza autenticación real.

## Recorrido de demostración

El guion detallado está en [`docs/guion-demo.md`](docs/guion-demo.md). Describe los flujos conectados, el selector de identidad de demo y las limitaciones de la persistencia en memoria.

## Diseño y defensa

- [Modelo editable de clases en StarUML](docs/modelo-skill-swap.mdj), con diagramas de Domain y Application.
- [Informe de decisiones actualizado](docs/informe-diseno.md) y [PDF actualizado sin UML](output/pdf/informe-diseno-actualizado.pdf). El [PDF histórico con diagramas](output/pdf/informe-diseno-skillswap.pdf) se conserva sin modificaciones.
- [Auditoría de patrones, GRASP y SOLID](docs/auditoria-patrones-y-grasp.md), con estado verificado en `main` y pendientes de integración.

## Estructura principal

```text
src/main/java/ar/edu/uade/skillswap/
├── controllers/  # Contratos que consume la vista
├── domain/       # Modelo y reglas del dominio
├── application/  # Casos de uso, modelos y puertos
├── infrastructure/ # Repositorios y wiring en memoria
├── mock/         # Controller alternativo para pruebas de UI
├── model/        # Modelos usados por el Mock UI Kit
└── ui/           # Interfaz Java Swing
```

Esta rama incorpora adaptadores JPA y migraciones Flyway V1–V6, incluidos los tipos de actor y publicación de #75 y el catálogo jerárquico de categorías de #37. La suite de integración utiliza la unidad JPA y el punto de composición de la aplicación contra MySQL real; los pasos para ejecutarla, sus escenarios y límites están en [Pruebas de MySQL](docs/pruebas-mysql.md). Se habilita con `mvn -Pmysql-it clean test` y requiere las variables de conexión de pruebas allí documentadas. Sin ese perfil, las pruebas de MySQL se omiten explícitamente.


## Integración y validación (#33)

La rama `feature/33-integration-report` añade un controller de calificaciones y un selector de usuario de demostración en Swing (Mateo, Ana y Lucía), para ejercitar roles de comprador y proveedor. Esta selección no reemplaza una autenticación real.

**Estado de verificación:** la revisión del 15/11/2026 incorpora `main` en `e64048c`, con #63/#64 y el modelo de organizaciones/desafíos de #75. Se conservan el selector de identidad, los tipos explícitos y las calificaciones. Se completaron el [recorrido visual](docs/validacion-visual-pr65.md), la cobertura por capa y el informe actualizado. La suite local ejecutó 224 pruebas sin fallos y omitió 39 de MySQL; el check de MySQL del PR registra el resultado de la cabeza final. La aprobación humana corresponde al revisor.

El detalle está en [docs/issue33-integracion-y-validacion.md](docs/issue33-integracion-y-validacion.md). El modelo StarUML y el PDF histórico se conservan intactos; el informe nuevo no contiene UML.

## Flujo de trabajo

Los cambios se integran mediante pull requests hacia `main`. Consultá los issues del repositorio para el alcance y responsable de cada entrega.
