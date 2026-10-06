# Base de datos

En desarrollo y QA el esquema lo administra Flyway. Las migraciones están en
`src/main/resources/db/migration` y se ejecutan solas al arrancar la API.
En producción no hay Flyway: el DBA ejecuta los scripts de `db/prod/` (ver
[Producción](#producción)). Hibernate solo valida el esquema (`ddl-auto=validate`).

| Migración | Contenido |
|---|---|
| `V1__esquema_inicial.sql` | Tablas, índices y restricciones. |
| `V2__datos_de_referencia.sql` | Catálogos, rutas, menús, plantilla de Excel, términos vigentes y parámetros del sistema. |
| `V3__montos_calculados_con_todos_los_decimales.sql` | Montos y factores calculados con escala 18; se redondean a 2 decimales solo al mostrarse. |
| `V4__estado_inactivado_por_pagador.sql` | Estado `INACTIVATED_BY_PAYER` y reclasificación de los inactivos que hizo el pagador. |
| `V5__lotes_de_dispersion.sql` | Tabla `dispersion_batches`, estados `REQUESTED_FOR_DISPERSION` y `DISPERSED`, rutas y menús de la terminal y la bitácora de dispersiones para el administrador. |
| `V6__alta_de_usuarios_proveedor_fuera_de_la_carga.sql` | Desactiva las columnas NIU Proveedor y las del operario en la plantilla de Excel (el proveedor se identifica por NIT y sus usuarios se crean en Gestión de usuarios); `entities.niu` admite nulos. |
| `V7__eliminar_niu_de_entidades.sql` | Elimina la columna `entities.niu`: las entidades se identifican solo por su NIT. |
| `V8__eliminar_rol_operator.sql` | Elimina el rol `OPERATOR` (no tenía pantallas ni permisos); sus usuarios pasan a `ADMIN` inactivos para conservar el historial. La descripción de `ADMIN` pasa a "Operador bancario". |
| `V9__recursos_de_carga.sql` | Tabla `upload_resources` (plantilla `.xlsx` y manual de la carga), manual inicial y pantalla "Recursos de carga" del ADMIN. |
| `V10__manual_de_carga_pdf.sql` | El manual de la carga pasa de Markdown a PDF publicado por el ADMIN; se elimina el manual en Markdown de V9. |
| `V11__bitacora_de_documentos_operador.sql` | Bitácora de documentos por pagador para el operador bancario (ADMIN), junto a Inicio. |
| `V12__parametros_de_mailjet.sql` | Credenciales de Mailjet, remitente y URL de acceso. Quedan en `CONFIGURAR` hasta que el administrador del sistema las reemplace. |
| `V13__url_de_mailjet.sql` | Endpoint de Mailjet (`https://api.mailjet.com/v3/send`), editable desde Gestión de parámetros. |
| `V14__secreto_jwt.sql` | Llave de firma de los JWT. El primer valor es `CONFIGURAR`. |
| `V15__valor_por_defecto_jwt_secret.sql` | Sustituye ese marcador por la llave por defecto. No pisa una llave que el ambiente ya haya cambiado. |

## Desarrollo y QA

1. Crear una base PostgreSQL 16 vacía.
2. Configurar las variables de entorno:

   | Variable | Default |
   |---|---|
   | `DB_URL` | `jdbc:postgresql://localhost:5432/postgres` |
   | `DB_USERNAME` | `postgres` |
   | `DB_PASSWORD` | `postgres` |
   | `FLYWAY_ENABLED` | `true` |

3. Al arrancar, Flyway ejecuta todas las migraciones. `JWT_SECRET` queda con una
   llave por defecto y la API arranca. Cada ambiente puede reemplazarla; el cambio
   invalida las sesiones ya emitidas.
4. Revisar en `system_parameters` los valores propios del ambiente, sobre todo
   `CORS_ALLOWED_ORIGINS` y `JWT_SECRET`, y el calendario de `bank_holidays_cat`.

Solo en desarrollo, para tener entidades, usuarios, línea de crédito y convenio de demo:

```bash
psql -h localhost -U postgres -d postgres -f db/dev/seed_dev.sql
```

## Producción

La API arranca con `FLYWAY_ENABLED=false`. Si Flyway quedara activo contra una base
creada por el script, la marcaría en la versión 2 y volvería a aplicar V3 en adelante,
y la API no arrancaría.

### Instalación inicial

`db/prod/instalacion_inicial.sql` lo ejecuta el DBA una sola vez, sobre una base
PostgreSQL 16 vacía. Deja la base igual que las migraciones V1 a V15 (mismas tablas,
restricciones, índices e identificadores de catálogo) y, en una sola transacción, crea:

| Qué | Para qué |
|---|---|
| Esquema completo | Tablas, restricciones e índices. |
| Catálogos | Tipos de entidad, roles, políticas de pago y desembolso, feriados oficiales de 2026, plantilla de Excel, tipos y versiones vigentes de términos, rutas y menús por rol. |
| Parámetros del sistema | Valores por defecto; `CORS_ALLOWED_ORIGINS` se toma del parámetro `cors_origenes`. |
| Entidad banco (tipo BANCO, `COD_003`) | Entidad a la que pertenecen los usuarios del banco. |
| Operador bancario (`ADMIN`) | Opera la plataforma y, desde Gestión de usuarios, crea usuarios `ADMIN`, `PAYER` y `SUPPLIER`. |
| Administrador del sistema (`SYSTEM_ADMIN`) | Administra los parámetros del sistema. Este rol solo se asigna en la base de datos. |

1. Copiar el archivo y completar la sección `PARÁMETROS`: orígenes CORS del cliente
   (separados por coma, por ejemplo `https://cxp.davivienda.com.sv`), NIT, nombre y
   código del banco, y DUI, correo, nombres y apellidos de cada usuario.
2. Ejecutar `psql -h <host> -U <usuario> -d <base> -f instalacion_inicial.sql`.
3. Arrancar la API con `FLYWAY_ENABLED=false`.

Si falta un parámetro, un formato no es válido o la base no está vacía, el script se
detiene sin guardar nada.

`db/prod/inserts_iniciales.sql` tiene solo esos INSERT de catálogos y parámetros,
sin el esquema ni la entidad banco. Sirve cuando las tablas ya existen.

Después de instalar:

- El operador bancario publica desde Recursos de carga la plantilla
  `db/prod/plantilla_carga_documentos.xlsx` y el manual `db/prod/manual_carga_documentos.pdf`.
  Si cambian las columnas de `excel_template_columns` o las validaciones de la carga, se
  actualizan `COLUMNS` en `db/prod/generar_plantilla_carga.py` y el contenido de
  `db/prod/GenerarManualCarga.java`, se regeneran ambos archivos y se vuelven a publicar:

  ```bash
  python3 db/prod/generar_plantilla_carga.py
  java -cp ~/.m2/repository/com/github/librepdf/openpdf/1.3.39/openpdf-1.3.39.jar db/prod/GenerarManualCarga.java
  ```
- El texto vigente de los términos del pagador es provisional; el operador bancario
  publica la versión legal desde la pantalla de términos.
- Los feriados de años posteriores a 2026 se registran desde el catálogo de feriados.

### Actualizaciones

Cada migración nueva de Flyway tiene su script equivalente para el DBA en
`db/prod/actualizaciones/` (ver el README de esa carpeta), y además se incorpora a
`instalacion_inicial.sql` para que una instalación nueva quede al día.
`InstalacionInicialScriptTest` compara el script con las migraciones y falla si
difieren.

## Base existente creada antes de Flyway

Si la base ya tiene el esquema pero no la tabla `flyway_schema_history`, al
arrancar Flyway la marca en la versión 2 (`baseline-on-migrate`) sin ejecutar
V1 ni V2. Antes, la base debe tener aplicados los scripts de `db/legacy/`.

## Cambios al esquema

Cada cambio es un archivo nuevo `V9__descripcion.sql`, `V10__...`, etc. Nunca se
edita una migración ya aplicada, porque Flyway valida su checksum y la API no
arrancaría. Cada migración nueva requiere también los cambios de producción descritos
en [Actualizaciones](#actualizaciones).

## Carpetas

- `dev/`: datos de demostración, solo para desarrollo.
- `prod/`: scripts que el DBA ejecuta en producción (instalación inicial y actualizaciones).
- `legacy/`: scripts manuales de antes de Flyway. Se conservan como historial y
  ya no se deben ejecutar.
- `samples/`: archivos Excel de ejemplo y los scripts que los generan.
