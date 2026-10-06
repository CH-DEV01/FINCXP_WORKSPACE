# Entregable de puesta en marcha — Financiamiento de Cuentas por Pagar

Banco Davivienda Salvadoreño · Banca Empresas. Generado el 05/10/2026.

## Contenido

| Carpeta | Archivo | Quién lo usa | Para qué |
|---|---|---|---|
| `01_Script_inicial` | `instalacion_inicial.sql` | DBA | Crea el esquema completo, catálogos, parámetros, la entidad banco y los dos primeros usuarios, en una sola transacción. Se ejecuta una vez sobre una base PostgreSQL 16 vacía. |
| `02_Manual_de_usuario` | `Manual_de_Usuario.pdf` | Usuarios | Uso de la plataforma por rol (versión 1.2). La sección 11 describe los correos automáticos: qué evento los dispara, a quién llegan y qué asunto tienen, incluido el aviso a los operadores bancarios cuando un proveedor envía una solicitud de anticipo. |
| | `Manual_de_Usuario_con_Anexo_Tecnico.pdf` | Equipo técnico | La misma guía más el anexo con el estado de las funcionalidades. |
| | `Manual_de_Usuario.md` | Equipo técnico | Fuente editable. |
| `04_Catalogo_de_rutas_por_rol` | `Catalogo_de_rutas_por_rol.pdf` | Seguridad / QA | Pantallas y rutas de API habilitadas para cada rol. |
| `03_Manual_tecnico` | `Manual_Tecnico.pdf` y `.md` | Infraestructura | Instalación y configuración de la API y el frontend, variables de entorno y despliegue en QA y producción. |
| `04_Carga_escenarios_exitosos` | `carga_escenarios_exitosos.xlsx` | QA | Carga válida: 31 documentos de 4 proveedores, total $25,000.00. Incluye facturas DIGITAL y PAPER, tipos CCF y FCI, políticas P30, P45, P60 y P90, desembolso T_PLUS_1 y ONLY_FRIDAYS, y documentos vencidos, cercanos al vencimiento y financiables. La hoja "Escenarios" explica qué pasa con cada uno. |
| | `generar_carga_exitosa.py` | QA | Vuelve a generar el archivo con fechas y códigos nuevos. |
| `07_Carga_escenarios_fallidos` | `carga_escenarios_fallidos.xlsx` | QA | Carga que se rechaza: 27 filas con un error distinto cada una y 3 filas válidas. La hoja "Escenarios" indica, por fila, la columna y el mensaje que aparece en el reporte de rechazo. |
| | `generar_carga_fallida.py` | QA | Vuelve a generar el archivo. |
| `08_Plantilla_de_carga` | `plantilla_carga_documentos.xlsx` | Operador bancario | Plantilla oficial que se publica en Recursos de carga y que descargan los pagadores. |
| `09_Terminos_y_condiciones` | `Terminos_Pagador_v1.1` y `Terminos_Proveedor_v1.0` (`.pdf` y `.md`) | Operador bancario / Legal | Texto vigente de cada tipo de términos, con los campos de la pantalla de términos (versión, título, texto de aceptación). |
| `10_Manual_de_carga` | `manual_carga_documentos.pdf` | Operador bancario | Instructivo para llenar y cargar el archivo; se publica en Recursos de carga junto con la plantilla. |

## Orden de puesta en marcha

1. **Base de datos (DBA).** Completar la sección `PARÁMETROS` de `instalacion_inicial.sql`: orígenes CORS, datos del banco, operador bancario (`ADMIN`) y administrador del sistema (`SYSTEM_ADMIN`). Luego ejecutar `psql -h <host> -U <usuario> -d <base> -f instalacion_inicial.sql`. Si falta un parámetro o la base no está vacía, el script se detiene sin guardar nada.
2. **API y frontend (infraestructura).** Desplegar según el manual técnico, sección 5. La API arranca con `FLYWAY_ENABLED=false`. El frontend se compila con `VITE_API_BASE_URL` y `VITE_PORTAL_URL` del ambiente.
3. **Parámetros (administrador del sistema).** En la pantalla de parámetros:
   - `JWT_SECRET`: reemplazar la llave por defecto por una propia del ambiente (Base64 de al menos 32 bytes). La llave por defecto está en el código fuente y no debe usarse en producción.
   - `MAILJET_API_KEY`, `MAILJET_API_SECRET`, `MAILJET_FROM_EMAIL`, `MAILJET_FROM_NAME` y `APP_LOGIN_URL`. Mientras sigan en `CONFIGURAR`, no se envían correos.
4. **Recursos de carga (operador bancario).** Publicar `08_Plantilla_de_carga/plantilla_carga_documentos.xlsx` y `10_Manual_de_carga/manual_carga_documentos.pdf` desde la pantalla Recursos de carga.
5. **Términos y condiciones (operador bancario).** Las dos versiones de la carpeta 09 ya quedan activas con el script inicial; no hay que subirlas para arrancar. Hay que reemplazar los textos que siguen pendientes (ver más abajo), publicando una versión nueva desde la pantalla de términos (por ejemplo, 1.2 para el pagador).
6. **Datos operativos (operador bancario).** Registrar pagadores con su línea de crédito y condiciones, crear sus usuarios y los de los proveedores, y registrar los feriados de años posteriores a 2026.
7. **Pruebas (QA).** Ver la sección siguiente.

## Cómo usar los archivos de prueba

- Antes de cargar, el pagador debe existir y estar activo, y su cupo disponible debe cubrir el total del archivo exitoso ($25,000.00). Si no alcanza, regenerar con otro total: `python3 generar_carga_exitosa.py carga.xlsx --total 9500`.
- Los proveedores del archivo exitoso se crean al cargarlo. Para que reciban el correo y puedan solicitar anticipos, hay que crearles usuarios en Gestión de usuarios.
- Al confirmar una solicitud de financiamiento desde la pantalla del proveedor, cada usuario operador bancario (`ADMIN`) activo debe recibir el correo *"Notificación Operativa: Nueva solicitud de anticipo de pago de {proveedor}"*, con el número de solicitud, montos y fecha de desembolso. El pagador no recibe correo. Requiere Mailjet configurado.
- Un archivo exitoso solo se puede cargar una vez. Si se vuelve a subir, la plataforma lo rechaza por **Riesgo de Doble Fondeo**, lo que sirve como prueba adicional de rechazo.
- Las fechas de emisión se calculan respecto al día en que se generó el archivo, y la antigüedad máxima es de 120 días (`MAX_INVOICE_AGE_DAYS`). Si pasan varias semanas, conviene regenerar los dos archivos. Los generadores requieren Python 3 con `openpyxl`.
- Hay un rechazo que no depende de las filas: si el total supera el cupo disponible del pagador, la carga se rechaza por límite de crédito.

## Pendientes antes de producción

- **Términos del pagador:** el texto vigente (1.1) es provisional y está marcado así. Legal debe entregar la versión aprobada.
- **Términos del proveedor, cláusula 7:** el texto contiene los marcadores `{{TASA_INTERES}}`, `{{TASA_REFERENCIA}}` y `{{PUNTOS_PORCENTUALES}}`. La plataforma muestra el texto tal cual y no los reemplaza, así que el proveedor vería los marcadores. Antes de producción hay que publicar una versión con los valores definidos por el banco, o implementar el reemplazo con las tasas del pagador.
