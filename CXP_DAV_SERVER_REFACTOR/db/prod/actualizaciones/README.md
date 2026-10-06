# Actualizaciones de producción

Producción no usa Flyway. Cada migración nueva `Vn__descripcion.sql` de
`src/main/resources/db/migration` se entrega al DBA como un script en esta carpeta,
con el mismo número y descripción: `Vn__descripcion.sql`.

## Reglas

- Mismo efecto que la migración: mismas tablas, columnas, nombres de restricciones e
  índices, y los mismos identificadores en los catálogos.
- Todo el script dentro de `BEGIN; ... COMMIT;` y con `\set ON_ERROR_STOP on` al
  inicio, para que un error no deje cambios a medias.
- Al inicio, verificar que la base esté en la versión anterior (por ejemplo, que
  exista lo que creó la migración previa y no exista lo que crea esta) y detenerse
  con `RAISE EXCEPTION` si no. Así un script aplicado dos veces o fuera de orden no
  hace nada.
- Si la migración necesita datos propios del ambiente, se piden con `\set` en una
  sección `PARÁMETROS`, como en `instalacion_inicial.sql`.
- El mismo cambio se incorpora a `../instalacion_inicial.sql`, para que una
  instalación nueva quede en la última versión. `InstalacionInicialScriptTest`
  falla si el script y las migraciones difieren.

## Orden

Los scripts se ejecutan en orden de número, empezando por el siguiente a la versión
de `instalacion_inicial.sql` con la que se instaló la base. La versión actual del
script incluye hasta V15.

```bash
psql -h <host> -U <usuario> -d <base> -f V12__descripcion.sql
```

| Script | Contenido |
|---|---|
| `V9__recursos_de_carga.sql` | Tabla de recursos de carga (plantilla y manual), manual inicial y pantalla "Recursos de carga" del ADMIN. Después, el ADMIN publica `db/prod/plantilla_carga_documentos.xlsx`. |
| `V10__manual_de_carga_pdf.sql` | El manual pasa de Markdown a PDF y se elimina el manual en Markdown. Después, el ADMIN publica `db/prod/manual_carga_documentos.pdf`. |
| `V11__bitacora_de_documentos_operador.sql` | Bitácora de documentos por pagador para el operador bancario (ADMIN): botón BITACORA junto a INICIO y menú en la posición 2. |
| `V12__parametros_de_mailjet.sql` | Parámetros de Mailjet y URL de acceso, en `CONFIGURAR`. Después, el administrador del sistema coloca la API key, el secret, el remitente y la URL. |
| `V13__url_de_mailjet.sql` | Endpoint de Mailjet. El valor inicial es `https://api.mailjet.com/v3/send`. |
| `V14__secreto_jwt.sql` | Llave de firma JWT. El primer valor es `CONFIGURAR`. |
| `V15__valor_por_defecto_jwt_secret.sql` | Sustituye `CONFIGURAR` por la llave por defecto. No pisa una llave ya cambiada. |
