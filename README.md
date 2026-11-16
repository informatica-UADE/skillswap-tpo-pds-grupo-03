# SkillSwap

SkillSwap es un marketplace académico para ofrecer y contratar mentorías. Este repositorio corresponde al Trabajo Práctico de Proceso de Desarrollo de Software.

## Estado actual

La aplicación ejecutable es una demo de escritorio en Java Swing. La vista usa controllers que conectan perfiles, publicaciones, búsqueda, contrataciones, billetera, calificaciones y notificaciones con servicios de Application, y estos persisten en **MySQL mediante repositorios JPA (Hibernate)**. No hay un modo en memoria: la aplicación siempre se conecta a la base configurada. `MockSkillSwapControllers` permanece disponible para pruebas y recorridos aislados de la UI.

Al arrancar, Flyway aplica las migraciones y Hibernate valida el esquema. Los datos se conservan entre ejecuciones. Las identidades, los proveedores de email y push y los SkillCoins son de demostración: no hay autenticación ni pasarela de pago reales.

## Requisitos

- JDK 17 o superior.
- Apache Maven 3.9 o superior.
- Git.
- MySQL 8.4 para ejecutar la aplicación (por ejemplo, con Docker) y para las pruebas de integración con MySQL.

## Obtener el proyecto

```bash
git clone https://github.com/informatica-UADE/skillswap-tpo-pds-grupo-03.git
cd skillswap-tpo-pds-grupo-03
```

## Compilar y probar

```bash
mvn clean test
```

Maven compila con Java 17 y ejecuta las pruebas automatizadas con JUnit. Las pruebas de persistencia y de integración usan los repositorios JPA sobre una base H2 en memoria, creada para cada prueba; no necesitan MySQL. Las pruebas que dependen de MySQL real (migraciones de Flyway, colación sin acentos, concurrencia) se habilitan con el perfil `mysql-it`, como se explica en [Pruebas de MySQL](docs/pruebas-mysql.md); sin ese perfil se omiten.

Para medir y exigir al menos **80% de líneas en Domain y Application por separado**:

```powershell
mvn -Pcoverage clean verify
powershell -File scripts/coverage-summary.ps1
```

El HTML queda en `target/site/jacoco/index.html`. La revisión local del PR #65 obtuvo **91,39% en Domain y 85,17% en Application**, sin excluir clases de esas capas. El perfil `mysql-it,coverage` agrega MySQL real y también comprueba ambos umbrales en CI. Consultar [resultados y límites](docs/issue33-integracion-y-validacion.md).

## Ejecutar la aplicación

La aplicación necesita una base MySQL vacía y un usuario con permisos sobre ella (incluidos los de migraciones y tablas temporales). Con Docker, en PowerShell:

```powershell
docker run --detach --name skillswap-mysql --publish 127.0.0.1:3306:3306 --env MYSQL_ROOT_PASSWORD=<contraseña de root> --env MYSQL_DATABASE=skillswap --env MYSQL_USER=skillswap --env MYSQL_PASSWORD=<contraseña de la demo> mysql:8.4 --default-time-zone=+00:00
```

Si el contenedor ya existe, alcanza con `docker start skillswap-mysql`. Después, desde la raíz del proyecto:

```powershell
$env:SKILLSWAP_DB_URL = 'jdbc:mysql://127.0.0.1:3306/skillswap?connectionTimeZone=UTC'
$env:SKILLSWAP_DB_USER = 'skillswap'
$env:SKILLSWAP_DB_PASSWORD = '<contraseña de la demo>'
mvn compile exec:java
```

Sin esas variables, la aplicación no arranca e indica cuál falta. Se abre la ventana **SkillSwap**. En la primera ejecución se crean perfiles, billeteras y publicaciones de muestra. Al reiniciar no se reemplazan perfiles, no se reinician saldos ni se duplican la bienvenida o las publicaciones, incluso si estas fueron pausadas. Al cerrar se liberan las conexiones JPA. Para cambiar de usuario se hace clic en el recuadro del perfil activo, abajo en la barra lateral: lista los perfiles activos guardados con su tipo (con los datos de muestra, Mateo, Ana y Lucía). No reemplaza una autenticación real.

## Recorrido de demostración

El guion detallado está en [`docs/guion-demo.md`](docs/guion-demo.md). Describe los flujos conectados, el selector de identidad de demo y los límites de la demo.

## Diseño y defensa

- [Modelo editable de clases en StarUML](docs/modelo-skill-swap.mdj), con diagramas de Domain y Application.
- [Auditoría explicativa de los UML](docs/auditoria-uml.md), con la vista macro de arquitectura y la vista micro del dominio.
- [Trazabilidad de issues #13–#21 y beneficios de Strategy](docs/Docs%20Issue%20Patrones%20Strategy%20.md).
- [Informe de decisiones de diseño](docs/informe-diseno.md) y [versión PDF](output/pdf/informe-diseno-skillswap.pdf).
- [Auditoría de patrones, GRASP y SOLID](docs/auditoria-patrones-y-grasp.md), con estado verificado en `main` y pendientes de integración.
- [Decisión e implementación de CQRS para el Issue #42](docs/issue-42-cqrs.md), con alcance, puertos y flujos de lectura/escritura.
- [Persistencia solo con JPA y MySQL](docs/decision-persistencia-solo-jpa.md): por qué se eliminaron los repositorios en memoria y cómo se prueban los JPA.

## Estructura principal

```text
src/main/java/ar/edu/uade/skillswap/
├── controllers/  # Contratos que consume la vista
├── domain/       # Modelo y reglas del dominio
├── application/  # Casos de uso, modelos y puertos
├── infrastructure/ # Repositorios JPA, migraciones, adapters y wiring
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
