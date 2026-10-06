-- Recursos de las pantallas de carga de documentos (ADMIN y PAYER): la plantilla .xlsx que
-- publica el ADMIN y el manual en Markdown que explica cómo llenarla. Hay a lo sumo uno de
-- cada tipo; publicar uno nuevo reemplaza al anterior.

CREATE TABLE public.upload_resources (
    id uuid NOT NULL,
    resource_type character varying(30) NOT NULL,
    file_name character varying(255),
    file_content bytea,
    file_size bigint,
    content text,
    updated_by_id uuid,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT upload_resources_pkey PRIMARY KEY (id),
    CONSTRAINT uk_upload_resources_type UNIQUE (resource_type),
    CONSTRAINT upload_resources_type_check CHECK (resource_type IN ('TEMPLATE', 'MANUAL')),
    CONSTRAINT upload_resources_content_check CHECK (
        (resource_type = 'TEMPLATE' AND file_name IS NOT NULL AND file_content IS NOT NULL
            AND file_size IS NOT NULL AND content IS NULL)
        OR (resource_type = 'MANUAL' AND content IS NOT NULL AND file_name IS NULL
            AND file_content IS NULL AND file_size IS NULL)),
    CONSTRAINT fk_upload_resources_updated_by FOREIGN KEY (updated_by_id) REFERENCES public.users(id)
);

-- Manual inicial; el ADMIN lo ajusta desde Recursos de carga.
INSERT INTO public.upload_resources (id, resource_type, content, created_at, updated_at) VALUES
    ('c9000000-0000-4000-8000-000000000041', 'MANUAL', $manual$# Manual de uso de la plantilla de carga

## Antes de empezar

- Descargue la plantilla vigente desde la sección **Recursos** de esta pantalla.
- La primera fila de la primera hoja contiene los encabezados. No los cambie ni los mueva de fila; el orden de las columnas sí puede cambiar.
- Cada fila a partir de la segunda es un documento. Las filas vacías se ignoran.
- Un mismo archivo puede incluir documentos de varios proveedores.
- El archivo debe ser `.xlsx` o `.xls` y respetar el tamaño máximo y el número máximo de registros que configura el banco.
- Antes de digitar, aplique el formato de celda **Texto** a las columnas *NIT Proveedor* y *Cuenta Bancaria* para conservar los ceros a la izquierda.

## Columnas

| Columna | Obligatoria | Formato de celda | Qué debe contener |
|---|---|---|---|
| Fecha Emision | Sí | Fecha | Fecha de emisión del documento. No puede ser futura ni superar la antigüedad máxima que configura el banco. |
| Monto | Sí | Número o Contabilidad | Monto del documento, mayor a cero y con máximo 2 decimales. El sistema no redondea. |
| Numero Documento | Sí, para documentos en papel | Texto | Número del documento (máximo 255 caracteres). |
| Codigo Generacion | Sí, para documentos digitales | Texto | Código de generación del DTE: 36 caracteres con el formato 8-4-4-4-12 (hexadecimal). Vacío en documentos en papel. |
| Sello Recepcion | Sí, para documentos digitales | Texto | Sello de recepción del DTE: 40 letras y números. Vacío en documentos en papel. |
| Numero Control | Sí, para documentos digitales | Texto | Número de control con el formato `DTE-TT-XXXXXXXX-NNNNNNNNNNNNNNN`. `TT` es 01 (factura de consumidor final) o 03 (comprobante de crédito fiscal) y debe coincidir con *Tipo Factura*. Vacío en documentos en papel. |
| Metodo Emision | Sí | Texto | `DIGITAL` o `PAPER`. |
| Tipo Factura | Sí | Texto | `CCF` (comprobante de crédito fiscal) o `FCI` (factura de consumidor final). |
| NIT Proveedor | Sí | Texto | NIT del proveedor: 14 dígitos, con o sin guiones. |
| Nombre Proveedor | Sí | Texto | Nombre o razón social del proveedor (máximo 255 caracteres). |
| Politica Pago | Sí | Texto | Código de la política de pago, por ejemplo `P30`, `P45`, `P60` o `P90`. |
| Dia Desembolso | Sí | Texto | Código de la política de desembolso, por ejemplo `T_PLUS_1` o `ONLY_FRIDAYS`. |
| Cuenta Bancaria | Sí | Texto | Cuenta del proveedor, solo números (máximo 50). |

## Reglas que se validan

- **Cuenta del proveedor:** todas las filas de un proveedor deben traer la misma cuenta. Si el proveedor ya está registrado, debe coincidir con su cuenta registrada, y la cuenta no puede pertenecer a otra entidad.
- **Documentos digitales:** el código de generación, el número de control y el sello de recepción no pueden repetirse dentro del archivo ni existir ya en el sistema.
- **Documentos en papel:** el número de documento no puede repetirse para el mismo proveedor en el mismo año, ni dentro del archivo ni en el sistema.
- **Fórmulas:** se toma el valor que Excel dejó guardado en la celda.

## Resultado de la carga

- Si todo es válido, se registran los documentos y se descarga un **comprobante** en PDF.
- Si hay errores, la carga se rechaza completa y se descarga un **reporte de inconsistencias** en PDF con la fila, la columna y el motivo de cada error. No se guarda ningún documento hasta que se corrija el archivo y se vuelva a cargar.
- La carga también se rechaza si el total supera la línea de crédito disponible del pagador.$manual$, now(), now());

-- Pantalla del administrador bancario (ADMIN) para mantener los recursos.
INSERT INTO public.routes_cat (id, component_name, created_at, description, path, status, updated_at) VALUES
    ('c9000000-0000-4000-8000-000000000001', 'UploadResourceManagement', now(), 'Recursos de carga', 'upload-resources-management', 'ACTIVE', now());

INSERT INTO public.role_routes (id, created_at, is_index, role_id, route_id)
SELECT 'c9000000-0000-4000-8000-000000000011', now(), false, r.id, 'c9000000-0000-4000-8000-000000000001'
FROM public.roles_cat r
WHERE r.name = 'ADMIN';

INSERT INTO public.menus_cat (id, created_at, description, icon, label, path, status, updated_at) VALUES
    ('c9000000-0000-4000-8000-000000000021', now(), NULL, 'folder', 'Recursos de carga', '/admin/upload-resources-management', 'ACTIVE', now());

INSERT INTO public.role_menus (id, created_at, display_order, menu_id, role_id)
SELECT 'c9000000-0000-4000-8000-000000000031', now(), 11, 'c9000000-0000-4000-8000-000000000021', r.id
FROM public.roles_cat r
WHERE r.name = 'ADMIN';
